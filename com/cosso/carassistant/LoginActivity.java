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

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * @author Rok Pajk Kosec
 * Activity that is started when application is started. It is used to check on server if
 * user exists and if his login details are correct. Also it is used for registration of
 * new user. Returns error if username already exists
 */
public class LoginActivity extends Activity {
	private final String LOG_TAG = "Log tag";
	
	private EditText userName;
	private EditText passWord;
	private EditText retypePass;
	private TextView loginLabel;
	private Button buttonLogin;
	private Button buttonRegister;
	
	//toggle between login and register mode
	private boolean register;
	
	//login service url
	private String URLlogin;
	//register service url
	private String URLregister;
	private DefaultHttpClient httpClient;
	private HttpGet loginRequest;
	private HttpResponse response;
	//login response - 1 if login successful, else 0
	private int loginResponse;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		if(isExternalStorageReadable()){
			URLlogin = Prefs.getSettings("login");
			URLregister = Prefs.getSettings("register");
		}
		else{
			URLlogin = "";
			URLregister = "";
		}
		
		buttonLogin = (Button)findViewById(R.id.button_login);
		buttonRegister = (Button)findViewById(R.id.button_register);
		userName = (EditText)findViewById(R.id.usernametext);
		passWord = (EditText)findViewById(R.id.passwordtext);
		loginLabel = (TextView)findViewById(R.id.loginlabel);
		retypePass = (EditText)findViewById(R.id.password2text);
		retypePass.setVisibility(View.GONE);
		register = false;
		loginResponse = 0;
	}
	
    @Override
    protected void onStart(){
    	super.onStart();
		Log.d(LOG_TAG, "on start login");
		httpClient = new DefaultHttpClient();
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    	Log.d(LOG_TAG, "on stop login");
    	httpClient.getConnectionManager().shutdown();
    }
	
    /**
     * @param view
     * when login/register button is pressed
     */
	public void Login(View view){
		Log.d(LOG_TAG, "login act " + userName.getText().toString() + " " + passWord.getText().toString());
		if(!register && !userName.getText().equals("") && !passWord.getText().equals("")){
			loginLabel.setText("Logging in...");
			new LoginTask().execute(userName.getText().toString(), passWord.getText().toString());
		}
		else if(!register){
			loginLabel.setText("Wrong user/pass!");
		}
		else{
			if(passWord.getText().toString().equals(retypePass.getText().toString())){
				new RegisterTask().execute(userName.getText().toString(), passWord.getText().toString());
			}
			else{
				loginLabel.setText("Passwords do not match!");
			}
		}
	}
	
	/**
	 * @param view
	 * When register button is pressed
	 */
	public void Register(View view){
		if(retypePass.getVisibility() == View.GONE){
			retypePass.setVisibility(View.VISIBLE);
			buttonRegister.setText("Back");
			buttonLogin.setText("Register");
			register = true;
		}
		else{
			retypePass.setVisibility(View.GONE);
			buttonRegister.setText("New user");
			buttonLogin.setText("Login");
			register = false;
		}
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
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}
	
	/**
	 * return intent
	 */
	private void goToMain(){
		Intent intent = new Intent(this, MainActivity.class);
		intent.putExtra("userName", userName.getText().toString());
		intent.putExtra("userPass", passWord.getText().toString());
		startActivity(intent);
		finish();
	}
	
	/**
	 * @author Rok Pajk Kosec
	 * async task for sending login/register request to server
	 */
	private class LoginTask extends AsyncTask<String, Void, Void>{
	    
		@Override
		protected Void doInBackground(String... params) {
			try {
				List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
				httpParams.add(new BasicNameValuePair("name", params[0]));
				httpParams.add(new BasicNameValuePair("pass", params[1]));

				loginRequest = new HttpGet(URLlogin + "?" + URLEncodedUtils.format(httpParams, "utf-8"));

				response = httpClient.execute(loginRequest);
				
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StringBuffer sb = new StringBuffer("");
				String l = "";

				while ((l = in.readLine()) != null) {
					sb.append(l);
				}
				in.close();
				loginResponse = Integer.valueOf(sb.toString());

				Log.d(LOG_TAG, "HTTP code : " + response.getStatusLine().getStatusCode() + " " + ", " + loginResponse);

			} catch (MalformedURLException e) {

				e.printStackTrace();

			} catch (IOException e) {

				e.printStackTrace();

			}
			return null;
		}

		@Override
		protected void onPostExecute(Void res) {
			if(loginResponse == 1){
				goToMain();
			}
			else{
				loginLabel.setText("Wrong user/pass!");
			}
		}
	}
	
	private class RegisterTask extends AsyncTask<String, Void, Void>{
	    
		@Override
		protected Void doInBackground(String... params) {
			try {

				List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
				httpParams.add(new BasicNameValuePair("name", params[0]));
				httpParams.add(new BasicNameValuePair("pass", params[1]));

				loginRequest = new HttpGet(URLregister + "?" + URLEncodedUtils.format(httpParams, "utf-8"));

				response = httpClient.execute(loginRequest);

				BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StringBuffer sb = new StringBuffer("");
				String l = "";

				while ((l = in.readLine()) != null) {
					sb.append(l);
				}
				in.close();
				loginResponse = Integer.valueOf(sb.toString());

				Log.d(LOG_TAG, "HTTP code1 : " + response.getStatusLine().getStatusCode() + " " + ", " + loginResponse);

			} catch (MalformedURLException e) {

				e.printStackTrace();

			} catch (IOException e) {

				e.printStackTrace();

			}
			return null;
		}

		@Override
		protected void onPostExecute(Void res) {
			if(loginResponse == 0){
				loginLabel.setText("Username already exists!");
			}
			else{
				loginLabel.setText("Registration successful!");
				retypePass.setVisibility(View.GONE);
				buttonRegister.setText("New user");
				buttonLogin.setText("Login");
				register = false;
			}
		}
	}

}
