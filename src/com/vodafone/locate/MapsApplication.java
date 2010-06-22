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
package com.vodafone.locate;

import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.vodafone.locate.activity.AbstractActivity;
import com.vodafone.locate.activity.ServicesActivity;
import com.vodafone.locate.components.AndroidCallbackHandler;
import com.vodafone.locate.data.CategoryLevelData;
import com.vodafone.locate.listeners.DataConnectionStateListener;
import com.vodafone.locate.listeners.GeoCodeChangeListener;
import com.vodafone.locate.listeners.WarningListener;
import com.vodafone.locate.persistance.ApplicationSettings;
import com.vodafone.locate.util.AndroidUnitsFormatter;
import com.vodafone.locate.util.ImageDownloader;
import com.vodafone.locate.util.PropertiesManager;
import com.vodafone.locate.view.VFMapComponentView;
import com.wayfinder.core.Core;
import com.wayfinder.core.CoreFactory;
import com.wayfinder.core.ModuleData;
import com.wayfinder.core.ServerData;
import com.wayfinder.core.map.MapStartupListener;
import com.wayfinder.core.network.NetworkError;
import com.wayfinder.core.network.ServerError;
import com.wayfinder.core.positioning.Criteria;
import com.wayfinder.core.positioning.LocationInformation;
import com.wayfinder.core.positioning.LocationInterface;
import com.wayfinder.core.positioning.LocationListener;
import com.wayfinder.core.positioning.LocationProvider;
import com.wayfinder.core.search.SearchError;
import com.wayfinder.core.shared.Position;
import com.wayfinder.core.shared.RequestID;
import com.wayfinder.core.shared.error.CoreError;
import com.wayfinder.core.shared.error.UnexpectedError;
import com.wayfinder.core.shared.geocoding.AddressInfo;
import com.wayfinder.core.shared.settings.GeneralSettings;
import com.wayfinder.core.shared.util.UnitsFormatter;
import com.wayfinder.pal.PAL;
import com.wayfinder.pal.android.AndroidPAL;
import com.wayfinder.pal.android.network.http.HttpConfigurationInterface;

public class MapsApplication extends Application implements LocationListener {

    public static final int DISMISS_TIME_WARNING_DIALOG = 10;
    public static final int NO_LOCATION_TIMEOUT = 15000; //15 seconds
   
    private static final int NETW_ERROR_WAITING_TIME = 10000;
    private static final double MOVING_SPEED_FACTOR = 7.716; 
    private static final double MOVING_SPEED_CUTTOF = 20; //  20 km/h
    private static final int SQUARE_DISTANCE_BETWEEN_MOVES = 20*20; //a square of 20 x 20

    //HEAD-server
    private static String CLIENT_ID;
    private static String SERVER_URL;
    private static int SERVER_PORT;
    private static boolean isDebug = false;
	private static boolean isInternalRelease = true;

	static {
		CLIENT_ID = "wf-android-demo";
		SERVER_URL = "[MY SERVER]";
		SERVER_PORT = 80;
	}

    private Handler handler;
    private Core core;
    private boolean isRoaming;
    private WarningListener warningListener;
    private Timer timer;
    private TimerTask displayNetworkErrorTask;
    private boolean roamingWarningDisplayed = false;
    private boolean warningsInitialized = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telefonyManager;
    private WifiManager wifiManager;
    private boolean shouldRecheckWifi = false;
    private AndroidUnitsFormatter unitsFormatter;
    private DataConnectionStateListener dataConnectionStateListener;
    private int dataConnectionState;
    private Stack<String> iServiceWindowHistory = new Stack<String>();
    private Criteria criteria;
    private LocationInformation lastLocationInformation;
    private LocationProvider lastLocationProvider;
    private AddressInfo lastAddressInfo;
    private LocationInformation lastAddressLocation;
    private String savedAddress;
    private Position savedAddressPosition;
	private GeoCodeChangeListener getCodeChangeListener;
	private Timer noLocationTimer;
	private boolean isCheckVersionDone;
	private AndroidPAL pal;
	private boolean roamingIsOK = false;
	private Vector<CategoryLevelData> categoryLevelDataVector;
	private Object searchMatchSync = new Object();
	private int lastDeliveredCategory;
	private boolean noInternetConnectionDialogShown;
	private VFMapComponentView mapComponentView;
    
