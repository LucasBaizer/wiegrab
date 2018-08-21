package com.lucasbaizer.wiegrab;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ReadCardsActivity extends Activity {
	private static WeakReference<ReadCardsActivity> instance;
	private static List<ConsumeParameters> paramsBuffer = new ArrayList<>();

	public static void buffer(int code, String output) {
		if(instance == null || instance.get() == null) {
			paramsBuffer.add(new ConsumeParameters(code, output));
		} else {
			instance.get().consume(code, output);
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_read_cards);

		instance = new WeakReference<>(this);

		for(ConsumeParameters params : paramsBuffer) {
			consume(params.code, params.output);
		}
	}

	public void consume(int code, final String output) {
		if((code & BleKeyService.CODE_RECEIVED_CARDS) == BleKeyService.CODE_RECEIVED_CARDS) {
			int cards = Integer.parseInt(output.split(" ", 2)[0]);

			TextView label = findViewById(R.id.label);
			label.setText("Received " + cards + " cards from BLEKey!");
		} else if((code & BleKeyService.CODE_CARD_DATA) == BleKeyService.CODE_CARD_DATA) {
			String[] split = output.split("/");

			RelativeLayout card = (RelativeLayout) getLayoutInflater().inflate(R.layout.view_card, null);
			TextView label = card.findViewById(R.id.label);
			TextView bits = card.findViewById(R.id.bitCount);
			TextView hex = card.findViewById(R.id.hex);
			ImageButton save = card.findViewById(R.id.button);
			label.setText(split[0]);
			bits.setText(split[1]);
			hex.setText(split[2]);

			save.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new SaveDialogFragment().setData(output).show(getFragmentManager(), "saveFragment");
				}
			});

			LinearLayout list = findViewById(R.id.cardList);
			list.addView(card);
		}
	}

	private static class ConsumeParameters {
		private int code;
		private String output;

		public ConsumeParameters(int code, String output) {
			this.code =  code;
			this.output = output;
		}
	}
}
