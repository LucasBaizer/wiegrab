package com.lucasbaizer.wiegrab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Map;

public class SettingsActivity extends Activity {
	private static CharSequence label = "Card Name";
	private static CharSequence bits = "";
	private static CharSequence hex = "";

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_settings);

		final TextView settingsLabel = findViewById(R.id.settingsLabel);
		final TextView settingsBitCount = findViewById(R.id.settingsBitCount);
		final TextView settingsHex = findViewById(R.id.settingsHex);

		settingsLabel.setText(label);
		settingsBitCount.setText(bits);
		settingsHex.setText(hex);

		for(Map.Entry<String, String> entry : CardData.getData().entrySet()) {
			String[] split = entry.getValue().split("/");

			RelativeLayout card = (RelativeLayout) getLayoutInflater().inflate(R.layout.view_card, null);
			final TextView label = card.findViewById(R.id.label);
			final TextView bits = card.findViewById(R.id.bitCount);
			final TextView hex = card.findViewById(R.id.hex);
			ImageButton use = card.findViewById(R.id.button);
			label.setText(entry.getKey());
			bits.setText(split[1]);
			hex.setText(split[2]);

			use.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					settingsLabel.setText(label.getText());
					settingsBitCount.setText(bits.getText().toString().split(" ", 2)[0]);
					settingsHex.setText(hex.getText());
				}
			});

			LinearLayout list = findViewById(R.id.cardList);
			list.addView(card);
		}

		Button save = findViewById(R.id.saveButton);
		save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					BleKeyService ble = MainActivity.getBleKeyService();

					byte[] cardData = new byte[BleKeyService.CARD_DATA_LENGTH];
					cardData[0] = Byte.parseByte(settingsBitCount.getText().toString().trim());

					byte[] data = new byte[cardData.length - 1];
					String[] split = settingsHex.getText().toString().trim().split(" ");
					if (split.length == data.length) {
						for (int i = 0; i < data.length; i++) {
							data[i] = (byte) Integer.parseInt(split[i].toLowerCase(), 16);
						}
						System.arraycopy(data, 0, cardData, 1, data.length);

						if (!ble.setCustomData(cardData)) {
							AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
							builder
									.setMessage("Could not write GATT characteristic.")
									.setPositiveButton("OK", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											dialog.dismiss();
											finish();
										}
									});
							AlertDialog dialog = builder.create();
							dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
							dialog.show();
						} else {
							label = settingsLabel.getText();
							bits = settingsBitCount.getText();
							hex = settingsHex.getText();

							finish();
						}
					}
				} catch(NumberFormatException e) {
					// ignore
				}
			}
		});
	}
}