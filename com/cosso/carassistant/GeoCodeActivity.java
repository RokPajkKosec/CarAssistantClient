package com.cosso.carassistant;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * @author Rok Pajk Kosec
 * Activity used to search for address or POI. It uses Google geo coding service.
 * Requests are relayed via the sam server used for other services of this system.
 */
public class GeoCodeActivity extends Activity {
	private final String LOG_TAG = "Log tag";
	private ListView listView;
	private TextView search;
	//list of location names
	private List<String> locationsList;
	//list of addresses. inclouding latitude and longitude.
	private List<Address> addressesList;
	private String searchString;
	//array adapter for updating UI
	private ArrayAdapter<String> adapter;
	//used in return intent to map activity. Index of selected result
	private int returnIndex;
	//url to service that relays request to Google
	private String URLgeoCode;	
	private DefaultHttpClient httpClient;
	private HttpGet geoRequest;
	private HttpResponse response;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_geo_code);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		if(isExternalStorageReadable()){
			URLgeoCode = Prefs.getSettings("geocode");
		}
		else{
			URLgeoCode = "";
		}
		
		listView = (ListView)findViewById(R.id.listViewLocations);
		search = (TextView)findViewById(R.id.editTextFind);

		addressesList = new ArrayList<Address>();
		locationsList = new ArrayList<String>();
		searchString = "";
		returnIndex = 0;
		
	    adapter = new ArrayAdapter<String>(this, R.layout.custom_list_item, locationsList);
	    listView.setAdapter(adapter);
	    
	    listView.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> myAdapter, View myView, int myItemInt, long mylng) {
	          returnIndex = myItemInt;
	          Log.d(LOG_TAG, "on click " + returnIndex);
	          finish();
	        }                 
	  });
	}
	
	@Override
	public void finish() {
		Log.d(LOG_TAG, "finish");
	  // Prepare data intent 
	  Intent data = new Intent();
	  data.putExtra("latitude", addressesList.get(returnIndex).getLatitude());
	  data.putExtra("longitude", addressesList.get(returnIndex).getLongitude());
	  // Activity finished ok, return the data
	  setResult(RESULT_OK, data);
	  super.finish();
	} 

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.geo_code, menu);
		return true;
	}
	
	@Override
	public void onBackPressed(){
		NavUtils.navigateUpFromSameTask(this);
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
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * @param view
	 * Finds location based on search query from edit text field
	 */
	public void findLocations(View view) {
        // Do something in response to button
		Log.d(LOG_TAG, "button ");
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
		searchString = search.getText().toString();
		testConn();
		
		new GetLatLngTask().execute();
		search.setText("");
    }
	
	private void testConn(){
		ConnectivityManager connMgr = (ConnectivityManager) 
		        getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI); 
		boolean isWifiConn = networkInfo.isConnected();
		networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		boolean isMobileConn = networkInfo.isConnected();
		Log.d(LOG_TAG, "Wifi connected: " + isWifiConn);
		Log.d(LOG_TAG, "Mobile connected: " + isMobileConn);
		
	    boolean test = (networkInfo != null && networkInfo.isConnected());
	    
	    Log.d(LOG_TAG, "Online: " + test);
	}
	
    @Override
    protected void onStart(){
    	super.onStart();
		Log.d(LOG_TAG, "on start geo");
		httpClient = new DefaultHttpClient();
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    	Log.d(LOG_TAG, "on stop geo");
    	httpClient.getConnectionManager().shutdown();
    }
	
    /**
     * @param address
     * Used in async task for sending request to server and recieving result
     */
	public void getLatLongFromAddress(String address) {
		address = address.replaceAll(" ", "%20");
		
		Log.d(LOG_TAG, "adddress: " + address);
		
        List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
		httpParams.add(new BasicNameValuePair("address", address));

		geoRequest = new HttpGet(URLgeoCode + "?" + URLEncodedUtils.format(httpParams, "utf-8")); //%20

        StringBuilder stringBuilder = new StringBuilder();

        String tempAddressString = "";
        double latitude;
        double longitude;
        Address tempAddress = null;
        addressesList.clear();
        
        try {
            response = httpClient.execute(geoRequest);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            int b;
            while ((b = stream.read()) != -1) {
                stringBuilder.append((char) b);
            }
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }
        
        Log.d(LOG_TAG, "sb: " + stringBuilder.toString());

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(stringBuilder.toString());
            	for(int i=0; i<((JSONArray)jsonObject.get("results")).length(); i++){
            		tempAddressString = ((JSONArray)jsonObject.get("results")).getJSONObject(i).getString("formatted_address");
            		latitude = ((JSONArray)jsonObject.get("results")).getJSONObject(i)
                            .getJSONObject("geometry").getJSONObject("location")
                            .getDouble("lat");
            		longitude = ((JSONArray)jsonObject.get("results")).getJSONObject(i)
                            .getJSONObject("geometry").getJSONObject("location")
                            .getDouble("lng");
            		tempAddress = new Address(Locale.getDefault());
            		tempAddress.setAddressLine(0, tempAddressString);
            		tempAddress.setLatitude(latitude);
            		tempAddress.setLongitude(longitude);
            		addressesList.add(tempAddress);
            	}

            Log.d(LOG_TAG,"json");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

	private class GetLatLngTask extends AsyncTask<Void, Void, Void>{
	    @Override
	    protected void onPreExecute(){
			Log.d(LOG_TAG, "listL1 " + locationsList.size());
			Log.d(LOG_TAG, "listA " + adapter.getCount());
			locationsList.clear();
	    }
		
		@Override
		protected Void doInBackground(Void... params) {
			getLatLongFromAddress(searchString);
			Log.d(LOG_TAG, "atask");
			return null;
		}

		@Override
		protected void onPostExecute(Void res) {
			Log.d(LOG_TAG, "poste");
			Log.d(LOG_TAG, "listL2 " + locationsList.size());
			Log.d(LOG_TAG, "listA " + adapter.getCount());
			
			//update UI
			for(Address a : addressesList){
				locationsList.add(new String(a.getAddressLine(0)));
			}
			adapter.notifyDataSetChanged();
		}
	}

}
