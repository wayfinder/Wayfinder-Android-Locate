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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.MapActivity;
import com.vodafone.locate.MapsApplication;
import com.vodafone.locate.R;
import com.vodafone.locate.dialog.AlertDialog;
import com.vodafone.locate.listeners.WarningListener;

public abstract class AbstractActivity extends MapActivity implements WarningListener{

    protected static final String KEY_EXIT = "key_exit";

	protected static final int DIALOG_GPS_WARNING = 0;
	protected static final int DIALOG_NO_SIM_WARNING = DIALOG_GPS_WARNING + 1;
	protected static final int DIALOG_NO_INTERNET_CONNECTION = DIALOG_NO_SIM_WARNING + 1;
	protected static final int DIALOG_WIFI_WARNING = DIALOG_NO_INTERNET_CONNECTION + 1;
	protected static final int DIALOG_ROAMING_WARNING = DIALOG_WIFI_WARNING +1;
	protected static final int DIALOG_ROAMING_IS_TURNED_OFF_WARNING = DIALOG_ROAMING_WARNING + 1;
	protected static final int DIALOG_DATA_CONNECTING_WARNING = DIALOG_ROAMING_IS_TURNED_OFF_WARNING + 1;
	protected static final int DIALOG_GENERAL_NETWORK_ERROR = DIALOG_DATA_CONNECTING_WARNING + 1;
    protected static final int DIALOG_NEW_INTERNAL_VERSION_AVAILABLE = DIALOG_GENERAL_NETWORK_ERROR + 1;
    protected static final int DIALOG_NEW_INTERNAL_VERSION_AVAILABLE_FORCED = DIALOG_NEW_INTERNAL_VERSION_AVAILABLE + 1;
    protected static final int DIALOG_NEW_EXTERNAL_VERSION_AVAILABLE = DIALOG_NEW_INTERNAL_VERSION_AVAILABLE_FORCED + 1;
    protected static final int DIALOG_NEW_EXTERNAL_VERSION_AVAILABLE_FORCED = DIALOG_NEW_EXTERNAL_VERSION_AVAILABLE + 1;
    protected static final int DIALOG_IN_FLIGHTMODE = DIALOG_NEW_EXTERNAL_VERSION_AVAILABLE_FORCED + 1;
    protected static final int DIALOG_NEXT_AVAILABLE_ID = DIALOG_IN_FLIGHTMODE + 1;

	private static final String LOGTAG = "AbstractActivity";
	private static int createdActivities = 0;
	private static int activeActivities = 0;
	
	private MapsApplication application;
	private Runnable roamingPositiveAction;
	private String generalErrorMessage;
	private int autoDismissTime;
	private boolean gpsWarningDisplayed;
	private boolean isFlightModeOk;
	private boolean isNoLocationOk;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		application = (MapsApplication) this.getApplication();

