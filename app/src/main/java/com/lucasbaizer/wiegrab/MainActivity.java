package com.lucasbaizer.wiegrab;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class MainActivity extends Activity implements OutputCallback {
	private static WeakReference<MainActivity> instance;
	private static BleKeyService ble;
	private DialogFragment dialog;
	private boolean isJamming;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		instance = new WeakReference<MainActivity>(this);

		setContentView(R.layout.activity_main);

		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "BLE is not supported on your device.", Toast.LENGTH_LONG).show();
		}

		Intent bleIntent = new Intent(this, BleKeyService.class);
		bindService(bleIntent, new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				ble = ((BleKeyService.BleKeyBinder) service).getService();
				try {
					ble.connect();
				} catch (IOException e) {
					e.printStackTrace();
				}

				dialog = new SearchingDialogFragment();
				dialog.show(getFragmentManager(), "searchingFragment");

				ble.output = MainActivity.this;
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				dialog.show(getFragmentManager(), "searchingFragment");
			}
		}, BIND_AUTO_CREATE);
	}

	public void onReplayLast(View button) {
		ble.replayLast();
	}

	public void onReplayCustom(View button) {
		ble.replayCustom();
	}

	public void onCustomSettings(View button) {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	public void onReadPrevious(View button) {
		if(!ble.readCards()) {
			return;
		}

		dialog = new ReadingDialogFragment();
		dialog.show(getFragmentManager(), "readingFragment");

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				ble.output.consume(BleKeyService.CODE_CARD_RESPONSE | BleKeyService.CODE_RECEIVED_CARDS,"2 cards received from BLEKey!");
				ble.output.consume(BleKeyService.CODE_CARD_RESPONSE | BleKeyService.CODE_CARD_DATA,"0/26 bits/AB CD EF 01 23 45");
				ble.output.consume(BleKeyService.CODE_CARD_RESPONSE | BleKeyService.CODE_CARD_DATA,"1/0 bits/00 00 00 00 00 00");
			}
		}).start();
	}

	public void onJamNetwork(View buttonView) {
		final Button button = (Button) buttonView;
		if(button.getText().equals("Stop Network Jam")) {
			button.setText("Jam Wiegand Network");

			isJamming = false;
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder
					.setMessage("This will erase any current custom card data you have. Are you sure you want to continue?")
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();

							if(!ble.setCustomData(new byte[BleKeyService.CARD_DATA_LENGTH])) {
								return;
							}
							ble.output.consume(-1, "Jamming Wiegand network (20/s)...");

							button.setText("Stop Network Jam");

							isJamming = true;
							new Thread(new Runnable() {
								@Override
								public void run() {
									while (isJamming) {
										try {
											Thread.sleep(50);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}

										if(!isJamming) {
											break;
										}

										if(!ble.replayCustom()) {
											isJamming = false;
											break;
										}
									}

									ble.output.consume(-1, "Stopped jamming Wiegand network.");
								}
							}).start();
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			AlertDialog dialog = builder.create();
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.show();
		}
	}

	@Override
	public void consume(int code, final String output) {
		if(!isJamming || (code & BleKeyService.CODE_REPLAY_CUSTOM) != BleKeyService.CODE_REPLAY_CUSTOM) {
			Log.d("Wiegrab", output);

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					((TextView) findViewById(R.id.console)).append("\n" + output);
					final ScrollView scroll = findViewById(R.id.scrollView);
					scroll.post(new Runnable() {
						@Override
						public void run() {
							scroll.fullScroll(View.FOCUS_DOWN);
						}
					});
				}
			});
		}

		if((code & BleKeyService.CODE_CONNECTED) == BleKeyService.CODE_CONNECTED) {
			dialog.dismiss();
		} else if((code & BleKeyService.CODE_DISCONNECTED) == BleKeyService.CODE_DISCONNECTED) {
			dialog.show(getFragmentManager(), "searchingFragment");
		} else if((code & BleKeyService.CODE_CARD_RESPONSE) == BleKeyService.CODE_CARD_RESPONSE) {
			dialog.dismiss();

			if((code & BleKeyService.CODE_NO_CARDS) == BleKeyService.CODE_NO_CARDS) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder
						.setMessage(output)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
				AlertDialog dialog = builder.create();
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.show();
			} else {
				if((code & BleKeyService.CODE_RECEIVED_CARDS) == BleKeyService.CODE_RECEIVED_CARDS) {
					Intent intent = new Intent(this, ReadCardsActivity.class);
					startActivity(intent);
				}
				ReadCardsActivity.buffer(code, output);
			}
		}
	}

	public static BleKeyService getBleKeyService() {
		return ble;
	}

	public static MainActivity getInstance() {
		return instance.get();
	}
}
