package com.cosso.carassistant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.PathOverlay;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Rok Pajk Kosec
 * Activity for displaying map and estimated consumption or time to destination. Route to destination
 * is displayed on map via path overlay
 */
public class MapActivity extends FragmentActivity {
	private final String LOG_TAG = "Log tag";
	private MapController mapController;
    private MapView mapView;
    private TextView mapText;
    private MyLocationOverlay myLocationOverlay = null;
    
	//url to service that generates rules from data
	private String URLrules;
	//url to service that calculates path and returns it to thi activity
	private String URLpath;
	private DefaultHttpClient httpClient;
	private HttpGet rulesRequest;
	private HttpGet pathRequest;
	private HttpResponse response;
	private HttpParams httpParameters;
	private String userName;
	//target class used for path calculation and rules generation requests. By default is speed difference
	private static String targetClass = "speedDifference";
	private List<GeoPoint> geoPointList;
	private PathOverlay myPath;
	//expected cost of path
	private double g;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		// Show the Up button in the action bar.
		setupActionBar();
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		if(isExternalStorageReadable()){
			URLrules = Prefs.getSettings("rules");
			URLpath = Prefs.getSettings("path");
		}
		else{
			URLrules = "";
			URLpath = "";
		}
		
		Intent intent = getIntent();
	    userName = intent.getStringExtra("userName");
	    
	    httpParameters  = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(httpParameters, 300000);
	    HttpConnectionParams.setSoTimeout(httpParameters, 300000);
	    
	    mapText = (TextView)findViewById(R.id.maptext);
		
		rulesRequest = new HttpGet(URLrules);
		pathRequest = new HttpGet(URLpath);
		geoPointList = new ArrayList<GeoPoint>();
		
