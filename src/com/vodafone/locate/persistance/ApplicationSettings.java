/*******************************************************************************
 * Copyright (c) 1999-2010, Vodafone Group Services
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright 
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above 
 *       copyright notice, this list of conditions and the following 
 *       disclaimer in the documentation and/or other materials provided 
 *       with the distribution.
 *     * Neither the name of Vodafone Group Services nor the names of its 
 *       contributors may be used to endorse or promote products derived 
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING 
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY 
 * OF SUCH DAMAGE.
 ******************************************************************************/
package com.vodafone.locate.persistance;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.vodafone.locate.MapsApplication;
import com.vodafone.locate.R;
import com.wayfinder.core.shared.Position;
import com.wayfinder.core.shared.settings.GeneralSettings;

public class ApplicationSettings {

    private static final ApplicationSettings INSTANCE = new ApplicationSettings();
	private static final String SHARED_PREFS_NAME               = "VodafoneMapsSettings";
    private static final String KEY_MEASUREMENTS                = "key_measurements";
    private static final String KEY_SAVED_ADDRESS                = "key_saved_address";
    private static final String KEY_SAVED_ADDRESS_LATITUDE                = "key_saved_address_latitude";
    private static final String KEY_SAVED_ADDRESS_LONGITUDE                = "key_saved_address_longitude";

    private static final String KEY_SERVER_URL                  = "key_server_url";
    private static final String KEY_SERVER_PORT                 = "key_server_port";
    private static final String KEY_CLIENT_TYPE                 = "key_client_type";
        
    
    private static final String URL_VERSION_BASE = "[ENTER ADDRESS]"; 
    private static final String URL_VERSION_APK = ".apk";
    private static final String URL_VERSION_CHECK = ".version";
    
	private String serverUrl;
	private int serverPort;
	private String clientId;

	private MapsApplication mapsApplication;
    private OnSettingsChangeListener listener;

	private ApplicationSettings() {
	}

	public static final ApplicationSettings get() {
		return INSTANCE;
	}

	public void initBeforeCore(MapsApplication app) {
		this.mapsApplication = app;
		SharedPreferences prefs = openPrefs();
        this.setServerAddress(prefs.getString(KEY_SERVER_URL, MapsApplication.getServerUrl()), prefs.getInt(KEY_SERVER_PORT, MapsApplication.getServerPort()));
        this.setClientId(prefs.getString(KEY_CLIENT_TYPE, MapsApplication.getClientId()));
	}

	public void initAfterCore(MapsApplication app) {
		//Loading settings from phone and setting measurement unit 
		GeneralSettings coreSettings = this.mapsApplication.getCore().getGeneralSettings();
        SharedPreferences prefs = openPrefs();
        this.setMeasurementSystem(prefs.getInt(KEY_MEASUREMENTS, coreSettings.getMeasurementSystem()));
        this.mapsApplication.setSavedAddress(prefs.getString(KEY_SAVED_ADDRESS, null));
        int lat = prefs.getInt(KEY_SAVED_ADDRESS_LATITUDE, 0);
        int lon = prefs.getInt(KEY_SAVED_ADDRESS_LONGITUDE, 0);
        if (lat != 0 && lon != 0) {
            this.mapsApplication.setSavedAddressPosition(new Position(lat, lon));
        }
	}

	public void setServerAddress(String url, int port) {
	    if(this.serverUrl == null || !this.serverUrl.equals(url) || this.serverPort != port) {
    		this.serverUrl = url;
    		this.serverPort = port;
            if(this.listener != null) {
                this.listener.onSettingsChangeListener(this);
            }
	    }
	}

	public String getServerUrl() {
		// if this is not a debug-version then disregard user�s settings and use
		// what is defined in MapsApplication
		if (!MapsApplication.isDebug()) {
			return MapsApplication.getServerUrl();
		}
		return this.serverUrl;
	}

	public int getServerPort() {
		// if this is not a debug-version then disregard user�s settings and use
		// what is defined in NavigatorApplication
		if (!MapsApplication.isDebug()) {
			return MapsApplication.getServerPort();
		}
		return this.serverPort;
	}

	public void setClientId(String clientId) {
	    if(this.clientId == null || !this.clientId.equals(clientId)) {
    	    this.clientId = clientId;
            if(this.listener != null) {
                this.listener.onSettingsChangeListener(this);
            }
	    }
	}

	public String getClientId() {
		if (!MapsApplication.isDebug()) {
			return MapsApplication.getClientId();
		}
		return this.clientId;
	}

    public int getMeasurementSystem() {
        return this.mapsApplication.getCore().getGeneralSettings().getMeasurementSystem();
    }
    
    public void setMeasurementSystem(int measurementsSettings) {
        GeneralSettings coreSettings = this.mapsApplication.getCore().getGeneralSettings();
        
        if(coreSettings.getMeasurementSystem() != measurementsSettings) {
            coreSettings.setMeasurementSystem(measurementsSettings);
            coreSettings.commit();
            this.mapsApplication.updateUnitsFormatter();
            if(this.listener != null) {
                this.listener.onSettingsChangeListener(this);
            }
        }
    }
    
    public void setOnSettingsChangeListener(OnSettingsChangeListener listener) {
        this.listener = listener;
    }
    
    public void removeOnSettingsChangeListener(OnSettingsChangeListener listener) {
        if(this.listener == listener) {
            this.listener = null;
        }
    }
    
    public void commit() {
        SharedPreferences prefs = openPrefs();
        Editor editor = prefs.edit();

        if(this.mapsApplication.isInitialized()) {
            editor.putInt(KEY_MEASUREMENTS, this.getMeasurementSystem());
            if(this.mapsApplication.getSavedAddress() != null){
            	editor.putString(KEY_SAVED_ADDRESS, this.mapsApplication.getSavedAddress());
            }
            if(this.mapsApplication.getSavedAddressPosition() != null){
            	editor.putInt(KEY_SAVED_ADDRESS_LATITUDE, this.mapsApplication.getSavedAddressPosition().getMc2Latitude());
            	editor.putInt(KEY_SAVED_ADDRESS_LONGITUDE, this.mapsApplication.getSavedAddressPosition().getMc2Longitude());
            }
        }
        editor.commit();
    }

    private SharedPreferences openPrefs() {
        SharedPreferences prefs = this.mapsApplication.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs;
    }

    public String getUrlVersionApk() {
		String appname = mapsApplication.getResources().getString(R.string.qtn_andr_368_app_name_txt);
		appname = appname.replace(' ', '_');
		return URL_VERSION_BASE + appname + URL_VERSION_APK;
	}

	public String getUrlVersionCheck() {
		String appname = mapsApplication.getResources().getString(R.string.qtn_andr_368_app_name_txt);
		appname = appname.replace(' ', '_');
		return URL_VERSION_BASE + appname + URL_VERSION_CHECK;
	}

	public interface OnSettingsChangeListener {
        void onSettingsChangeListener(ApplicationSettings settings);
    }
}
