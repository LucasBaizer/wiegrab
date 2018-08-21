package com.lucasbaizer.wiegrab;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.IOException;

public class SaveDialogFragment extends DialogFragment {
	private String data;

	public SaveDialogFragment setData(String data) {
		this.data = data;

		return this;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		final EditText input = new EditText(getActivity());
		input.setHint("card name");
		input.setText(data.split("/")[0]);
		input.setSelection(input.getText().length());
		input.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));

		builder
				.setView(input)
				.setCancelable(false)
				.setPositiveButton("Save", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();

						try {
							CardData.putData(input.getText().toString().trim(), data);
						} catch (IOException e) {
							e.printStackTrace();
						}
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
		dialog.setCanceledOnTouchOutside(false);
		return dialog;
	}
}
