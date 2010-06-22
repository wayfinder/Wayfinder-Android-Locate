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
package com.vodafone.locate.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vodafone.locate.MapsApplication;
import com.vodafone.locate.R;
import com.vodafone.locate.listeners.GeoCodeChangeListener;
import com.vodafone.locate.persistance.ApplicationSettings;
import com.wayfinder.core.Core;
import com.wayfinder.core.geocoding.GeocodeListener;
import com.wayfinder.core.positioning.LocationInformation;
import com.wayfinder.core.positioning.LocationInterface;
import com.wayfinder.core.positioning.LocationListener;
import com.wayfinder.core.positioning.LocationProvider;
import com.wayfinder.core.shared.RequestID;
import com.wayfinder.core.shared.error.CoreError;
import com.wayfinder.core.shared.geocoding.AddressInfo;

public class GeoCodeView extends LinearLayout implements LocationListener {

    private static final int ACCURACY_2_STREET             = 1;
    private static final int ACCURACY_3_CITY_PART          = 2;
    private static final int ACCURACY_4_CITY               = 3;
    private static final int ACCURACY_BAD                  = 4;
    
	private TextView textViewPosition;
	private TextView textViewPrecision;
	private ImageView plusMinusImage;
	private ProgressBar progress;
	private MapsApplication mapsApplication;
	private boolean windowIsVisible;
	private boolean shouldListenToGPS = true;
	private Animation slideOutAnimation;
	private Animation slideInAnimation;
	
	public GeoCodeView(Context context) {
		super(context);
		init(context);
	}

	public GeoCodeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public void stopListener(){
		shouldListenToGPS = false;
		mapsApplication.getCore().getLocationInterface().removeLocationListener(this);
		progress.setVisibility(View.GONE);
	}
	