    @Override
    public void onCreate() {
        super.onCreate();
        this.handler = new Handler();
        
        PropertiesManager.get().init(this);
        Boolean debugFlag = new Boolean(PropertiesManager.get().get("debug_flag"));
        MapsApplication.isDebug = debugFlag.booleanValue();

        Boolean internalReleaseFlag = new Boolean(PropertiesManager.get().get("internal_release"));
        MapsApplication.isInternalRelease  = internalReleaseFlag.booleanValue();
        startNoLocationTimer();
        
    }
    
    public void startNoLocationTimer() {
    	cancelNoLocationTimer();
    	if (lastLocationInformation == null) {
			noLocationTimer = new Timer();
			TimerTask noLocationTimerTask = new TimerTask() {
				@Override
				public void run() {
					if (warningListener != null) {
						handler.post(new Runnable() {
							public void run() {
								warningListener.displayNoLocationWarning();
								Log.w("MapsApplication.setupNoLocationTimer",
										"Current position is not available");
							}
						});
					}
				}
			};
			noLocationTimer.schedule(noLocationTimerTask, NO_LOCATION_TIMEOUT);
		}
	}
    
    public void cancelNoLocationTimer(){
    	if(noLocationTimer != null){
    		noLocationTimer.cancel();
    	}
    }

	@Override
    public void onTerminate() {
        super.onTerminate();
    }

    public static final boolean isDebug() {
        return MapsApplication.isDebug;
    }
    
    public static final boolean isInternalRelease() {
    	return MapsApplication.isInternalRelease;
    }
    
    public static boolean isEmulator() {
        return android.os.Build.MODEL.contains("sdk");
    }

    public String getVersion() {
        String version = "0.0.0";
        try {
            PackageManager packageManager = this.getPackageManager();
            if(packageManager != null) {
                PackageInfo packageInfo = packageManager.getPackageInfo(this.getPackageName(), 0);
                if(packageInfo != null) {
                    version = packageInfo.versionName;
                }
            }
        } catch (NameNotFoundException e) {}
        return version;
    }
    
    public String getBuild() {
    	String version = "#0";
        try {
            PackageManager packageManager = this.getPackageManager();
            if(packageManager != null) {
                PackageInfo packageInfo = packageManager.getPackageInfo(this.getPackageName(), 0);
                if(packageInfo != null) {
                    version = "#" + packageInfo.versionCode;
                }
            }
        } catch (NameNotFoundException e) {}
        return version;
    }

    public void initiatePreCore() {
        ApplicationSettings.get().initBeforeCore(this);
    }
    
    public void initiateCore(final MapStartupListener mapStartupListener) {
        if(this.core == null) {
            this.startCore();
        }
        
        ApplicationSettings.get().initAfterCore(this);
    }

    public void initiate(MapStartupListener mapStartupListener) {
        if(this.core == null) {
            this.initiatePreCore();
            this.initiateCore(mapStartupListener);
        }
    }
    
