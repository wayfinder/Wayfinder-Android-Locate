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
package com.vodafone.locate.activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.vodafone.locate.MapsApplication;
import com.vodafone.locate.R;
import com.vodafone.locate.dialog.AlertDialog;
import com.vodafone.locate.listeners.DataConnectionStateListener;
import com.vodafone.locate.persistance.ApplicationSettings;
import com.vodafone.locate.util.VersionUtil;
import com.wayfinder.core.shared.RequestID;
import com.wayfinder.core.shared.User;
import com.wayfinder.core.shared.error.CoreError;
import com.wayfinder.core.userdata.UserListener;
import com.wayfinder.core.wfserver.info.ClientUpgradeInfo;
import com.wayfinder.core.wfserver.info.UpgradeCheckListener;

public abstract class AbstractStartupActivity extends AbstractActivity implements UserListener, DataConnectionStateListener, UpgradeCheckListener  {
	
	private boolean isCoreStarted;
	private MapsApplication application;
	private Runnable openFirstPageAction;
	private String newVersionId;
	private Handler handler;
	private boolean startUpDataConnectionTest;
	private ClientUpgradeInfo upgradeInfo;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		application = (MapsApplication) this.getApplication();
		openFirstPageAction = new Runnable(){
			public void run() {
				application.setRoamingIsOk(true);
		    	application.setDataConnectionStateListener(AbstractStartupActivity.this);

		    	Intent intent = new Intent(AbstractStartupActivity.this, EulaActivity.class);
				intent.putExtra(ServiceWindowActivity.KEY_SHOW_START_PAGE, true);
		        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
			}
		};
		handler = new Handler();
	}
	
    @Override
    protected void onResume() {
    	this.checkDialogs();
		super.onResume();
    }

	protected void checkDialogs() {
		application.initWarnings();
    	
    	if(application.isInAirplaneMode() && !application.isWifiEnabled() && !this.isFlightModeOk()){
    		showDialog(DIALOG_IN_FLIGHTMODE);
    		return;
    	}
    	else{
    		removeDialog(DIALOG_IN_FLIGHTMODE);
    	}

    	if(application.isRoaming() && !application.isWifiEnabled()){
    		if(!application.isDataRoamingEnabled()){
    			application.setRoamingIsOk(false);
    			displayDataRoamingIsTurnedOffWarning();
        		return;
    		}
    		else{
    			if(!application.isRoamingOk()){
        			setRoamingPositiveAction(new Runnable() {
    					public void run() {
    						application.setRoamingIsOk(true);
    				    	application.setDataConnectionStateListener(AbstractStartupActivity.this);
    				    	checkDialogs();
    					}
    				});

        			displayRoamingWarning(0);
    	    		return;
    			}
        	}
        }
    	
    	if(!application.isMobileDataEnabled() && !application.isWifiEnabled()) {
        	displayNoInternetConnectionWarning(0);
        	return;
    	}
    	else{
    		removeDialog(DIALOG_NO_INTERNET_CONNECTION);
    	}
    	
    	if(isConnecting() && !application.isWifiEnabled()) {
    		application.setDataConnectionStateListener(this);
    		showConnectingDialog();
    		startUpDataConnectionTest = true;
            return;
    	}
    	else{
    		removeDialog(DIALOG_DATA_CONNECTING_WARNING);
    	}
    	
    	if(!application.isLocationActive() && !this.isNoLocationOk()) {
    		displayGpsWarning();
    		return;
    	}else{
    		removeDialog(DIALOG_GPS_WARNING);
    	}
    	
    	application.setDataConnectionStateListener(this);
    	normalStartup();
	}

	private void showConnectingDialog() {
		//removed untill needed
//		showDialog(DIALOG_DATA_CONNECTING_WARNING);
	}

	private boolean isConnecting() {
			return (application.getDataConnectionState() == TelephonyManager.DATA_DISCONNECTED ||
            application.getDataConnectionState() == TelephonyManager.DATA_CONNECTING );	
	}
    
    private void normalStartup(){
    	if(!this.isCoreStarted){
            this.isCoreStarted = true;
            //The reason for us to create a HandlerThread is that core needs a handler 
            //that is prepared with looper
            //Then, the reason for us to do the init in another thread is that the 
            //onCreate method should return as quickly as possible, so that the 
            //Splash-image can be shown while we init core
            HandlerThread handlerThread = new HandlerThread("SplashActivity-HandlerThread");
            handlerThread.start();
            
            final Handler handler = new Handler(handlerThread.getLooper());
            handler.post(new Runnable() {
                public void run() {
                    if(!application.isInitialized()) {
                        initApplication(application);
                    }
                    else {
                    	application.getCore().getUserDataInterface().obtainUserFromServer(AbstractStartupActivity.this);
                    }
                    handler.getLooper().quit();
                }
            });
    	}
    }
    
    private void initApplication(MapsApplication app) {
        app.initiateCore(null);
        app.getCore().getUserDataInterface().getUser(this);
    }
    
    public void currentUser(User user) {
		if(user == null || !user.isActivated()){
		    this.openFirstPageInServiceWindow();
		} 
		else if(!application.isCheckVersionDone()){
			application.seCheckVersionDone(true);
			runVersionCheck();
		}
	}
    
    private void runVersionCheck(){
        new Thread("AbstractStartupActivity.VersionCheckThread") {
            public void run() {
                checkVersion();
            }
        }.start();
    }

	public void error(RequestID requstId, CoreError errorCode) {
		this.openFirstPageInServiceWindow();
	}
	
	public void openFirstPageInServiceWindow(){
		if(application.isRoaming()){
			if(application.getDataConnectionState() == TelephonyManager.DATA_CONNECTING){
				showConnectingDialog();
				application.setDataConnectionStateListener(this);
				startUpDataConnectionTest = false;
			} 
			else if(!application.isWifiEnabled()) {
				if(!application.isRoamingOk() && !application.isRoamingWarningDisplayed()){
					setRoamingPositiveAction(openFirstPageAction);
					displayRoamingWarning(0);
				} 
				else if(isRoamingWarningDisplayed()){
					setRoamingPositiveAction(openFirstPageAction);
				}
			} 
			else {
				runOnUiThread(openFirstPageAction);
			}
		} 
		else {
			runOnUiThread(openFirstPageAction);
		}
		
	}

	protected Dialog onCreateDialog(int id) {
		AlertDialog dialog;
    	switch (id) {
			case DIALOG_DATA_CONNECTING_WARNING: {
				dialog = new AlertDialog(this);
		        dialog.setMessage(R.string.qtn_andr_368_mobile_data_conn_txt);
		        dialog.setProgressbarVisible(true);
		        dialog.setOnCancelListener(new OnCancelListener() {
		            public void onCancel(DialogInterface dialog) {
		                finish();
		            }
		        });
		        
		        return dialog;
			}
			case DIALOG_NEW_INTERNAL_VERSION_AVAILABLE: {
				dialog = new AlertDialog(this);
				dialog.setTitle(getString(R.string.qtn_andr_368_upgrade_app_2_version_txt, this.newVersionId));
				dialog.setMessage(R.string.qtn_andr_368_new_version_available_txt);
				dialog.setNegativeButton(R.string.qtn_andr_368_cancel_tk, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				dialog.setPositiveButton(R.string.qtn_andr_368_download_tk, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						 try {
	                         Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(ApplicationSettings.get().getUrlVersionApk()));
	                         dialog.dismiss();
	                         startActivity(i);
	                         
	                     } catch (Throwable t) {
							Log.e("AbstractStartupActivity.onCreateDialog",	"Download intent failed "+t);
	                     }
					}
				});
				return dialog;
			}
			case DIALOG_NEW_INTERNAL_VERSION_AVAILABLE_FORCED: {
				dialog = new AlertDialog(this);
				dialog.setTitle(getString(R.string.qtn_andr_368_upgrade_app_2_version_txt, this.newVersionId));
				dialog.setMessage(getString(R.string.qtn_andr_368_u_need_2_dwnload_update_txt, this.newVersionId));
				dialog.setNegativeButton(R.string.qtn_andr_368_exit_txt, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				dialog.setPositiveButton(R.string.qtn_andr_368_download_tk, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						 try {
	                         Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(ApplicationSettings.get().getUrlVersionApk()));
	                         dialog.dismiss();
	                         startActivity(i);
	                     } catch (Throwable t) {
							Log.e("AbstractStartupActivity.onCreateDialog",	"Download intent failed "+t);
	                     }
					}
				});
				dialog.setOnDismissListener(new OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						finish();
						exitApplication();
					}
				});
				dialog.setOnCancelListener(new OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						dialog.dismiss();
					}
				});
				
				return dialog;
			}
			case DIALOG_NEW_EXTERNAL_VERSION_AVAILABLE: {
				dialog = new AlertDialog(this);
				dialog.setTitle(getString(R.string.qtn_andr_368_upgrade_app_2_version_txt, this.newVersionId));
				dialog.setMessage(R.string.qtn_andr_368_new_version_available_txt);
				dialog.setNegativeButton(R.string.qtn_andr_368_cancel_tk, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				dialog.setPositiveButton(R.string.qtn_andr_368_download_tk, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						 try {
	                         String upgradeUri = upgradeInfo.getUpgradeUri();
	                         Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(upgradeUri));
	                         dialog.dismiss();
	                         startActivity(i);
	                         
	                     } catch (Throwable t) {
							Log.e("AbstractStartupActivity.onCreateDialog",	"Download intent failed "+t);
	                     }
					}
				});
				return dialog;
			}
			case DIALOG_NEW_EXTERNAL_VERSION_AVAILABLE_FORCED: {
				dialog = new AlertDialog(this);
				dialog.setTitle(getString(R.string.qtn_andr_368_upgrade_app_2_version_txt, this.newVersionId));
				dialog.setMessage(getString(R.string.qtn_andr_368_u_need_2_dwnload_update_txt, this.newVersionId));
				dialog.setNegativeButton(R.string.qtn_andr_368_cancel_tk, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
				dialog.setPositiveButton(R.string.qtn_andr_368_download_tk, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						 try {
	                         String upgradeUri = upgradeInfo.getUpgradeUri();
	                         Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(upgradeUri));
	                         dialog.dismiss();
	                         startActivity(i);
	                         
	                     } catch (Throwable t) {
							Log.e("AbstractStartupActivity.onCreateDialog",	"Download intent failed "+t);
	                     }
					}
				});
				return dialog;
			}
			default: {
				return super.onCreateDialog(id);
			}
		}
	}
	
	private void showNewVersionDialog(String newVersionID, final boolean isForced, final boolean isInternalRelease) {
		this.newVersionId = newVersionID;
		this.handler.post(new Runnable() {
			public void run() {
				if(isInternalRelease) {
					if(isForced) {
						showDialog(DIALOG_NEW_INTERNAL_VERSION_AVAILABLE_FORCED);
					}
					else {
						showDialog(DIALOG_NEW_INTERNAL_VERSION_AVAILABLE);
					}
				}
				else {
					if(isForced) {
						showDialog(DIALOG_NEW_EXTERNAL_VERSION_AVAILABLE_FORCED);
					}
					else {
						showDialog(DIALOG_NEW_EXTERNAL_VERSION_AVAILABLE);
					}
				}
			}
		});
	}
	
    public void onDataConnectionStateChanged(int state){
    	if(application.getDataConnectionState() == TelephonyManager.DATA_CONNECTED) {
    		application.setDataConnectionStateListener(null);
    		removeDialog(DIALOG_DATA_CONNECTING_WARNING);
    		if (startUpDataConnectionTest) {
    			normalStartup();
    		}
    		startUpDataConnectionTest = false;
    	}
    }

    private void checkVersion() {
    	if(MapsApplication.isInternalRelease()) {
	        URL url = null;
	        BufferedReader in = null;
	        try {
	            url = new URL(ApplicationSettings.get().getUrlVersionCheck());
	            Log.i("AbstractStartupActivity", "checkVersion() checking version at: " + url);
	            in = new BufferedReader(new InputStreamReader(url.openStream()));
	            String serverVersion = in.readLine();
	            String forcedVersion = in.readLine();
	            boolean isForced = (forcedVersion != null && (forcedVersion.startsWith("true") || forcedVersion.startsWith("1")));
	            String clientVersion = application.getVersion();
	            if(VersionUtil.compareVersions(clientVersion, serverVersion) > 0) {
	                showNewVersionDialog(serverVersion, isForced, true);
	            }
	        } catch (IOException e) {
	            Log.e("AbstractStartupActivity", "checkVersion() Version check: " + e);
	            e.printStackTrace();
	        } finally {
	            if(in != null) {
	                try {
	                    in.close();
	                } catch (IOException e) {}
	            }
	        }
    	}
    	else {
    		this.getApp().getCore().getWfServerInterface().clientUpgradeCheck(this);
    	}
    }
    
    public void clientNotFound(RequestID requestID) {
    	Log.e("AbstractStartupActivity", "clientNotFound() No client was found on server");
    }
    
    public void clientUpgrade(RequestID requestID, ClientUpgradeInfo upgradeInfo) {
   		this.upgradeInfo = upgradeInfo;
        showNewVersionDialog(upgradeInfo.getLatestVersion(), upgradeInfo.isForceUpgrade(), false);
    }
    
    public void clientUpToDate(RequestID requestID) {
    	Log.i("AbstractStartupActivity", "clientUpToDate() Client is up to date");
    }
}
