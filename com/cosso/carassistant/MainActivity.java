package com.cosso.carassistant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

/**
 * @author Rok Pajk Kosec
 * Main activity used to collect and process data collected from GPS and OBD II.
 * It also is used for sending data to server via post service
 */
public class MainActivity extends Activity{
	private final String LOG_TAG = "Log tag";
	private final String TAG = "BluetoothChat";

	private LocationManager locationManager;
	
	//login details
	private String userName;
    private String passWord;
	
	//raw data from sensor output
	private double mLat;
	private double mLong;
	private double mSpeed;
	
	//distance and heading calculation values
	private double lastLat;
	private double lastLong;
	
	//private Location lastLocation;
	private double tempDistance;
	private float[] directionResults;
	private double headDirection;
	
	//run averages calculation values
	private double run;
	private double avgSpeed;
	private int takeCount;
	private int headingTakeCount;
	private double avgHeading;
	private double avgConsumption;
	private double tempAvgConsumption;
	
	//text views
	private TextView latMatched;
	private TextView longMatched;
	private TextView source;
	private TextView take;
	private TextView avgSpd;
	private TextView avgHead;
	private TextView avgConsmpt;
	private TextView wayName;
	private TextView wayType;
	private TextView waySpeedLimit;
	private TextView postResponse;
	private TextView button;
	
	//location obtaining data
	private LocationListener listener;
	private Listener gpsListener;
	private Location gpsLocation;
	private boolean gpsFixed;
	private long mLastLocationMillis;

	//file management values
	private File myFile;
	private boolean doLog;
	private boolean writable;
	private boolean readable;
	
	//map matching
	private MapMatch match;
	private double matchedLat;
	private double matchedLong;
    private double[] matchResult;
    private boolean doMatch;
    private String previousWayType;
    
    //weather check
	private Handler mHandler;
	private Runnable weatherCheck;
	private boolean weatherFlag;
	
	//server post
	private String URL;
	private Entry postEntry;	
	private DefaultHttpClient httpClient;
	private HttpPost postRequest;
	private Gson gson;
	private String gsonInput;
	private StringEntity postInput;
	private HttpResponse response;
	private int postBlock;
	
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int CCM_SET = 4;

    //OBD values
	private int speed;
	private double maf;
	private int map;
	private int rpm;
	private int iat;
	private String cs = ">";
	private int consumptionCounter = 0;
    private String message = "";
    private static double ccm;
    private double ve;
    private double c;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    boolean btAvailable;
    

	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(savedInstanceState != null){
			Log.d(LOG_TAG, "saved instance sate not null");
	    }
		
		setContentView(R.layout.activity_main);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		Log.d(LOG_TAG, "created!");
		
		/*
		 * check if external storage is writable/readable. If true, check if .txt file exists and
		 * if not create it. Used to store data into text file. Not used, kept for future development
		 */
		if(isExternalStorageWritable()){
			Log.d(LOG_TAG, "external writable");
			myFile = openFile("gpsData.txt");
			Log.d(LOG_TAG, "file opened" + myFile.getPath());
		    match = new MapMatch();
		    URL = Prefs.getSettings("post");
			writable = true;
			readable = true;
			if(dbExists()){
				Log.d(LOG_TAG, "database exists");
			}
		}
		else if(isExternalStorageReadable()){
		    match = new MapMatch();
		    URL = Prefs.getSettings("post");
		    writable = false;
		    readable = true;
		    if(dbExists()){
				Log.d(LOG_TAG, "database exists");
			}
		}
		else{
		    URL = "";
			writable = false;
			readable = false;
		}
		
		// Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
        	btAvailable = false;
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }
        else{
        	btAvailable = true;
        }
				
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		Log.d(LOG_TAG, "system service retrieved");
		
		//initialize variables needed at this point
	    latMatched = (TextView)findViewById(R.id.matchedlat);
	    longMatched = (TextView)findViewById(R.id.matchedlong);
	    source = (TextView)findViewById(R.id.source);
	    take = (TextView)findViewById(R.id.take);
	    avgSpd = (TextView)findViewById(R.id.avgspeed);
	    avgHead = (TextView)findViewById(R.id.avgheading);
	    avgConsmpt = (TextView)findViewById(R.id.avgconsumption);
	    wayName = (TextView)findViewById(R.id.wayname);
	    wayType = (TextView)findViewById(R.id.waytype);
	    waySpeedLimit = (TextView)findViewById(R.id.speedlimit);
	    postResponse = (TextView)findViewById(R.id.post);
    	button = (TextView)findViewById(R.id.button_toggle);

	    lastLat = 10000;
	    lastLong = 10000;
	    run = 0;
	    takeCount = 0;
	    avgSpeed = 0;
	    gpsFixed = false;
	    doLog = false;
		matchResult = new double[2];
		doMatch = true;
		ccm = 1.4 / 2.0;
		
		// The Handler that gets information back from the BluetoothChatService
		mHandler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	        	
	            switch (msg.what) {
	            case MESSAGE_STATE_CHANGE:
	                Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
	                switch (msg.arg1) {
	                case BluetoothChatService.STATE_CONNECTED:
	                    startTransmission();
	                    
	                    break;
	                case BluetoothChatService.STATE_CONNECTING:
	                    break;
	                case BluetoothChatService.STATE_LISTEN:
	                	break;
	                case BluetoothChatService.STATE_NONE:
	                    break;
	                }
	                break;
	            case MESSAGE_WRITE:
	                break;
	            case MESSAGE_READ:
	                byte[] readBuf = (byte[]) msg.obj;
	                // construct a string from the valid bytes in the buffer
	                String readMessage = new String(readBuf, 0, msg.arg1);
	                
	                message = message + readMessage;
	                
	                if(message.contains(cs)){
	                	OBDMonitor2(message);
	                }
	                break;
	            case MESSAGE_DEVICE_NAME:
	                // save the connected device's name
	                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
	                Toast.makeText(getApplicationContext(), "Connected to "
	                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
	                break;
	            case MESSAGE_TOAST:
	                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
	                               Toast.LENGTH_SHORT).show();
	                break;
	            }
	        }
	    };
		
	    //initialize variables needed at this point
		weatherFlag = true;
		postEntry = new Entry();
		postRequest = new HttpPost(URL);
		gson = new Gson();
		postBlock = 0;
		previousWayType = "New type";
	    	    
		Log.d(LOG_TAG, "init done");
		
		//get username and password from Login activity
		Intent intent = getIntent();
	    userName = intent.getStringExtra("userName");
	    passWord = intent.getStringExtra("userPass");
	    
	    Log.d(LOG_TAG, "userpass " + userName + " " + passWord);
		
	    //set username and password to Entry object that is used for sending data to server
		postEntry.setUsername(userName);
		postEntry.setUserpass(passWord);

		//timer set to 5 minutest that sets weather flag to true
	    weatherCheck = new Runnable() {
	        public void run() {
	            Log.d(LOG_TAG,"Awake");

	            weatherFlag = true;
	            mHandler.postDelayed(weatherCheck, 300000);
	        }
	    };
	    
	    //gps listener used for detecting loss of gps signal and in this case reseting variables before new take
		gpsListener = new Listener() {
			@Override
		    public void onGpsStatusChanged(int event){
				switch (event) 
		        {
		            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
		            	gpsFixed = (SystemClock.elapsedRealtime() - mLastLocationMillis) < 3000;
		            	if(!gpsFixed){
		            		mLat = 0;
			    	    	mLong = 0;
			    	    	mSpeed = 0;
				    		headDirection = 0;
				    		run = 0;
				    		takeCount = 0;
				    		avgSpeed = 0;
			        		take.setText(String.format("%.2f",run)+" m");
				        	source.setText("Waiting for GPS...");
		            	}
		            	break;
		            case GpsStatus.GPS_EVENT_FIRST_FIX:
		            	gpsFixed = true;
		            	break;
		            case GpsStatus.GPS_EVENT_STARTED:
		            	break;
		            case GpsStatus.GPS_EVENT_STOPPED:
		            	break;
		            default:
		            	break;
		        }
			}
		};

		/*
		 * location listener used to gather gps data every time location changes.
		 */
		listener = new LocationListener() {

	        @Override
	        public void onLocationChanged(Location location) {       	
	    		Log.d(LOG_TAG, "onLocationChanged started");
	        	
	    	    gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	    	    
	    		Log.d(LOG_TAG, "location retrieved");
	    		
	    		mLastLocationMillis = SystemClock.elapsedRealtime();

	    		//when location changes check if gps is fixed. If not ignore this location change
	    		if(gpsFixed){
	    			Log.d(LOG_TAG, "Location changed - use of GPS");
		    		Log.d(LOG_TAG, "Weather flag: " + weatherFlag);
		        	source.setText("GPS connected!");
	    			
		        	mLat = gpsLocation.getLatitude();
		        	mLong = gpsLocation.getLongitude();
		        	mSpeed = gpsLocation.getSpeed();
		    		
		        	//if location of previous and current location is the same, set match flag to false, se map matching will not run
		    		if((mLat == lastLat) && (mLong == lastLong)){
		    			doMatch = false;
		    		}
		    		else{
		    			doMatch = true;
		    		}
		    			        		
	        		/*
	        		 * match current location to map if location is actually changed. This part also calculates distance
	        		 * between this and previous location, heading of this move and sets data and counters for averages
	        		 */
	        		if(readable && doMatch){
		        		directionResults = distanceCalculator(gpsLocation);
		        		headDirection = (directionResults[1]+360)%360;
		        		tempDistance = directionResults[0];
		        		
		        		headDirection = Math.round(headDirection*100);
		        		headDirection /= 100;
		        		
			        	avgHeading += headDirection;
			        	headingTakeCount++;
	        			
		        		match.match(mLat, mLong, headDirection, matchResult);
		        		matchedLat = matchResult[0];
		        		matchedLong = matchResult[1];
		        		latMatched.setText(String.valueOf(matchedLat));
		        		longMatched.setText(String.valueOf(matchedLong));
		        		wayName.setText(match.getCurrentWayName());
		    			Log.d(LOG_TAG, "wayName set");
		        		wayType.setText(match.getCurrentWayType());
		    			Log.d(LOG_TAG, "wayType set");
		        		waySpeedLimit.setText(String.valueOf(match.getSpeedLimit()));
		    			Log.d(LOG_TAG, "waySpeedLimit set");
	        		}
	        		else{
	        			tempDistance = 0;
	        		}
	        		
	        		//current take length
		        	run += tempDistance;
		        	//take counter
		        	takeCount++;
		        	//average speed calculation value
		        	avgSpeed += mSpeed;
		        	
		        	//detection of way type change. If this happens reset take variables
		        	if(!previousWayType.equals(match.getCurrentWayType())){
		        		run = 0;
		        		avgSpeed = mSpeed;
		        		avgHeading = headDirection;
		        		avgConsumption = 0;
		        		consumptionCounter = 0;
		        		takeCount = 0;
		        		headingTakeCount = 0;
		        	}
		        			        
		        	/*
		        	 * check if take is 500m long. If it is (or longer due to location change) calculate averages
		        	 * and send them to server via PostTask. Also updates UI and saves data into Entry object.
		        	 * Resets variables
		        	 */
		        	if(run >= 500.0){
		        		avgSpeed /= takeCount;
		        		avgHeading /= headingTakeCount;
		        		//avgConsumption /= takeCount;
		        		tempAvgConsumption = avgConsumption / consumptionCounter;
		        		avgSpd.setText(String.format("%.2f",avgSpeed*3.6)+" km/h");
		        		avgHead.setText(String.valueOf((int)avgHeading)+"°");
		        		avgConsmpt.setText(String.format("%.1f",tempAvgConsumption)+" l/100 km");
		        		
			        	postEntry.setSpeedLimit(match.getSpeedLimit());
		        		postEntry.setLatitude(matchedLat);
		        		postEntry.setLongitude(matchedLong);
		        		postEntry.setSpeed(avgSpeed*3.6);
		        		postEntry.setConsumption(tempAvgConsumption);
		        		postEntry.setHeading((int)avgHeading);
		        		postEntry.setRoadType(match.getCurrentWayType());
		        		postEntry.setWeatherDataCheck(weatherFlag);
		        		/*
			        	if(doLog && writable){
			        		writeToFile(myFile, String.format("%.15f",mLat)+";"+String.format("%.15f",mLong)+";"+String.format("%.3f",mSpeed)+";"+String.format("%.2f",headDirection)+
			        				";"+Integer.toString((int)SystemClock.elapsedRealtime()/1000)+";"+match.getCurrentWayName()+
			        				";"+String.format("%.15f",matchedLat)+";"+String.format("%.15f",matchedLong)+";"+weatherFlag+"\r\n");
			        	}
		        		*/
		        		if(isOnline() && postBlock > 1 && doLog){
				            new PostTask().execute();
				            weatherFlag = false;
		        		}
		        		else if(isOnline() && postBlock <= 1 && doLog){
		        			postResponse.setText("Initializing...");
		        		}
		        		else{
		        			postResponse.setText("Not connected!");
		        		}
		        		
		        		run = 0;
		        		avgSpeed = 0;
		        		avgHeading = 0;
		        		avgConsumption = 0;
		        		consumptionCounter = 0; 
		        		takeCount = 0;
		        		headingTakeCount = 0;
		        		postBlock++;
		        	}		        	
		        	previousWayType = match.getCurrentWayType();
	    	    }

        		take.setText(String.format("%.2f",run)+" m");
	        }

	        @Override
	        public void onProviderDisabled(String provider) {
	        }

	        @Override
	        public void onProviderEnabled(String provider) {
	        }

	        @Override
	        public void onStatusChanged(String provider, int status, Bundle extras) {
	        }
	    };
			    
	}
	
	/**
	 * @param location
	 * @return latitude and longitude
	 * used to get distance between previous location and current location
	 */
	protected float[] distanceCalculator(Location location){
		float[] results = new float[3];
		if(location == null){
			return results;
		}
		else if(lastLat == 10000 && lastLong == 10000 & location != null){
			lastLat = location.getLatitude();
			lastLong = location.getLongitude();
			return results;
		}

		Location.distanceBetween(lastLat, lastLong, location.getLatitude(), location.getLongitude(), results);
		
		lastLat = location.getLatitude();
		lastLong = location.getLongitude();

		return results;
	}
	
	/**
	 * @param gpsLoc
	 * @param netLoc
	 * @return
	 * Unused, kept for future development
	 */
	protected boolean isBetterLocation(Location gpsLoc, Location netLoc) {
	    if (netLoc == null || gpsLoc == null) {
	        return false;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = gpsLoc.getTime() - netLoc.getTime();
	    boolean isNewer = timeDelta > 0;

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (gpsLoc.getAccuracy() - netLoc.getAccuracy());
	    boolean isMoreAccurate = accuracyDelta < 0;

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isNewer && isMoreAccurate) {
	        return true;
	    }
	    return false;
	}
	
	/**
	 * @param fileName
	 * @return file
	 * Opens file
	 */
	private File openFile(String fileName){
		File file;
		try {
			file = new File(Environment.getExternalStorageDirectory() + "/gpsData");
		    if (file.mkdirs()) {
		        Log.d(LOG_TAG, "directory created");
		    }
		    else if(file.exists()){
		    	Log.d(LOG_TAG, "directory already exists");
		    }
		    else{
		    	Log.e(LOG_TAG, "directory not created");
		    }
		    
		    file = new File(Environment.getExternalStorageDirectory() + "/gpsData/"+fileName);
			if (file.createNewFile()) {
			    Log.d(LOG_TAG, "file created");
			}
			else if(file.exists()){
				Log.d(LOG_TAG, "file already exists");
			}
			else{
				Log.e(LOG_TAG, "file not created");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return file;
	}
	
	/**
	 * @return true if database exists
	 * Check if database that contains maps exists
	 */
	private boolean dbExists(){
		File file = new File(Environment.getExternalStorageDirectory() + "/gpsData/map.db");
		if(file.exists()){
			return true;
		}
		else{
			return false;
		}		
	}
	
	/**
	 * @param file
	 * @param text
	 * Writing data into file
	 */
	private void writeToFile(File file, String text){
		try {
			FileWriter fWriter = new FileWriter(file, true);
			fWriter.append(text);
			fWriter.flush();
			fWriter.close();
    		Log.d(LOG_TAG, "write to file "+text);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks if external storage is available for read and write
	 */
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}

	/**
	 * Checks if external storage is available to at least read
	 */
	public boolean isExternalStorageReadable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state) ||
	        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
	/**
	 * @return true if data connection is present
	 * Check data connection (mobile or wifi)
	 */
	public boolean isOnline() {
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}
	
	/**
	 * @param view
	 * Called when the user clicks the Log button. Pusts empty line to separate each recording
	 */
    public void toggleLog(View view) {
        // Do something in response to button
    	if(doLog){
    		button.setText("Not logging");
    		doLog = false;
    		if(writable){
    			writeToFile(myFile, "Session end\r\n\r\n");
    		}
    	}
    	else{
    		button.setText("Logging");
    		doLog = true;
    		if(writable){
    			writeToFile(myFile, "Session start\r\n");
    		}
    	}
    }

    /**
     * Start all needed services and reset variables used for averages calculation
     */
    @Override
	protected void onResume(){
		super.onResume();
		locationManager.addGpsStatusListener(gpsListener);
	    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
		Log.d(LOG_TAG, "on resume");
		mLat = 0;
    	mLong = 0;
    	mSpeed = 0;
		headDirection = 0;
		run = 0;
		takeCount = 0;
		avgSpeed = 0;
		match.reconnect(this);
		locationManager.addGpsStatusListener(gpsListener);
	    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
	    mHandler.postDelayed(weatherCheck, 30000);
	    
	    if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
	}
	
    /**
     * Closes needed services
     */
    @Override
	protected void onPause(){
		super.onPause();
		Log.d(LOG_TAG, "on pause");
		mHandler.removeCallbacks(weatherCheck);
		locationManager.removeUpdates(listener);
		locationManager.removeGpsStatusListener(gpsListener);
		match.disconnect();
	}
    
    /**
     * Does part of bluetooth initialization
     */
    @Override
    protected void onStart(){
    	super.onStart();
		Log.d(LOG_TAG, "on start");
		httpClient = new DefaultHttpClient();
		
		if (btAvailable && !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else if(btAvailable){
            if (mChatService == null) setupChat();
        }
    }
    
    /**
     * closes http connection
     */
    @Override
    protected void onStop(){
    	super.onStop();
    	Log.d(LOG_TAG, "on stop");
    	httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * @param msg
     * Used for periodicaly send PIDs to OBD II device and for consumption calculation
     */
    public void OBDMonitor2(String msg){
    	if(!OBDParser.checkResponse(msg)){
    		OBDMonitor2("41 FF FF FF");
    		return;
    	}
    	int caseSwitch = OBDParser.parseResponse(msg);
    	String message1;
    	
    	switch(caseSwitch){
    		case 11:
    			map = OBDParser.parseVSS(msg);
    			message = "";
    			message1 = "01 0C";
    			break;
    		case 12:
    			rpm = (int)OBDParser.parseMAF(msg);
    			message = "";
    			message1 = "01 0D";
    			break;
    		case 13:
    			speed = OBDParser.parseVSS(msg);
    			message = "";
    			message1 = "01 0F";
    			break;
    		case 15:
    			iat = OBDParser.parseVSS(msg);
    			iat = iat + 233;
    			message = "";
    			message1 = "01 0B";
    			
    			if(speed >= 0 && iat >= 233 && map >= 0 && rpm >= 0){
		            //calculation
		            ve = 0.75;
		            maf = ((double)map/(double)iat)*3.484*((double)rpm/60)*ccm*ve;
		            if(speed == 0){
		            	c += (3600 * maf)/(14.7 * 730.00);
		            }
		            else{
			            c += ((3600 * maf)/(14.7 * 730.00 * speed))*100;

			            avgConsumption = avgConsumption + c;
			            consumptionCounter++;
			            
			            c = 0;
		            }
	            }
    			
    			break;
    		default:
    			c = -1;
    			
    			message = "";
    			message1 = "01 0B";
    			Log.d(TAG,"send map d");
    			break;
    	}
    	
    	sendMessage(message1);
    }
    
    /**
     * Initalization of OBD II device and connection
     */
    public void startTransmission() {
    	    	
    	try {
    		if(true){
				Thread.sleep(2000);
				sendMessage("ATD");
				Log.d(TAG, "atd");
				Thread.sleep(3000);

		    	sendMessage("ATZ");
				Log.d(TAG, "atz");
				Thread.sleep(3000);
				
		    	sendMessage("AT E0");
				Log.d(TAG, "ate0");
				Thread.sleep(500);
							
		    	sendMessage("ATSP 4");
				Log.d(TAG, "atsp");
				Thread.sleep(500);

		    	sendMessage("01 00");
				Log.d(TAG, "01 00");
				
				new StartChatTask().execute();
    		}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }

    /**
     * Initialize bluetoothChatService used for communication with OBD II device
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");
    	
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }
    
    /**
     * Sends dummy PID to obdMonitor2 method for communication start
     */
    private void startChat(){
    	Log.e(TAG, "startChat() auto");
    	OBDMonitor2("41 FF FF FF");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null){
        	mChatService.stop();
        }
    }
    
    /**
     * @param message
     * Used for sending message (PID request) to OBD II device
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
        	//add \r that indicates end of command
        	message = message + '\r';
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case CCM_SET:
            if (resultCode == Activity.RESULT_OK) {
                ccm = data.getExtras().getDouble("ccm");
                Log.d(TAG, "onActivityResult ccm " + ccm);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		Intent ccmIntent = null;
		switch (item.getItemId()) {
		case R.id.ccm:
            // Launch the DeviceListActivity to see devices and do scan
			// select engine volume activity / done
			ccmIntent = new Intent(this, CcmActivity.class);
            startActivityForResult(ccmIntent, CCM_SET);
            return true;
		case R.id.goToMap:
	    	Intent intent = new Intent(this, MapActivity.class);
			intent.putExtra("userName", userName);
			intent.putExtra("userPass", passWord);
	    	startActivity(intent);
			return true;
		case R.id.scanForDevices:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        }
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * @author Rok Pajk Kosec
	 * Used to send post request to service that gathers used data
	 */
	private class PostTask extends AsyncTask<Void, Void, Void>{
	    
	   @Override
	   protected Void doInBackground(Void... params) {
		   try {
			   	gsonInput = gson.toJson(postEntry);
				
				postInput = new StringEntity(gsonInput);
				postInput.setContentType("application/json");
				postRequest.setEntity(postInput);
		 
				response = httpClient.execute(postRequest);
		 
			  } catch (MalformedURLException e) {
		 
				e.printStackTrace();
		 
			  } catch (IOException e) {
		 
				e.printStackTrace();
		 
			  }
		   return null;
		}
	   	
	   	@Override
	   	protected void onPostExecute(Void res)
	   	{	 
			if (response == null) {				
				postResponse.setText("Fail!");
			}
			else if(response.getStatusLine().getStatusCode() != 201){
				Log.d(LOG_TAG, "Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
				postResponse.setText("Fail " + response.getStatusLine().getStatusCode());
			}
			else{
				postResponse.setText("Success!");
			}
	   	}
	}
	
	/**
	 * @author Rok Pajk Kosec
	 * Used to do async thread pause that waits for OBD II to set up and so does not stop UI
	 */
	private class StartChatTask extends AsyncTask<Void, Void, Void>{
	    
		@Override
		protected Void doInBackground(Void... params) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void res) {
			startChat();
		}
	}
}