    private void startCore() {
        Log.i("MapsApplication", "startCore()");
     
        String version = "0.0.0";
        PackageManager packageManager = this.getPackageManager();
        if(packageManager != null) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(this.getPackageName(), 0);
                if(packageInfo != null) {
                    version = packageInfo.versionName;
                }
            } catch (NameNotFoundException e) {}
        }

        ApplicationSettings settings = ApplicationSettings.get();
        String serverUrl = settings.getServerUrl();
        int serverPort = settings.getServerPort();
        String clientId = settings.getClientId();
        
        final ServerData bootData = new ServerData(clientId, version, new String[] { serverUrl }, new int[] { serverPort });
        
        pal = initPal();
        
        if(isDebug) {
            pal.enableFileLoggingAsynch();
        }
        
        HttpConfigurationInterface httpConfig = pal.getHttpConfiguration();
        ImageDownloader.create(httpConfig);

        AndroidCallbackHandler callbackHandler = new AndroidCallbackHandler(this.handler);
        final ModuleData modData = new ModuleData(pal, bootData, callbackHandler);
        
        this.core = CoreFactory.createFullCore(modData);

        LocationInterface locationInterface = this.core.getLocationInterface();
        locationInterface.initLocationSystem();
        locationInterface.addLocationListener(getCriteria(), this);

        this.pausePositioning(false);
    }

    protected HttpConfigurationInterface getHttpConfig(PAL pal) {
        //in this class its an androidPal. 
        return ((AndroidPAL)pal).getHttpConfiguration();
    }

    protected AndroidPAL initPal() {
        //Non HTC the widget when there?s a route, is transported to routeactivity 
        return AndroidPAL.createAndroidPAL(this);
    }
    
    public Core getCore() {
        if(this.core == null) {
            this.initiate(null);
        }
        return core;
    }
    
    public void pausePositioning(boolean pause) {
        if(this.core != null) {
            LocationInterface loc = this.core.getLocationInterface();
            if(pause) {
                Log.i("NavigationApplication", "pausePositioning() suspending positioning");
//              loc.removeLocationListener(this);
                loc.suspend();
            }
            else {
                Log.i("NavigationApplication", "pausePositioning() resuming positioning");
//              loc.addLocationListener(this.getCriteria(), this);
                loc.resume();
            }
        }
    }
    
    public boolean isInitialized() {
        return (this.core != null);
    }

    public static String getServerUrl() {
        return SERVER_URL;
    }
    
    public static int getServerPort() {
        return SERVER_PORT;
    }
    
    public static String getClientId() {
        return CLIENT_ID;
    }
    
    public String getServerAddress() {
        ApplicationSettings settings = ApplicationSettings.get();
        int port = settings.getServerPort();
        if(port == 80) {
            return "http://" + settings.getServerUrl() + "/";
        }
        return "http://" + settings.getServerUrl() + ":" + settings.getServerPort() + "/";
    }
    
    
    //-------------------------WARNINGS SECTION---------------------------------------
    
    public void initWarnings(){
        if(warningsInitialized){
            return;
        }
        if(isSIMCardAbsent()){
//            warningListener.displayNoSIMWarning();
        } else {
            warningsInitialized = true;
            if(this.phoneStateListener == null) {
                Log.i("Maps Application", "Starting Phone state");
                this.phoneStateListener = this.createPhoneStateListener();
            }        
            
            if(this.telefonyManager == null){
                this.telefonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            }
            this.telefonyManager.listen(this.phoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE 
                                                            | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE 
                                                            | PhoneStateListener.LISTEN_CALL_STATE);
            
            //We need to initiate the value here, since not all phones call the listener when you register to it
            this.dataConnectionState = this.telefonyManager.getDataState();
        }
        
//        boolean gpsFound = this.isGPSActive();
//        if(!gpsFound) {
//            if(this.warningListener != null) {
//                this.warningListener.displayGpsWarning();
//            }
//        }
    }
    
    public boolean isLocationActive() {
        String stringValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        StringTokenizer st = new StringTokenizer(stringValue, ",");
        boolean gpsFound = false;
        while(st.hasMoreTokens()) {
            String provider = st.nextToken();
            if("gps".equals(provider) || "network".equals(provider)) {
                gpsFound = true;
                break;
            }
 
        }
        return gpsFound;
    }
    
    public boolean isWifiEnabled() {
    	
    	return false;
    	/*
        if(this.wifiManager == null){
            this.wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        }
        return wifiManager.isWifiEnabled();*/
    }

    public boolean isRoaming(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return ni.isRoaming();
    }
    
    public boolean isDataRoamingEnabled(){
        String stringValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.DATA_ROAMING);
        int intValue = Integer.parseInt(stringValue);
        return intValue == 1;
    }
    
    public boolean isMobileDataEnabled() {
        ConnectivityManager connec =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connec.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting()) { 
            return true;
        }
        return false;
    }
    
     /**
     * @return the shouldRecheckWifi
     */
    public boolean shouldRecheckWifi() {
        return shouldRecheckWifi;
    }

    /**
     * @param shouldRecheckWifi the shouldRecheckWifi to set
     */
    public void setShouldRecheckWifi(boolean shouldRecheckWifi) {
        this.shouldRecheckWifi = shouldRecheckWifi;
    }
    
    public boolean isSIMCardAbsent(){
        if(this.telefonyManager == null){
             this.telefonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            }
        int simState = this.telefonyManager.getSimState();
        return simState == TelephonyManager.SIM_STATE_ABSENT;
    }
    
    public void setWarningListener(WarningListener listener){
        this.warningListener  = listener;
    }
    
    public WarningListener getWarningListener() {
        return warningListener;
    }
    
    
    
    private PhoneStateListener createPhoneStateListener() {
        return new PhoneStateListener() {

            public void onServiceStateChanged(ServiceState serviceState) {
                isRoaming = serviceState.getRoaming();
                super.onServiceStateChanged(serviceState);
            }
            
            @Override
            public void onDataConnectionStateChanged(int state) {
                dataConnectionState = state;
                
                if(dataConnectionStateListener != null){
                    dataConnectionStateListener.onDataConnectionStateChanged(state);
                }
                
                if(wifiManager == null){
                    wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                }
                
                boolean wifiEnabled = isWifiEnabled();
                int wifiState = wifiManager.getWifiState();
				if(state == TelephonyManager.DATA_DISCONNECTED && warningListener != null && wifiEnabled){
                	if(wifiState == WifiManager.WIFI_STATE_ENABLED || wifiState == WifiManager.WIFI_STATE_ENABLING){
                		dataConnectionState = TelephonyManager.DATA_CONNECTED;
                	}
                	if(displayNetworkErrorTask != null) {
                        displayNetworkErrorTask.cancel();
                        displayNetworkErrorTask = null;
                    }
                	super.onDataConnectionStateChanged(state);
                	return;
                }
                
                if(state == TelephonyManager.DATA_CONNECTING || state == TelephonyManager.DATA_CONNECTED) {
                    if(displayNetworkErrorTask != null) {
                        displayNetworkErrorTask.cancel();
                        displayNetworkErrorTask = null;
                    }
                }
                
                if(isRoaming && !wifiEnabled){
                    if(state == TelephonyManager.DATA_DISCONNECTED && !isDataRoamingEnabled()){
                        if(warningListener != null && AbstractActivity.isApplicationActive()){
                            warningListener.displayDataRoamingIsTurnedOffWarning();
                        }
                    }  else if(state == TelephonyManager.DATA_DISCONNECTED){
                         if(warningListener != null && AbstractActivity.isApplicationActive()){
                             displayNoInternetConnectionDialog();
                         }
                    }  else if(state == TelephonyManager.DATA_CONNECTED) {
                        if(!roamingWarningDisplayed  && warningListener != null && AbstractActivity.isApplicationActive()){
                            warningListener.displayRoamingWarning(0);
                            roamingWarningDisplayed = true;
                        }
                    }
                } else {
                    if(state == TelephonyManager.DATA_DISCONNECTED){
                        if(warningListener != null && AbstractActivity.isApplicationActive()){
                            displayNoInternetConnectionDialog();
                        }
                    }
                }
                super.onDataConnectionStateChanged(state);
            }
            
            private void displayNoInternetConnectionDialog() {
                //When roaming "No internet connection"-dialog is displayed alot when starting the application. 
                //A Timer that displays this only if network has been down more than X seconds has been added
                if(timer == null) {
                    timer = new Timer("MapsApplication-Timer");
                }

                if(displayNetworkErrorTask == null) {
                    displayNetworkErrorTask = new TimerTask() {
                        public void run() {
                            warningListener.displayNoInternetConnectionWarning(0);
                        }
                    };
                    timer.schedule(displayNetworkErrorTask, NETW_ERROR_WAITING_TIME);
                }
            }
        };
    }
    
    
    
    public UnitsFormatter getUnitsFormatter() {
        if(this.unitsFormatter == null) {
            this.updateUnitsFormatter();
        }
        return this.unitsFormatter;
    }
    
    public void updateUnitsFormatter() {
        //bugfix: a bandage on a shotgun wound...
        //When measurementssystem is requested and core is null we hardcode to 
        //use the metrics system. The problem is a greater one; why is core null? 
        //But this will have to do for now //MMART 091016
        int measurementSystem = GeneralSettings.UNITS_METRIC;
        if(this.core != null && this.core.getGeneralSettings() != null) {
            measurementSystem = this.core.getGeneralSettings().getMeasurementSystem();
        }
        this.unitsFormatter = new AndroidUnitsFormatter(measurementSystem, this);
    }

    /**
     * @param dataConnectionStateListener the dataConnectionStateListener to set
     */
    public void setDataConnectionStateListener(DataConnectionStateListener dataConnectionStateListener) {
        this.dataConnectionStateListener = dataConnectionStateListener;
    }
    
    public int getDataConnectionState(){
        return dataConnectionState;
    }

    /**
     * @return the roamingWarningDisplayed
     */
    public boolean isRoamingWarningDisplayed() {
        return roamingWarningDisplayed;
    }

    /**
     * @param roamingWarningDisplayed the roamingWarningDisplayed to set
     */
    public void setRoamingWarningDisplayed(boolean roamingWarningDisplayed) {
        this.roamingWarningDisplayed = roamingWarningDisplayed;
    }

    /**
     * @return the iServiceWindowHistory
     */
    public Stack<String> getServiceWindowHistory() {
        return iServiceWindowHistory;
    }
    
	public Criteria getCriteria() {
		if (this.criteria == null) {
			this.criteria = new Criteria.Builder().accuracy(
					Criteria.ACCURACY_NONE).costAllowed().build();
		}
		return this.criteria;
	}
	
	public void locationUpdate(LocationInformation locationInformation, LocationProvider locationProvider) {
		 cancelNoLocationTimer();
	     lastLocationInformation = locationInformation;
	     lastLocationProvider = locationProvider;
	}

	/**
	 * @return the lastLocationInformation
	 */
	public LocationInformation getLastLocationInformation() {
		return lastLocationInformation;
	}
	
	public Position getLastLocationPosition() {
	    if (lastLocationInformation != null) {
	        return lastLocationInformation.getMC2Position();
	    } else if (savedAddressPosition != null) {
	        return savedAddressPosition;
	    } else {
	        return null;
	    }
	}

	/**
	 * @return the lastLocationProvider
	 */
	public LocationProvider getLastLocationProvider() {
		return lastLocationProvider;
	}

	/**
	 * @return the lastAddressInfo
	 */
	public AddressInfo getLastAddressInfo() {
		return lastAddressInfo;
	}

	/**
	 * @param lastAddressInfo the lastAddressInfo to set
	 */
	public void setLastAddressInfo(AddressInfo lastAddressInfo, LocationInformation lastAddressLocation) {
		this.lastAddressInfo = lastAddressInfo;
		this.lastAddressLocation = lastAddressLocation;
		this.savedAddressPosition = this.lastAddressLocation.getMC2Position();
	}
	
	public boolean shouldUpdateAddressInfo(LocationInformation currentLocation){
		if(lastAddressLocation == null || currentLocation == null){
			return true;
		}
		return userIsActive() && userHasMoved(currentLocation);
	}
	
	private boolean userHasMoved(LocationInformation currentLocation){
		if(lastAddressLocation == null || currentLocation == null){
			return true;
		}
		Position currentPosition = currentLocation.getMC2Position();
		Position lastGeocodingRequestPosition = lastAddressLocation.getMC2Position();
		double distance = currentPosition.distanceTo(lastGeocodingRequestPosition);
		double squaredDistance = distance * distance;
		Log.i("MapsApplication","userHasMoved(): distance = " + squaredDistance);
		float lastSpeed = currentLocation.getSpeed();
		//convert from m/s to  km/h
		lastSpeed = lastSpeed * 3.6f;
		if(lastSpeed < MOVING_SPEED_CUTTOF){
			Log.i("MapsApplication","userHasMoved(): LOW speed = " + squaredDistance);
			if(squaredDistance > SQUARE_DISTANCE_BETWEEN_MOVES){
				return true;
			}
		} else {
			Log.i("MapsApplication","userHasMoved(): HIGH speed = " + lastSpeed);
			if(squaredDistance > MOVING_SPEED_FACTOR * lastSpeed * lastSpeed){
				return true;
			}
		}
		return false;
	}
	
	private boolean userIsActive(){
		return true;
	}

	/**
	 * @return the lastAddressLocation
	 */
	public LocationInformation getLastAddressLocation() {
		return lastAddressLocation;
	}

	public void setSavedAddress(String savedAddress) {
		this.savedAddress = savedAddress;
	}

	public String getSavedAddress() {
		return savedAddress;
	}

	public void setSavedAddressPosition(Position savedAddressPosition) {
		this.savedAddressPosition = savedAddressPosition;
	}

	public Position getSavedAddressPosition() {
		return savedAddressPosition;
	}
	
	   public void error(RequestID requestID, CoreError error) {
	    Log.w("CoreError", error.toString());
		String requestIDString = "null";
		if (requestID != null) {
			requestIDString = "" + requestID.getRequestID();
		}
		int type = error.getErrorType();
		switch (type) {
		case CoreError.ERROR_GENERAL: {
			if (warningListener != null) {
				warningListener
						.displayGeneralErrorMessage(R.string.qtn_andr_368_error_try_again_txt);
			}
			Log.e("MapsApplication", "GeneralError[id:" + requestIDString
					+ "]: " + error.getInternalMsg());
			break;
		}
		case CoreError.ERROR_NETWORK: {
			NetworkError netError = (NetworkError) error;
			if (warningListener != null) {
			    //TODO: better error message needed
			    warningListener.displayNetworkErrorMessage();
			}
			Log.e("MapsApplication", "NetworkError[id:" + requestIDString
					+ "]: " + netError.getInternalMsg());
			break;
		}
		case CoreError.ERROR_SEARCH: {
			SearchError searchError = (SearchError) error;
			if (warningListener != null) {
				// this should be improved in the future
			  //TODO: better error message needed
				warningListener.displayGeneralErrorMessage(R.string.qtn_andr_368_error_try_again_txt);
			}
			Log.e("MapsApplication", "SearchError[id:" + requestIDString
					+ "]: " + searchError.getInternalMsg());
			break;
		}
		case CoreError.ERROR_SERVER: {
			ServerError serverError = (ServerError) error;
			Log.e("MapsApplication", "ServerError[id:" + requestIDString
					+ "]: " + serverError.getInternalMsg());

			final String uri = serverError.getStatusUri();
			if (uri != null && uri.length() > 0) {
				Intent intent = new Intent(MapsApplication.this,
						ServicesActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra(ServicesActivity.KEY_URI, uri);
				startActivity(intent);
			} else {
//				switch (serverError.getStatusCode()) {
//				case RouteError.ERRROUTE_ORIGIN_PROBLEM:
//					if (warningListener != null) {
//						warningListener
//								.displayGeneralErrorMessage(R.string.qtn_andr_route_cant_b_calc_from_ur_pos_txt);
//					}
//					break;
//				case RouteError.ERRROUTE_DESTINATION_PROBLEM:
//					if (warningListener != null) {
//						warningListener
//								.displayGeneralErrorMessage(R.string.qtn_andr_route_cant_b_calc_2_req_pos_txt);
//					}
//					break;
//				case RouteError.ERRROUTE_NOT_FOUND:
//					if (warningListener != null) {
//						warningListener
//								.displayGeneralErrorMessage(R.string.qtn_andr_route_cant_b_calc_2_req_pos_txt);
//					}
//					break;
//				case RouteError.ERRROUTE_TOO_FAR:
//					if (warningListener != null) {
//						warningListener
//								.displayGeneralErrorMessage(R.string.qtn_andr_too_long_route_txt);
//					}
//					break;
//				}
			}
			break;
		}
		case CoreError.ERROR_UNEXPECTED: {
			UnexpectedError unexpectedError = (UnexpectedError) error;
			if (warningListener != null) {
				warningListener
						.displayGeneralErrorMessage(R.string.qtn_andr_368_error_try_again_txt);
			}
			Log.e("MapsApplication", "UnexpectedError[id:"
					+ requestIDString + "]: "
					+ unexpectedError.getInternalMsg());
			break;
		}
		default: {
			if (warningListener != null) {
				warningListener
						.displayGeneralErrorMessage(R.string.qtn_andr_368_error_try_again_txt);
			}
			Log.e("MapsApplication", "Error[id:" + requestIDString + "]: "
					+ error.getInternalMsg());
			break;
		}
		}
	}

	public void setGeoCodeChangeListener(GeoCodeChangeListener listener) {
		getCodeChangeListener = listener;
	}
	
	public GeoCodeChangeListener getGeoCodeChangeListener() {
		return getCodeChangeListener;
	}

	public boolean isCheckVersionDone() {
		return isCheckVersionDone;
	}

	public void seCheckVersionDone(boolean checked) {
		isCheckVersionDone = checked;
	}
	
	public boolean isInAirplaneMode() {
		return pal.getNetworkLayer().getNetworkInfo().isAirplaneMode();
	}

	public void setRoamingIsOk(boolean b) {
		roamingIsOK  = true;
	}

	public boolean isRoamingOk() {
		return roamingIsOK;
	}

	public void addLevel(int type, Vector viewData) {
		synchronized (searchMatchSync) {
			if(this.categoryLevelDataVector == null){
				this.categoryLevelDataVector = new Vector<CategoryLevelData>();
			}
			this.categoryLevelDataVector.add(new CategoryLevelData(type, viewData));
		}
	}
	
	/**
	 * When the test hits end this method returns false and then resets itself to be used again. 
	 * This is because we want to support multiple flips of the phone
	 * 
	 * @return true if there is another element 
	 */
	public boolean hasNextCategoryLevelData() {
		synchronized (searchMatchSync) {
			if(this.categoryLevelDataVector != null && this.categoryLevelDataVector.size() > lastDeliveredCategory){
				return true;
			}
			else {
				lastDeliveredCategory = 0;
				return false;
			}
		}
	}
	
	public CategoryLevelData getNextCategoryLevelData() {
		synchronized (searchMatchSync) {
			if(this.categoryLevelDataVector != null && this.categoryLevelDataVector.size() > lastDeliveredCategory){
				CategoryLevelData categoryLevelData = categoryLevelDataVector.get(lastDeliveredCategory);
				lastDeliveredCategory++;
				return categoryLevelData;
			}
			return null;
		}
	}

	public void clearCategoryLevelData() {
		synchronized (searchMatchSync) {
			if(this.categoryLevelDataVector != null ){
				this.categoryLevelDataVector.clear();
				this.categoryLevelDataVector = null;
				System.gc();
			}
		}
	}

	public boolean isNoInternetConnectionDialogShown() {
		return noInternetConnectionDialogShown;
		
	}

	public void setNoInternetConnectionDialogShown(boolean shown) {
		noInternetConnectionDialogShown = shown;
	}

	public void setMapComponentiew(VFMapComponentView mapView) {
		this.mapComponentView = mapView;
	}

	public VFMapComponentView getMapView() {
		return mapComponentView;
		
	}
	
}
