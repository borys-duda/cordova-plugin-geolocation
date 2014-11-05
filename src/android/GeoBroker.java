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

import java.util.ArrayList;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.cordova.geolocation.XmlParser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;


/*
 * This class is the interface to the Geolocation.  It's bound to the geo object.
 *
 * This class only starts and stops various GeoListeners, which consist of a GPS and a Network Listener
 */

public class GeoBroker extends CordovaPlugin {
	private final String		GPS_PROVIDER 		= "gps";
	private final String		WIFI_PROVIDER 		= "wifi";
	private final String		GLS_PROVIDER		= "gls";
	private final String		CELLULAR_PROVIDER	= "cellular";
	
	// ENTER_OPENCELLID_KEY_HERE
	private final String		OPENCELLID_KEY		 = "030dd8c9-3899-4e98-9b94-0a006ae569ee";
	
	private String				enabledProvider;
	 
    private GPSListener 		gpsListener;
    private NetworkListener 	networkListener;
    private LocationListener	locationListener;
    private LocationManager 	locationManager;
    
    private TelephonyManager 	telephonyManager;
    private GsmCellLocation 	gsmCellLocation;
    
    private String				mncString;
    private String				mccString;
    private String				lacString;
    private String				cellidString;
    
    private CallbackContext 	mCallbackContext;

    
    private void popupAlert(final String message) {
//    	final Context context = this.cordova.getActivity();
//    	
//    	this.cordova.getActivity().runOnUiThread(new Runnable() {
//			
//			@Override
//			public void run() {
//		        new AlertDialog.Builder(context)
//					.setTitle("Alert")
//					.setMessage(message)
//					.setPositiveButton(android.R.string.yes, new OnClickListener() {
//					public void onClick(DialogInterface dialog, int which) { 
//						// continue with delete
//		        	}
//				}).show();				
//			}
//		});
    }
    
    public Context getContext() {
    	return this.cordova.getActivity();
    }
    
    private boolean isLocationListenerEnabled() {
    	int response = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.cordova.getActivity());
        if (response == ConnectionResult.SUCCESS)
        	return true;
        