	@Override
	protected void onAttachedToWindow() {
		if(shouldListenToGPS){
			Core core = mapsApplication.getCore(); 
			LocationInterface locationInterface = core.getLocationInterface();
			locationInterface.addLocationListener(this.mapsApplication.getCriteria(), this);
		}
		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {
		mapsApplication.getCore().getLocationInterface().removeLocationListener(this);
		super.onDetachedFromWindow();
	}

	private void init(Context context) {
		View.inflate(context, R.layout.include_reversed_geocoding_fields, this);
		textViewPosition = (TextView) findViewById(R.id.textview_position);
		textViewPrecision = (TextView) findViewById(R.id.precision_label);
		plusMinusImage = (ImageView) findViewById(R.id.image_plus_minus);
		progress = (ProgressBar) findViewById(R.id.progressbar);
		if (wasOldAdress) {
			progress.setImage(R.drawable.loc_update_from_old);
		} else {
			progress.setImage(R.drawable.loc_update_continous);
		}
		
		mapsApplication = (MapsApplication) context.getApplicationContext();
		
		this.slideOutAnimation = AnimationUtils.loadAnimation(context, R.anim.push_up_out);
		this.slideInAnimation = AnimationUtils.loadAnimation(context, R.anim.push_up_in);
        
	}

	public void locationUpdate(final LocationInformation location, LocationProvider provider) {
		if (location != null && mapsApplication.shouldUpdateAddressInfo(location)) {
			Log.i("GeoCodeView", "new reversed geocoding requested");
			try {
				mapsApplication.getCore().getGeocodeInterface().reverseGeocode(location.getMC2Position(), new GeocodeListener() {
					public void reverseGeocodeDone(RequestID requestID, final AddressInfo addressInfo) {
						
						
						slideOutAnimation.setAnimationListener(new AnimationListener() {
				            public void onAnimationEnd(Animation animation) {			                
				            	textViewPosition.setText(formatGeocodingText(addressInfo));
				            	textViewPosition.startAnimation(slideInAnimation);
				            }
	
				            public void onAnimationRepeat(Animation animation) {
				            }
	
				            public void onAnimationStart(Animation animation) {
				            }
				        });
						
						String address = formatGeocodingText(addressInfo);
						
						String savedAddress = mapsApplication.getSavedAddress();
						if (savedAddress == null || !savedAddress.equals(address)) {
							textViewPosition.startAnimation(slideOutAnimation);										
						}
						if (wasOldAdress) {
		                    textViewPosition.setTextAppearance(getContext(), R.style.label_text_white_bold);
		                }
						progress.setImage(R.drawable.loc_update_continous);	
						
						textViewPrecision.setVisibility(View.GONE);
						plusMinusImage.setVisibility(View.GONE);
						mapsApplication.setLastAddressInfo(addressInfo, location);
						mapsApplication.setSavedAddress(address);
						ApplicationSettings.get().commit();
						geoCodeHasChanged(addressInfo);
					}
	
	
					public void error(RequestID requestID, CoreError error) {
						if(windowIsVisible){
							Log.e("GeoCodeView.GeocodeListener", "error() "
									+ error.getInternalMsg());
							mapsApplication.error(requestID, error);
						}
					}
				});
			} catch(IllegalArgumentException e) {
				Log.e("GeoCodeView", "locationUpdate() error: " + e);
			}
		}
	}
	
	protected void geoCodeHasChanged(AddressInfo addressInfo) {
		GeoCodeChangeListener geoCodeChangeListener = mapsApplication.getGeoCodeChangeListener();
		if(geoCodeChangeListener != null){
			geoCodeChangeListener.onGeoCodeChange(addressInfo);	
		}
	}
	
	/**
     * Returns an accuracy constant based on what we get from the reverse geocode request
     * @param addressInfo the addres info provided by the listener
     * @return
     */
    private int getAccuracyLevel(AddressInfo addressInfo) {
        if (addressInfo.getStreet() != null && addressInfo.getStreet().length() > 0) {
            return ACCURACY_2_STREET; //aka ACCURACY 2 (we don't have 1) - street level
        } else if (addressInfo.getCityPart() != null && addressInfo.getCityPart().length() > 0) {
            return ACCURACY_3_CITY_PART;      
        } else if (addressInfo.getCity() != null && addressInfo.getCity().length() > 0) {
            return ACCURACY_4_CITY;
        } 
        return ACCURACY_BAD;
    }

    private String formatGeocodingText(AddressInfo addressInfo) {
        StringBuilder result = new StringBuilder();
        int accuracy = getAccuracyLevel(addressInfo);
        if (accuracy == ACCURACY_2_STREET) {
            result.append(addressInfo.getStreet());
            if (isInformationValid(addressInfo.getCityPart())) {
                result.append(", ");
                result.append(addressInfo.getCityPart());
            } else {
                if (isInformationValid(addressInfo.getCity())) {
                    result.append(", ");
                    result.append(addressInfo.getCity());
                }
            }
        } else if (accuracy == ACCURACY_3_CITY_PART) {
            result.append(addressInfo.getCityPart());
            if (isInformationValid(addressInfo.getCity())) {
                result.append(", ");
                result.append(addressInfo.getCity());
            }
        } else if (accuracy == ACCURACY_4_CITY){
            result.append(addressInfo.getCity());
            if (isInformationValid(addressInfo.getCountryOrState())) {
                result.append(", ");
                result.append(addressInfo.getCountryOrState());
            }
        } else {
            //show only country
            result.append(addressInfo.getCountryOrState());
        }
        return result.toString();

    }
    
    private boolean isInformationValid(String aInfo) {
        if (aInfo != null && aInfo.length() == 0) {
            return false;
        }
        if (aInfo == null) {
            return false;
        }
        return true;
    }
    
	
	
    private boolean wasOldAdress = false;
    
	@Override
	protected void onWindowVisibilityChanged(int visibility) {
	    windowIsVisible = (visibility == View.VISIBLE);
		AddressInfo lastAddressInfo = mapsApplication.getLastAddressInfo();
		if(lastAddressInfo != null){
			textViewPosition.setText(formatGeocodingText(lastAddressInfo));
			if(mapsApplication.getLastAddressLocation() != null)
//			textViewPrecision.setText(formatPrecisionText(application.getLastAddressLocation()));
			textViewPrecision.setVisibility(View.GONE);
			plusMinusImage.setVisibility(View.GONE);
		} else {
			if(mapsApplication.getSavedAddress() != null){
			    if (!wasOldAdress) {
			        wasOldAdress = true;
			        textViewPosition.setTextAppearance(getContext(), R.style.label_text_gray_bold);
			        progress.setImage(R.drawable.loc_update_from_old);
			    }
				textViewPosition.setText(mapsApplication.getSavedAddress());
			} else {
			    if (!wasOldAdress) {
                    wasOldAdress = true;
                    textViewPosition.setTextAppearance(getContext(), R.style.label_text_gray_bold);
                    progress.setImage(R.drawable.loc_update_from_old);
                }
				textViewPosition.setText(R.string.qtn_andr_368_updating_txt);
			}
			textViewPrecision.setVisibility(View.GONE);
			plusMinusImage.setVisibility(View.GONE);
			textViewPrecision.setText("");
		}
		super.onWindowVisibilityChanged(visibility);
	}
}
