/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cosso.carassistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

/**
 * @author Rok Pajk Kosec
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 * Activity is used to insert volume of engine
 */
public class CcmActivity extends Activity {
    private static final String TAG = "DeviceListActivity";

    private EditText text;
    private double ccm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_ccm);

        text = (EditText)findViewById(R.id.ccmtext);
        
        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);
        
    }
    
    public void setCcm(View view){
    	if(!text.getText().toString().equals("")){
    		ccm = Double.valueOf(text.getText().toString());
    		finish();
    	}
    	else{
    		ccm = 1.4;
    		finish();
    	}
    }
    
    @Override
	public void finish() {
		Log.d(TAG, "finish");
	  // Prepare data intent 
	  Intent data = new Intent();
	  data.putExtra("ccm", ccm);
	  // Activity finished ok, return the data
	  setResult(RESULT_OK, data);
	  super.finish();
	} 

}
