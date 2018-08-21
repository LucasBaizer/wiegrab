package com.lucasbaizer.wiegrab;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CardData {
	private static File cardFile;
	private static Map<String, String> data = new HashMap<>();

	static {
		try {
			ensureExists();
			readData();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Map<String, String> getData() {
		return new HashMap<>(data);
	}

	public static void putData(String name, String value) throws IOException {
		data.put(name, value);

		saveData();

		Log.d("Wiegrab", "Saved card data to file: " + cardFile.getAbsolutePath());
	}

	private static void saveData() throws IOException {
		String str = "";
		for(Map.Entry<String, String> pair : data.entrySet()) {
			str += "[" + pair.getKey() + "]\n";
			str += pair.getValue() + "\n";
		}
		FileWriter out = new FileWriter(cardFile);
		out.write(str.trim());
		out.flush();
		out.close();
	}

	private static void readData() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(cardFile));
		String line = "";
		String key = "";
		while((line = reader.readLine()) != null) {
			line = line.trim();
			if(line.isEmpty()) {
				continue;
			}

			if(line.startsWith("[") && line.endsWith("]")) {
				key = line.substring(1, line.length() - 1);
			} else {
				data.put(key, line);
				key = null;
			}
		}
	}

	private static void ensureExists() throws IOException {
		File cardData = Environment.getExternalStoragePublicDirectory("CardData");
		Log.d("Wiegrab", "CardData directory: " + cardData.getAbsolutePath());
		if(!cardData.exists()) {
			Log.d("Wiegrab", "Making CardData directory...");
			cardData.mkdir();
			Log.d("Wiegrab", "CardData now exists: " + cardData.exists());
		}

		cardFile = new File(cardData, "cards.txt");
		if(!cardFile.exists()) {
			Log.d("Wiegrab", "Making cards file...");
			cardFile.createNewFile();
		}
	}
}
