package com.cosso.carassistant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.os.Environment;
import android.util.Log;

public class Prefs {
	private static final String LOG_TAG = "Log tag";
		
	public static String getSettings(String key){
		String fileName = "settings.txt";
		String setting = "";
		String delims = "[ ]+";
		String command = "";
		
		File file = new File(Environment.getExternalStorageDirectory() + "/gpsData/"+fileName);
		if(file.exists()){
			Log.d(LOG_TAG, "file exists");
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				Log.d(LOG_TAG, line);
				String[] tokens = line.split(delims);
				Log.d(LOG_TAG, tokens[0] + " " + tokens[0].length());
				if(line.length() != 0 && tokens[0].charAt(0) == '!'){
					command = tokens[0].substring(1);
					if(command.equals(key)){
						setting = tokens[1];
						break;
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return setting;
	}
	
}
