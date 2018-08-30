package com.lucasbaizer.wiegrab;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BleKeyService extends Service {
	private static final byte[] DEFAULT_MAC = new byte[] { (byte) 0xD4, (byte) 0x66, (byte) 0x9D, (byte) 0xCC, (byte) 0xCF, (byte) 0xC0 };
	public static final int CARD_DATA_LENGTH = 7;
	private static final UUID UUID_READ_CARDS = UUID.fromString("0000aaaa-0000-1000-8000-00805f9b34fb");
	private static final UUID UUID_WRITE = UUID.fromString("0000bbbb-0000-1000-8000-00805f9b34fb");
	private static final UUID UUID_CUSTOM_DATA = UUID.fromString("0000cccc-0000-1000-8000-00805f9b34fb");

	public static final int CODE_CONNECTED = 1;
	public static final int CODE_DISCONNECTED = 2;
	public static final int CODE_REPLAY_CUSTOM = 4;
	public static final int CODE_CARD_RESPONSE = 8;
	public static final int CODE_NO_CARDS = 16;
	public static final int CODE_CARD_DATA = 32;
	public static final int CODE_RECEIVED_CARDS = 64;

	public OutputCallback output;

	private IBinder binder = new BleKeyBinder();
	private BluetoothGatt gatt;
	private BluetoothGattCallback callback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			Log.d("Wiegrab", "onConnectionStateChange: " + status + ": " + newState);
			if(newState == BluetoothProfile.STATE_CONNECTED) {
				output.consume(CODE_CONNECTED,"Connected!");
			} else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
				output.consume(CODE_DISCONNECTED, "Disconnected!");
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if(characteristic.getUuid().equals(UUID_READ_CARDS)) {
				byte[] lastCards = characteristic.getValue();
				int cardCount = lastCards.length / CARD_DATA_LENGTH;

				if(cardCount == 0) {
					output.consume(CODE_CARD_RESPONSE | CODE_NO_CARDS, "No cards received from BLEKey.");
				} else {
					output.consume(CODE_CARD_RESPONSE | CODE_RECEIVED_CARDS, cardCount + " cards received from BLEKey!");
					for (int i = 0; i < cardCount; i++) {
						int start = i * CARD_DATA_LENGTH;
						byte[] cardData = new byte[CARD_DATA_LENGTH - 1];
						for(int j = start + 1, k = cardData.length - 1; j < start + CARD_DATA_LENGTH; j++, k--) {
							cardData[k] = lastCards[j];
						}

						String str = "";
						for(byte b : cardData) {
							str += String.format("%02X", b) + " ";
						}
						output.consume(CODE_CARD_RESPONSE | CODE_CARD_DATA, i + "/" + lastCards[i] + " bits/" + str.substring(0, str.length() - 1));
					}
				}
			}
		}
	};

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		try {
			disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return super.onUnbind(intent);
	}

	public void connect() throws IOException {
		BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter adapter = manager.getAdapter();
		BluetoothDevice device = adapter.getRemoteDevice(DEFAULT_MAC);

		Log.d("Wiegrab", "Attemping to connect to GATT server...");
		gatt = device.connectGatt(this, true, callback);
	}

	public void disconnect() throws IOException {
		gatt.disconnect();
	}

	public boolean replayLast() {
		return replay(0xFF);
	}

	public boolean replayCustom() {
		return replay(0xFD);
	}

	public boolean replay(int card) {
		if(card == 0xFF) {
			output.consume(-1, "Replaying last card...");
		} else if(card == 0xFD) {
			output.consume(CODE_REPLAY_CUSTOM, "Replaying custom card...");
		} else {
			output.consume(-1, "Replaying card " + card + "...");
		}

		BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID_WRITE, BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, 0);
		characteristic.setValue(new byte[] { (byte) card });
		if(!gatt.writeCharacteristic(characteristic)) {
			output.consume(-1, "Could not write GATT characteristic.");
			return false;
		}
		return true;
	}

	public boolean readCards() {
		output.consume(-1, "Reading last cards...");

		BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID_READ_CARDS, BluetoothGattCharacteristic.PROPERTY_READ, 0);
		if(!gatt.readCharacteristic(characteristic)) {
			output.consume(-1, "Could not read GATT characteristic.");
			return false;
		}
		return true;
	}

	public boolean setCustomData(byte[] data) throws IndexOutOfBoundsException {
		output.consume(-1, "Setting custom card data...");

		BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID_CUSTOM_DATA, BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, 0);
		characteristic.setValue(data);
		if(!gatt.writeCharacteristic(characteristic)) {
			output.consume(-1, "Could not write GATT characteristic.");
			return false;
		}
		return true;
	}

	public class BleKeyBinder extends Binder {
		BleKeyService getService() {
			return BleKeyService.this;
		}
	}
}