        Intent intent = this.getIntent();
        final boolean exit = intent.getBooleanExtra(KEY_EXIT, false);
        if(exit) {
            this.finish();
            System.exit(0);  
            return;
        }
	}

	@Override
	protected void onPause() {
		super.onPause();
		activeActivities--;
		if (activeActivities <= 0) {
			// None of the WayfinderActivities are interacting with the user
			// right now.
			// Beware, this could mean that we have a dialog on top of the
			// WayfinderActivity.
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		createdActivities--;
		if (createdActivities <= 0) {
			createdActivities = 0;
			// No activities are visible on the screen. Be aware that this
			// method may never be called from the system
			Log.i("AbstractActivity", "onStop() Last activity stopped[" + this
					+ "]");
			this.application.pausePositioning(true);
			this.application.cancelNoLocationTimer();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		createdActivities++;
		if (createdActivities == 1) {
			// This is the first activity started.
			Log.i(LOGTAG, "onStart() starting positioning");
			this.application.pausePositioning(false);
			this.application.startNoLocationTimer();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		activeActivities++;
		application.setWarningListener(this);
	}
	
	public boolean isFlightModeOk() {
		return this.isFlightModeOk;
	}
	
	public boolean isNoLocationOk() {
		return this.isNoLocationOk;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_ROAMING_WARNING: {
				AlertDialog dialog = new AlertDialog(this);
				dialog.setTitle(R.string.qtn_andr_368_roaming_txt);
				dialog.setMessage(R.string.qtn_andr_368_roaming_warning_messq_txt);
				dialog.setPositiveButton(R.string.qtn_andr_368_ok_tk,
						new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								if(roamingPositiveAction != null){
									runOnUiThread(roamingPositiveAction);
									roamingPositiveAction = null;
								}
							}
						});
				dialog.setNegativeButton(R.string.qtn_andr_368_exit_txt,
						new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								application.setRoamingWarningDisplayed(false);
								roamingPositiveAction = null;
								exitApplication();
							}
						});
				
				dialog.setOnCancelListener(new OnCancelListener(){
					public void onCancel(DialogInterface arg0) {
						AbstractActivity.this.finish();
					}
					
				});
				dialog.setOnDismissListener(new OnDismissListener(){
					public void onDismiss(DialogInterface arg0) {
						roamingPositiveAction = null;
						application.setRoamingWarningDisplayed(false);
					}
					
				});
				application.setRoamingWarningDisplayed(true);
				return dialog;
			}
			case DIALOG_ROAMING_IS_TURNED_OFF_WARNING: {
				AlertDialog dialog = new AlertDialog(this);
				dialog.setTitle(R.string.qtn_andr_368_you_r_roaming_txt);
				dialog.setMessage(R.string.qtn_andr_368_turn_data_roaming_onq_txt);			
	            dialog.setPositiveButton(R.string.qtn_andr_368_ok_tk,
	                    new OnClickListener() {
	                        public void onClick(DialogInterface dialog, int which) {
	                            AbstractActivity.this
	                                    .dismissDialog(DIALOG_ROAMING_IS_TURNED_OFF_WARNING);
	                            try {
	                                Intent i = new Intent(Settings.ACTION_SETTINGS);
	                                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	                                dialog.dismiss();
	                                startActivity(i);
	                            } catch (Throwable t) {
	                                Log.e("AbstractActivity", "onDismiss() " + t);
	                            }
	                        }
	                    });   
	            
	            dialog.setNegativeButton(R.string.qtn_andr_368_exit_txt,
	            		 new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	dialog.dismiss();
                        exitApplication();
                    }
                });
	            
	            dialog.setOnDismissListener(new OnDismissListener() {
	                public void onDismiss(DialogInterface dialog) {
	                	application.setRoamingWarningDisplayed(false);
	                    if (shouldBeCloseOnError()) {
	                        AbstractActivity.this.finish();
	                    }
	                }
	            });
				return dialog;
			}
			case DIALOG_GPS_WARNING: {
				AlertDialog dialog = new AlertDialog(this);
				dialog.setTitle(R.string.qtn_andr_368_cant_find_yr_loc_txt);
				dialog.setMessage(R.string.qtn_andr_368_enable_loc_source_ch_settq_txt);
				dialog.setPositiveButton(R.string.qtn_andr_368_ok_tk,
						new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								try {
									Intent i = new Intent(
											Settings.ACTION_LOCATION_SOURCE_SETTINGS);
									i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									dialog.dismiss();
									startActivity(i);
								} catch (Throwable t) {
									Log.e("AbstractActivity", "onDismiss() " + t);
								}
							}
						});
				dialog.setNegativeButton(R.string.qtn_andr_368_cancel_tk,
						new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								isNoLocationOk = true;
								dialog.dismiss();
								checkDialogs();
							}
						});
				dialog.setOnDismissListener(new OnDismissListener() {
					
					public void onDismiss(DialogInterface dialog) {
						gpsWarningDisplayed = false;					
					}
				});
				gpsWarningDisplayed = true;
				return dialog;
			}
	
			case DIALOG_NO_SIM_WARNING: {
				AlertDialog dialog = new AlertDialog(this);
				dialog.setTitle(R.string.qtn_andr_368_note_title);
				dialog.setMessage(R.string.qtn_andr_368_insert_sim_txt);
				dialog.setNeutralButton(R.string.qtn_andr_368_ok_tk,
						new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
				dialog.setOnDismissListener(new OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						AbstractActivity.this.finish();
					}
				});
				return dialog;
			}
	
			case DIALOG_NO_INTERNET_CONNECTION: {
				AlertDialog dialog = new AlertDialog(this);
				dialog.setTitle(R.string.qtn_andr_368_note_title);
				dialog.setMessage(R.string.qtn_andr_368_conn_sett_txt);
				dialog.setPositiveButton(R.string.qtn_andr_368_ok_tk,
					new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						AbstractActivity.this.dismissDialog(DIALOG_NO_INTERNET_CONNECTION);
						// Works for bringing up the settings
						try {
							Intent i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
							i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							dialog.dismiss();
							startActivity(i);
						} catch (Throwable t) {
							Log.e("AbstractActivity", "onDismiss() " + t);
						}
					}
				});
				dialog.setNegativeButton(R.string.qtn_andr_368_cancel_tk,
						new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				dialog.setOnDismissListener(new OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						application.setNoInternetConnectionDialogShown(false);
					}
				});
				return dialog;
			}
	
			case DIALOG_WIFI_WARNING: {
				AlertDialog dialog = new AlertDialog(this);
				dialog.setTitle(R.string.qtn_andr_368_note_title);
				dialog.setMessage(R.string.qtn_andr_368_turn_off_wifi_txt);
				dialog.setNeutralButton(R.string.qtn_andr_368_settings_tk,
						new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								Intent i = new Intent(
										Settings.ACTION_WIRELESS_SETTINGS);
								i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(i);
								dialog.dismiss();
							}
						});
				dialog.setPositiveButton(R.string.qtn_andr_368_close_tk,
						new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								finish();
							}
						});
				return dialog;
			}
			
			case DIALOG_GENERAL_NETWORK_ERROR: {
		        AlertDialog dialog = new AlertDialog(this);
	            dialog.setTitle(R.string.qtn_andr_368_note_title);
	            dialog.setMessage(generalErrorMessage);
	            dialog.setNeutralButton(R.string.qtn_andr_368_ok_tk,
	                    new OnClickListener() {
	                public void onClick(DialogInterface dialog, int which) {
	                    AbstractActivity.this.dismissDialog(DIALOG_GENERAL_NETWORK_ERROR);
	                }
	            });
	            dialog.setOnDismissListener(new OnDismissListener(){
	                public void onDismiss(DialogInterface dialog) {
	                    if(shouldBeCloseOnError()){
	                        AbstractActivity.this.finish();
	                    }
	                }
	            });
	            return dialog;
			}
			
			case DIALOG_IN_FLIGHTMODE: {
				AlertDialog dialog = new AlertDialog(this);
				dialog.setTitle(R.string.qtn_andr_368_flight_mode_txt);
				dialog.setMessage(R.string.qtn_andr_368_flight_mo_ch_settq_txt);
				dialog.setPositiveButton(R.string.qtn_andr_368_ok_tk,
						new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						AbstractActivity.this
								.dismissDialog(DIALOG_IN_FLIGHTMODE);
						// Works for bringing up the settings
						try {
							Intent i = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
							i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							dialog.dismiss();
							startActivity(i);
						} catch (Throwable t) {
							Log.e("AbstractActivity", "onDismiss() " + t);
						}
					}
				});
				dialog.setNegativeButton(R.string.qtn_andr_368_cancel_tk,
					new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							isFlightModeOk = true;
							dialog.dismiss();
							checkDialogs();
					}
				});
				return dialog;
			
			}
			default: {
				return super.onCreateDialog(id);
			}
		}
	}

	/**
	 * Overridden in AbstractStartupActivity for checking status of Device before letting the user into the application 
	 */
	protected void checkDialogs() {
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		 case DIALOG_ROAMING_WARNING: {
             AlertDialog alert = (AlertDialog) dialog;
             alert.setAutoDismissTime(this.autoDismissTime);
             break;
         }
		case DIALOG_NO_INTERNET_CONNECTION: {
			AlertDialog alert = (AlertDialog) dialog;
			alert.setAutoDismissTime(this.autoDismissTime);
			break;
		}
		default: {
			super.onPrepareDialog(id, dialog);
		}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.vodafone.android.navigator.listeners.WarningListener#displayNoSIMWarning
	 * ()
	 */
//	public void displayNoSIMWarning() {
//		showDialog(DIALOG_NO_SIM_WARNING);
//	}

	public boolean shouldBeCloseOnError() {
		return false;
	}
	
    public static boolean isApplicationActive() {
        Log.i("AbstractActivity", "created activities: " + createdActivities);
        return createdActivities > 0;
    }

    public static boolean isApplicationVisible() {
        Log.i("AbstractActivity", "active activities: " + activeActivities);
        return activeActivities > 0;
    }
    
    protected void setAutoDismissTime(int autoDismissTime) {
    	this.autoDismissTime = autoDismissTime;
	}
    
	public void displayGeneralErrorMessage(int messageId) {
		//TODO: removed temporarily
//		displayGeneralErrorDialog(messageId, null);
	}

	public void displayGpsWarning() {
		showDialog(DIALOG_GPS_WARNING);
	}

	public void displayNoInternetConnectionWarning(int autoDismissTime) {
		setAutoDismissTime(autoDismissTime);
		if (isApplicationActive()) {
			if(!application.isRoamingWarningDisplayed() && !application.isNoInternetConnectionDialogShown()){
				application.setNoInternetConnectionDialogShown(true);
				showDialog(DIALOG_NO_INTERNET_CONNECTION);
			}
		} else {
			Toast.makeText(this, R.string.qtn_andr_368_conn_sett_txt, Toast.LENGTH_LONG);
		}
	}

	public void displayDataRoamingIsTurnedOffWarning() {
		if (isApplicationActive()) {
			application.setRoamingWarningDisplayed(true);
			showDialog(DIALOG_ROAMING_IS_TURNED_OFF_WARNING);
		} else {
			Toast.makeText(this, R.string.qtn_andr_368_turn_data_roaming_onq_txt,
					Toast.LENGTH_LONG).show();
		}
	}


	public void displayNoLocationWarning() {
		if (isApplicationActive() && !gpsWarningDisplayed) {
			Toast.makeText(this, R.string.qtn_andr_368_curr_loc_not_avail_txt,
						Toast.LENGTH_LONG).show();
		}
	}
	
	public void displayRoamingWarning(int autoDismissTime) {
		if(!application.isRoamingOk()){
			if (isApplicationActive()) {
				setAutoDismissTime(autoDismissTime);
				application.setRoamingWarningDisplayed(true);
				showDialog(DIALOG_ROAMING_WARNING);
			} else {
				Toast.makeText(this, R.string.qtn_andr_368_roaming_warning_messq_txt,
						Toast.LENGTH_LONG).show();
			}
		}
	}
	/**
	 * Displays a simple dialog wich will contain the message and the error code provided as parameters.
	 * Defaul action on OK is dismissing the dialog.
	 * @param message
	 * @param errorCode
	 */
	public void displayGeneralErrorDialog(int resourceID, String errorCode) {
        String errorText = application.getApplicationContext().getResources().getString(resourceID);
        displayGeneralErrorDialog(errorText, errorCode);
        
    }
	
	/**
     * Displays a simple dialog wich will contain the message and the error code provided as parameters.
     * Defaul action on OK is dismissing the dialog.
     * @param message
     * @param errorCode
     */
    public void displayGeneralErrorDialog(String message, String errorCode) {
        String errorText = message;
        if (errorCode != null && errorCode.length() > 0) {
            errorText+= "(" + errorCode +")"; 
        }
        generalErrorMessage = errorText;
        if(isApplicationActive()) {
            showDialog(DIALOG_GENERAL_NETWORK_ERROR);
        } else {
            Toast.makeText(this, generalErrorMessage, Toast.LENGTH_LONG);
        }
    }

    public void displayNetworkErrorMessage() {
        if(isApplicationActive()) {
        	displayNoInternetConnectionWarning(0);
        }
    }

        
    
	public void displaySafetyWarning() {
		
	}

	public void displaySynchronizationCompletedMessage() {
		
	}

	public void displayWifiWarning() {
//		showDialog(DIALOG_WIFI_WARNING);
	}

	public void handleSoundMuteStateChanged() {
		
	}   
	
	protected MapsApplication getApp() {
		return application;
	}
	
	public boolean exitApplication() {
	    Intent intent = new Intent(this, LocateActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    intent.putExtra(LocateActivity.KEY_EXIT, true);
        this.startActivity(intent);
        finish();
		return true;
	}

	/**
	 * @param roamingPositiveAction the roamingPositiveAction to set
	 */
	protected void setRoamingPositiveAction(Runnable roamingPositiveAction) {
		this.roamingPositiveAction = roamingPositiveAction;
	}

	/**
	 * @return the roamingWarningDisplayed
	 */
	public boolean isRoamingWarningDisplayed() {
		return application.isRoamingWarningDisplayed();
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
}
