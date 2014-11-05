/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/

package org.apache.cordova.geolocation;

import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

/**
 * This class handles requests for GPS location services.
 *
 */
public class LocationListener extends GoogleLocationListener implements GooglePlayServicesClient.ConnectionCallbacks,GooglePlayServicesClient.OnConnectionFailedListener {
	
    public LocationListener(LocationManager locationManager, GeoBroker m) {
        super(locationManager, m, "[Cordova LocationListener]");
        
        int response = GooglePlayServicesUtil.isGooglePlayServicesAvailable(m.getContext());
        if (response == ConnectionResult.SUCCESS) {
        	locationClient = new LocationClient(m.getContext(), this, this);
        	locationClient.connect();
        	locationRequest = LocationRequest.create();
        	locationRequest.setInterval(10000);
        }
    }


    /**
     * Start requesting location updates.
     *
     * @param interval
     */
    @Override
    protected void start() {
        if (!this.running) {
            if (this.locationClient != null) {
                this.running = true;
                //if (this.locationClient.isConnected())
                //	locationClient.requestLocationUpdates(locationRequest, this);
            } else {
                this.fail(CordovaLocationListener.POSITION_UNAVAILABLE, "Google Location Service is not available.");
            }
        }
    }


	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onConnected(Bundle arg0) {
		// TODO Auto-generated method stub
		Log.d("", "Google Location Service is connected.");
		locationClient.requestLocationUpdates(locationRequest, this);
	}


	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}
}