		mapView = (MapView) findViewById(R.id.mapview);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true); 
        
        mapController = mapView.getController();
        mapController.setZoom(15);
           
        Drawable marker=getResources().getDrawable(android.R.drawable.star_big_on);
        int markerWidth = marker.getIntrinsicWidth();
        int markerHeight = marker.getIntrinsicHeight();
        marker.setBounds(0, markerHeight, markerWidth, 0);
                
        myLocationOverlay = new MyLocationOverlay(this, mapView);
        mapView.getOverlays().add(myLocationOverlay);
        myLocationOverlay.enableFollowLocation();
        
        myLocationOverlay.enableMyLocation();
        
        myLocationOverlay.setDrawAccuracyEnabled(true);
        myLocationOverlay.runOnFirstFix(new Runnable() {
        public void run() {
                mapController.animateTo(myLocationOverlay.getMyLocation());
            }
        });
        mapView.getOverlays().add(myLocationOverlay);
        
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		Log.d("log tag", "map on pause");

		myLocationOverlay.disableMyLocation();
		myLocationOverlay.disableCompass();
		myLocationOverlay.disableFollowLocation();
	}
	
    @Override
    protected void onStart(){
    	super.onStart();
		Log.d(LOG_TAG, "on start");
		httpClient = new DefaultHttpClient(httpParameters);
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    	Log.d(LOG_TAG, "on stop");
    	httpClient.getConnectionManager().shutdown();
    }
	
    protected boolean isRouteDisplayed() {
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
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.selectTarget:
			Log.d(LOG_TAG, targetClass);
			new ClassDialogFragment().show(getSupportFragmentManager(), "classDialogFragment");
			return true;
		case R.id.getRules:
			// get na server pa toast response / done
			new GetRulesTask().execute();
			return true;
		case R.id.getPath:
			// najprej nov okn, kjer vpises destinacijo, da dobiš x,y (google service) potem pa get na server response je pot. / done
			Intent intent = new Intent(this, GeoCodeActivity.class);
			startActivityForResult(intent, 1);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // Check which request we're responding to
	    if (requestCode == 1) {
	        // Make sure the request was successful
	        if (resultCode == RESULT_OK) {
	        	Log.d(LOG_TAG, "lat return " + data.getExtras().getDouble("latitude"));
	        	Log.d(LOG_TAG, "long return " + data.getExtras().getDouble("longitude"));
	        	new GetPathTask().execute(data.getExtras().getDouble("latitude"), data.getExtras().getDouble("longitude"));
	        }
	    }
	}
	
	private void toaster(String text){
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Initialize path overlay
	 */
	private void getNewPathOverlay(){
		mapView.getOverlays().remove(myPath);
		myPath = new PathOverlay(Color.RED, this);
		Paint p = new Paint();
		p.setColor(Color.RED);
		p.setAlpha(100);
		p.setStyle(Paint.Style.STROKE);
		p.setStrokeWidth(6);
		myPath.setPaint(p);
	}
		
	/**
	 * @author Rok Pajk Kosec
	 * Async task that sends request for path for target class that is currently selected
	 * and recieves it in JSON format. Also puts new overlay on map
	 */
	private class GetPathTask extends AsyncTask<Double, Void, Void>{
	    
		@Override
		protected Void doInBackground(Double... params) {
			try {

				List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
				httpParams.add(new BasicNameValuePair("startLat", String.valueOf((double)(myLocationOverlay.getMyLocation().getLatitudeE6())/1000000.0)));
				httpParams.add(new BasicNameValuePair("startLong", String.valueOf((double)(myLocationOverlay.getMyLocation().getLongitudeE6())/1000000.0)));
				httpParams.add(new BasicNameValuePair("targetLat", String.valueOf(params[0])));
				httpParams.add(new BasicNameValuePair("targetLong", String.valueOf(params[1])));
				httpParams.add(new BasicNameValuePair("targetClass", targetClass));
				httpParams.add(new BasicNameValuePair("name", userName));

				pathRequest = new HttpGet(URLpath + "?" + URLEncodedUtils.format(httpParams, "utf-8"));

				response = httpClient.execute(pathRequest);
				
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StringBuffer sb = new StringBuffer("");
				String l = "";

				while ((l = in.readLine()) != null) {
					sb.append(l);
				}
				in.close();
				String data = sb.toString();
				
				try {
					JSONObject json = new JSONObject(data);
					JSONArray jArray = json.getJSONArray("node");

					g = (double)Long.valueOf(String.valueOf(jArray.getJSONObject(0).get("id"))) / 1000.0;
					
					if (jArray != null) { 					   
					   for (int i=0;i<jArray.length();i++){ 
						   geoPointList.add(new GeoPoint(Double.valueOf(String.valueOf(jArray.getJSONObject(i).get("latitude"))), Double.valueOf(String.valueOf(jArray.getJSONObject(i).get("longitude")))));
					   } 
					}
				} catch (JSONException e) {
					e.printStackTrace();
				} 
				
				Log.d(LOG_TAG, "HTTP code2 : " + response.getStatusLine().getStatusCode() + " " + HttpConnectionParams.getConnectionTimeout(httpParameters) + " " + HttpConnectionParams.getSoTimeout(httpParameters));

			} catch (MalformedURLException e) {

				e.printStackTrace();

			} catch (IOException e) {

				e.printStackTrace();

			}
			return null;
		}

		@Override
		protected void onPostExecute(Void res) {
			if (response == null) {
				toaster("No response");
			} else if (response.getStatusLine().getStatusCode() != 200) {
				Log.d(LOG_TAG, "Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
				toaster(String.valueOf(response.getStatusLine().getStatusCode()));
			} else {
				getNewPathOverlay();
				for(GeoPoint p : geoPointList){
					myPath.addPoint(p);
				}
				mapView.getOverlays().add(myPath);
				toaster("Path calculated");
				
				if(targetClass.equals("consumption")){
					mapText.setText("Expected consumption: " + Double.valueOf(g) + " liters");
				}
				else{
					mapText.setText("Expected time:" + (int)g + " seconds");
				}

			}
		}
	}
	
	/**
	 * @author Rok Pajk Kosec
	 * Async task that sends request for rules generation for target class that is currently selected
	 */
	private class GetRulesTask extends AsyncTask<Void, Void, Void>{
	    
		@Override
		protected Void doInBackground(Void... params) {
			try {

				List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
				httpParams.add(new BasicNameValuePair("name", userName));
				httpParams.add(new BasicNameValuePair("targetClass",
						targetClass));

				rulesRequest = new HttpGet(URLrules + "?" + URLEncodedUtils.format(httpParams, "utf-8"));

				response = httpClient.execute(rulesRequest);

				BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StringBuffer sb = new StringBuffer("");
				String l = "";

				while ((l = in.readLine()) != null) {
					sb.append(l);
				}
				in.close();
				String data = sb.toString();

				Log.d(LOG_TAG, "HTTP code1 : " + response.getStatusLine().getStatusCode() + " " + ", " + data);

			} catch (MalformedURLException e) {

				e.printStackTrace();

			} catch (IOException e) {

				e.printStackTrace();

			}
			return null;
		}

		@Override
		protected void onPostExecute(Void res) {
			if (response == null) {
				toaster("No response");
			} else if (response.getStatusLine().getStatusCode() != 200) {
				Log.d(LOG_TAG, "Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
				toaster(String.valueOf(response.getStatusLine().getStatusCode()));
			} else {
				toaster("Rules calculated");
			}
		}
	}
	
	/**
	 * @author Rok Pajk Kosec
	 * Dialog fragment for selecting target class
	 */
	public static class ClassDialogFragment extends DialogFragment {
			
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			super.onCreateDialog(savedInstanceState);
			CharSequence[] options = new CharSequence[2];
			options[0] = "Speed difference";
			options[1] = "Consumption";
		    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		    builder.setTitle("Choose target class")
		           .setItems(options, new DialogInterface.OnClickListener() {
		               public void onClick(DialogInterface dialog, int which) {
			               switch(which){
			               case 0:
			            	   targetClass = "speedDifference";
			            	   break;
			               case 1:
			            	   targetClass = "consumption";
			            	   break;
			               }   
		           }
		    });
		    return builder.create();
		}
	}

}