        return false;
    }
    
    /**
     * Executes the request and returns PluginResult.
     *
     * @param action 		The action to execute.
     * @param args 		JSONArry of arguments for the plugin.
     * @param callbackContext	The callback id used when calling back into JavaScript.
     * @return 			True if the action was valid, or false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	try {
	        if (locationManager == null) {
	            locationManager = (LocationManager) this.cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
	        }
	        
	        boolean enableHighAccuracy = args.getBoolean(0);
	        int 	maximumAge = args.getInt(1);
	        int		timeout = args.getInt(2);
	        String	type = args.getString(3);
	
	        if (type.equals(""))
	        	return false;
	        
	        mCallbackContext 	= callbackContext;
	        enabledProvider 	= "";
	        
	        if (action.equals("getLocation")) {
		        if (type.equals(GPS_PROVIDER)) {
			        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			        	fail(400, "GSP provider is disabled.", callbackContext, true);
			        	return true;
			        }
			        
		    		enabledProvider = LocationManager.GPS_PROVIDER;
		    	
		            if (gpsListener == null)
		                gpsListener = new GPSListener(locationManager, this);
		        	
		            Location last = this.locationManager.getLastKnownLocation(enabledProvider);
		            // Check if we can use lastKnownLocation to get a quick reading and use less battery
		            if (last != null && (System.currentTimeMillis() - last.getTime()) <= maximumAge) {
		            	win(last, callbackContext, true);
		            } else {
		                getCurrentLocation(type, callbackContext, enableHighAccuracy, timeout);
		            }
		        }
		        else if (type.equals(WIFI_PROVIDER)) {
			        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			        	fail(400, "WIFI provider is disabled.", callbackContext, true);
			        	return true;
			        }
			        
		    		enabledProvider = LocationManager.NETWORK_PROVIDER;
		    	
		            if (networkListener == null)
		            	networkListener = new NetworkListener(locationManager, this);
		        	
		            Location last = this.locationManager.getLastKnownLocation(enabledProvider);
		            // Check if we can use lastKnownLocation to get a quick reading and use less battery
		            if (last != null && (System.currentTimeMillis() - last.getTime()) <= maximumAge) {
		            	win(last, callbackContext, true);
		            } else {
		                getCurrentLocation(type, callbackContext, enableHighAccuracy, timeout);
		            }
		        }
		        else if (type.equals(GLS_PROVIDER)) {
		        	if (!isLocationListenerEnabled()) {
			        	fail(400, "Google Location Service is disabled.", callbackContext, true);
			        	return true;
		        	}
			    	
		            if (locationListener == null)
		            	locationListener = new LocationListener(locationManager, this);
		        	
		     		enabledProvider = GLS_PROVIDER;
		    		getCurrentLocation(type, callbackContext, enableHighAccuracy, timeout);
		        }
		        else if (type.equals(CELLULAR_PROVIDER)) {
		        	if (!isCellularLocationEnabled()) {
			        	fail(400, "Cellular Provider is disabled.", callbackContext, true);
			        	return true;
		        	}
			    	
		     		enabledProvider = CELLULAR_PROVIDER;
		    		getCurrentLocation(type, callbackContext, enableHighAccuracy, timeout);
		        }
	        }
	        else if (action.equals("addWatch")) {
	            String id = args.getString(0);
	            boolean highAccuracy = args.getBoolean(1);
	            this.addWatch(id, callbackContext, highAccuracy);
	        }
	        else if (action.equals("clearWatch")) {
	            String id = args.getString(0);
	            this.clearWatch(id);
	        }
	        else {
	            return false;
	        }
    	} catch (Exception ex) {
    		ex.printStackTrace();
    		fail(400, "Unknown error.", callbackContext, true);
    	}
        
        return true;
    }

    private void clearWatch(String id) {
        this.gpsListener.clearWatch(id);
        this.networkListener.clearWatch(id);
    }

    private void getCurrentLocation(String type, CallbackContext callbackContext, boolean enableHighAccuracy, int timeout) {
        if (type.equals(GPS_PROVIDER)) {
            gpsListener.addCallback(callbackContext, timeout);
        } else if (type.equals(WIFI_PROVIDER)) {
            networkListener.addCallback(callbackContext, timeout);
        }
        else if (type.equals(GLS_PROVIDER)) {
        	locationListener.addCallback(callbackContext, timeout);
        }
        else if (type.equals(CELLULAR_PROVIDER)) {
        	getCellularLocation();
        }
    }

    private void addWatch(String timerId, CallbackContext callbackContext, boolean enableHighAccuracy) {
        if (enableHighAccuracy) {
            this.gpsListener.addWatch(timerId, callbackContext);
        } else {
            this.networkListener.addWatch(timerId, callbackContext);
        }
    }

    /**
     * Called when the activity is to be shut down.
     * Stop listener.
     */
    public void onDestroy() {
        if (this.networkListener != null) {
            this.networkListener.destroy();
            this.networkListener = null;
        }
        if (this.gpsListener != null) {
            this.gpsListener.destroy();
            this.gpsListener = null;
        }
    }

    /**
     * Called when the view navigates.
     * Stop the listeners.
     */
    public void onReset() {
        this.onDestroy();
    }

    public JSONObject returnLocationJSON(Location loc) {
        JSONObject o = new JSONObject();

        try {
            o.put("latitude", loc.getLatitude());
            o.put("longitude", loc.getLongitude());
            o.put("altitude", (loc.hasAltitude() ? loc.getAltitude() : null));
            o.put("accuracy", loc.getAccuracy());
            o.put("heading", (loc.hasBearing() ? (loc.hasSpeed() ? loc.getBearing() : null) : null));
            o.put("velocity", loc.getSpeed());
            o.put("timestamp", loc.getTime());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return o;
    }

    public void win(Location loc, CallbackContext callbackContext, boolean keepCallback) {
    	popupAlert("Your location is " + loc.getLatitude() + " : " + loc.getLatitude());
    	PluginResult result = new PluginResult(PluginResult.Status.OK, this.returnLocationJSON(loc));
    	result.setKeepCallback(keepCallback);
        callbackContext.sendPluginResult(result);
    }

    /**
     * Location failed.  Send error back to JavaScript.
     * 
     * @param code			The error code
     * @param msg			The error message
     * @throws JSONException 
     */
    public void fail(int code, String msg, CallbackContext callbackContext, boolean keepCallback) {
        JSONObject obj = new JSONObject();
        String backup = null;
        try {
            obj.put("code", code);
            obj.put("message", msg);
        } catch (JSONException e) {
            obj = null;
            backup = "{'code':" + code + ",'message':'" + msg.replaceAll("'", "\'") + "'}";
        }
        PluginResult result;
        if (obj != null) {
            result = new PluginResult(PluginResult.Status.ERROR, obj);
        } else {
            result = new PluginResult(PluginResult.Status.ERROR, backup);
        }

        result.setKeepCallback(keepCallback);
        callbackContext.sendPluginResult(result);
    }

    public boolean isGlobalListener(CordovaLocationListener listener)
    {
    	if (gpsListener != null && networkListener != null)
    	{
    		return gpsListener.equals(listener) || networkListener.equals(listener);
    	}
    	else
    		return false;
    }
    
    public boolean isLocationListener() {
    	if (locationListener != null)
    		return true;
    	
    	return false;
    }
    
    private boolean isCellularLocationEnabled() {
    	if (telephonyManager == null) {
	    	telephonyManager = (TelephonyManager)this.cordova.getActivity().getSystemService(Context.TELEPHONY_SERVICE);
	    	if (telephonyManager == null)
	    		return false;
    	}
    	
    	try {
	    	if (gsmCellLocation == null) {
		    	gsmCellLocation = (GsmCellLocation)telephonyManager.getCellLocation();
		    	if (gsmCellLocation == null)
		    		return false;
	    	}
    	}
    	catch (Exception ex) {
    		return false;
    	}
    	
    	int simState = telephonyManager.getSimState();
    	if (simState != TelephonyManager.SIM_STATE_READY)
    		return false;
    	
    	return true;
    }
    
    private void getCellularLocation()
    {
    	mncString 	 = getMMCMCCLACCELLID(1);
    	mccString 	 = getMMCMCCLACCELLID(2);
    	lacString 	 = getMMCMCCLACCELLID(3);
    	cellidString = getMMCMCCLACCELLID(4);
    	
//    	mccString = "262";
//    	mncString = "2";
//    	lacString = "434";
//    	cellidString = "9200";
    	
    	sendCellPositon();
    }
    
    private String getMMCMCCLACCELLID(int type)
    {
    	if (telephonyManager == null) {
	    	telephonyManager = (TelephonyManager)this.cordova.getActivity().getSystemService(Context.TELEPHONY_SERVICE);
	    	if (telephonyManager == null)
	    		return "";
    	}
    	
    	if (gsmCellLocation == null) {
	    	gsmCellLocation = (GsmCellLocation)telephonyManager.getCellLocation();
	    	if (gsmCellLocation == null)
	    		return "";
    	}
    	
    	String operator = telephonyManager.getNetworkOperator();
    	String mccString = "";
    	String mmcString = "";
    	
    	if (operator.length() > 3)
    	{
    		mccString = operator.substring(0, 3);
    		mmcString = operator.substring(3);
    	}
    	
    	int lac = 0;
    	int cellid = 0;
    	
    	if (gsmCellLocation != null)
    	{
    		lac = gsmCellLocation.getLac();
    		cellid = gsmCellLocation.getCid();
    	}
    	
    	String returnvalue = String.format("%s/%s/%d/%d", mmcString, mccString, lac, cellid);
    	if (type == 1)
    		return mmcString;
    	if (type == 2)
    		return mccString;
    	if (type == 3)
    		return String.format("%d", lac);
    	if (type == 4)
    		return String.format("%d", cellid);
    	
    	return returnvalue;
    }
    
    private void sendCellPositon()
    {
    	new AsyncSendCellid().execute();
    }
    
    private static String[] cellAttributes = {
    	"lat",
    	"lon",
    	"mcc",
    	"mnc",
    	"cellid",
    	"averageSignalStrength",
    	"range",
    	"samples",
    	"changeable",
    	"radio"
    };
    
    private class AsyncSendCellid extends AsyncTask<String , String , String>
	{
		String response = "";
		
		private String[] parseResponse(String response) {
			XmlParser 	xmlParser = new XmlParser(response);
			Element 	rootElement = xmlParser.getRootElement();
			if (rootElement == null)
				return null;
			
			ArrayList<Element> 	cells = xmlParser.parseDocument(rootElement, "cell");
			Element 			cellElement = cells.get(0);
			String[] 			attrValues = new String[cellAttributes.length + 1];
			
			for (int attrIndex = 0; attrIndex < cellAttributes.length; attrIndex ++) {
				attrValues[attrIndex] = xmlParser.getAttribute(cellElement, cellAttributes[attrIndex]);
			}
			
			return attrValues;
		}
		
		protected String doInBackground(String... params) {
			try {
		        HttpClient 		client = new DefaultHttpClient();  
		        String 			getURL = "http://www.opencellid.org/cell/get?key=" + OPENCELLID_KEY + "&mcc="+mccString+"&mnc="+mncString+"&cellid="+cellidString+"&lac=" + lacString;
		        HttpGet 		get = new HttpGet(getURL);
		        HttpResponse 	responseGet = client.execute(get);  
		        HttpEntity 		resEntityGet = responseGet.getEntity();
		        
		        if (resEntityGet != null) {  
		            response = EntityUtils.toString(resEntityGet);
		            
		            if (response != null)
					{
			            String[] attrValues = parseResponse(response);
						Location loc = new Location("");
						loc.setLatitude(Double.parseDouble(attrValues[0]));
						loc.setLongitude(Double.parseDouble(attrValues[1]));
						
						win(loc, mCallbackContext, true);
					}
		            else {
		            	//popupAlert("Cellular is not available.");
		            	fail(400, "Cellular is not available.", mCallbackContext, true);
		            }
		        }
			} catch (Exception e) {
			    e.printStackTrace();
			    //popupAlert("Cellular network has some errors.");
            	fail(400, "Cellular network has some errors.", mCallbackContext, true);
			}
			
			return null;
		}

		protected void onPostExecute(String result) {
			super.onPostExecute(result);
		}

		protected void onPreExecute() {
			super.onPreExecute();
		}
	}    
}
