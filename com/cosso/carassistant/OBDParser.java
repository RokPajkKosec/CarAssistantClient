package com.cosso.carassistant;

import android.util.Log;

/**
 * @author Rok Pajk Kosec
 * Class for parsing OBD II responses
 */
public class OBDParser {
	private static final String TAG = "BluetoothChat";
	
	/**
	 * @param message
	 * @return parsed integer value
	 * Used for parsing 1 byte long responses
	 */
	public static int parseVSS(String message){
		
		message = message.replaceAll(">", "");
		message = message.replaceAll(" ", "");
		message = message.replaceAll("\n", "");
		message = message.replaceAll("\r", "");
		
		try {
			message = message.substring(4);
			Log.d(TAG, "parse v: " + Integer.parseInt(message, 16));
			
			return Integer.parseInt(message, 16);
		} catch (Exception e) {
			return -1;
		}

	}
	
	/**
	 * @param message
	 * @return integer value of response type
	 * Used for parsing 1 byte response code
	 */
	public static int parseResponse(String message){
		message = message.replaceAll(">", "");
		message = message.replaceAll(" ", "");
		message = message.replaceAll("\n", "");
		message = message.replaceAll("\r", "");

		try {
			String res = message.substring(2,4);
			Log.d(TAG, "parse r: " + Integer.parseInt(res, 16));
			
			return Integer.parseInt(res, 16);
		} catch (Exception e) {
			return -1;
		}

	}
	
	/**
	 * 
	 * @param message
	 * @return true if response is response to request code
	 */
	public static boolean checkResponse(String message){
		message = message.replaceAll(">", "");
		message = message.replaceAll(" ", "");
		message = message.replaceAll("\n", "");
		message = message.replaceAll("\r", "");

		try {
			String res = message.substring(0,2);
			Log.d(TAG, "parse r: " + res);
			
			if(res.equals("41")){
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}
	
	/**
	 * @param message
	 * @return double value of parsed response
	 * Used for parsing 2 byte response code
	 */
	public static double parseMAF(String message){
		String a;
		String b;
		
		message = message.replaceAll(">", "");
		message = message.replaceAll(" ", "");
		message = message.replaceAll("\n", "");
		message = message.replaceAll("\r", "");
		
		try {
			a = message.substring(4,6);
			b = message.substring(6);
			
			int aInt = Integer.parseInt(a, 16);
			int bInt = Integer.parseInt(b, 16);
			
			Log.d(TAG, "parse m: " + aInt + " " + bInt);
			
			return ((aInt * 256) + bInt)/4;
		} catch (Exception e) {
			return -1.0;
		}

	}
	
	/**
	 * @param message
	 * @return String value of response
	 * Unused, kept for future developement
	 */
	public static String parse2(String message){
    	String dataRecieved;
    	int value = 0;
    	int value2 = 0;
    	int PID = 0;
		dataRecieved = message;
                       
        if((dataRecieved != null) && (dataRecieved.matches("\\s*[0-9A-Fa-f]{2} [0-9A-Fa-f]{2}\\s*\r?\n?" ))) {

			dataRecieved = dataRecieved.trim();
			String[] bytes = dataRecieved.split(" ");
			
			message = message + dataRecieved;

			if((bytes[0] != null)&&(bytes[1] != null)) {
				
				PID = Integer.parseInt(bytes[0].trim(), 16);
				value = Integer.parseInt(bytes[1].trim(), 16); 
			}
        }
        
        else if((dataRecieved != null) && (dataRecieved.matches("\\s*[0-9A-Fa-f]{1,2} [0-9A-Fa-f]{2} [0-9A-Fa-f]{2}\\s*\r?\n?" ))) {
        	
			dataRecieved = dataRecieved.trim();
			String[] bytes = dataRecieved.split(" ");
			
			message = message + dataRecieved;
			
			if((bytes[0] != null)&&(bytes[1] != null)&&(bytes[2] != null)) {
				
				PID = Integer.parseInt(bytes[0].trim(), 16);
				value = Integer.parseInt(bytes[1].trim(), 16);
				value2 = Integer.parseInt(bytes[2].trim(), 16);
			}
        }
        
        else if((dataRecieved != null) && (dataRecieved.matches("\\s*[0-9]+(\\.[0-9]?)?V\\s*\r*\n*" ))) {
        	
        	dataRecieved = dataRecieved.trim();
        	String volt_number = dataRecieved.substring(0, dataRecieved.length()-1); 
        	double needle_value = Double.parseDouble(volt_number);
        	needle_value = (((needle_value - 11)*21) /0.5) - 100;
        	int volt_value = (int)(needle_value);
        	
			message = message + dataRecieved;
        	
        } 
        else if((dataRecieved != null) && (dataRecieved.matches("\\s*[0-9]+(\\.[0-9]?)?V\\s*V\\s*>\\s*\r*\n*" ))) {
        	
        	dataRecieved = dataRecieved.trim();
        	String volt_number = dataRecieved.substring(0, dataRecieved.length()-1); 
        	double needle_value = Double.parseDouble(volt_number);
        	needle_value = (((needle_value - 11)*21) /0.5) - 100;
        	int volt_value = (int)(needle_value);
        	
			message = message + dataRecieved;
        }    
        else if((dataRecieved != null) && (dataRecieved.matches("\\s*[ .A-Za-z0-9\\?*>\r\n]*\\s*>\\s*\r*\n*" ))) {
			message = message + dataRecieved;
        }
        else {
        	message = message + dataRecieved;
    	}
		
        Log.d(TAG, "parse: " + PID + " " + value + " " + value2);
		
		return new String();
	}
}
