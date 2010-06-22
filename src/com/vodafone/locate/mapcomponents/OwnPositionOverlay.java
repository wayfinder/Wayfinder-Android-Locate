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
package com.vodafone.locate.mapcomponents;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.vodafone.locate.MapsApplication;
import com.vodafone.locate.R;
import com.wayfinder.core.positioning.Criteria;
import com.wayfinder.core.positioning.LocationInformation;
import com.wayfinder.core.positioning.LocationInterface;
import com.wayfinder.core.positioning.LocationListener;
import com.wayfinder.core.positioning.LocationProvider;
import com.wayfinder.core.shared.Position;

public class OwnPositionOverlay extends Overlay implements LocationListener {

	private static int NOT_ANIMATING = 0;
	private static int SHOULD_ANIMATE = 1;
	private static int IS_ANIMATING = 2;
	private static int ANIMATION_DURATION = 1000; 
	
	private int animationState = 0;
	
	private LocationInterface locationInterface;
	private Criteria criteria;
		
	private MapView mapView;
	private int currentAccurancy = 1000;
	private int endAccurancy;
	private GeoPoint geoPoint;
	private Paint circlePaint;
	private Paint accurancyPaint;
	private int locationAccurancy;
	private Point myPosition;
	private long animationStartTime;
	private float accuranceySteps;
	private int changeRadius;
	private Drawable gpsPointer;
	
	public OwnPositionOverlay(Context context, MapView mapView, int startAccurancy) {
		super();
		this.mapView = mapView;
		this.locationInterface = ((MapsApplication)context.getApplicationContext()).getCore().getLocationInterface(); 
		this.gpsPointer = mapView.getResources().getDrawable(R.drawable.pointer);
		
		
		accurancyPaint = new Paint();
		accurancyPaint.setColor(context.getResources().getColor(R.color.now_299_semiTransparent));
		accurancyPaint.setStyle(Style.FILL);
		accurancyPaint.setAntiAlias(true);
				
		circlePaint = new Paint();
		circlePaint.setColor(context.getResources().getColor(R.color.color_blue_dark));
		circlePaint.setStyle(Style.STROKE);
		circlePaint.setStrokeWidth(1);
		circlePaint.setAntiAlias(true);
		
	}

	public synchronized boolean enableMyLocation() {
		locationInterface.addLocationListener(getCriteria(), this);
		mapView.invalidate();
		return true;
	}
	
	public synchronized void disableMyLocation() {
		locationInterface.removeLocationListener(this);
	}
	
    private Criteria getCriteria() {
        if (this.criteria == null) {
            this.criteria = new Criteria.Builder().accuracy(
                    Criteria.ACCURACY_NONE).costAllowed().build();
        }
        return this.criteria;
    }
    
    @Override
    public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
    	if(geoPoint == null){
    		return false;
    	}
    	if(animationState == SHOULD_ANIMATE){
    		animationStartTime = when;
    		animationState = IS_ANIMATING;
    		endAccurancy = locationAccurancy;
    		changeRadius = currentAccurancy - endAccurancy;
    		accuranceySteps = ((float)changeRadius)/ANIMATION_DURATION ;  
    	}
    	if(shadow){
    		int accurancyRad = endAccurancy;
    		if(animationState == IS_ANIMATING){
    			accurancyRad = (int)(endAccurancy + (changeRadius - (accuranceySteps * (when - animationStartTime)))); 
    			if((accurancyRad <= endAccurancy && changeRadius > 0) 
    					|| (accurancyRad >= endAccurancy && changeRadius < 0) 
    					|| changeRadius == 0){
    				animationState = NOT_ANIMATING;
    				currentAccurancy = endAccurancy;
    			}
    		}	
    		
    		accurancyRad = ((int)mapView.getProjection().metersToEquatorPixels(accurancyRad));
    		myPosition = mapView.getProjection().toPixels(geoPoint, null);
			canvas.drawCircle(myPosition.x, myPosition.y, accurancyRad, accurancyPaint);
			canvas.drawCircle(myPosition.x, myPosition.y, accurancyRad, circlePaint);
    	}
    	else {
    		int halfGpsWH = gpsPointer.getMinimumHeight() / 2;
			gpsPointer.setBounds(myPosition.x - halfGpsWH , myPosition.y-halfGpsWH, myPosition.x+halfGpsWH, myPosition.y + halfGpsWH);
			gpsPointer.draw(canvas);
		}
    	
    	if(animationState != NOT_ANIMATING){
    		return true;
    	}
    	else return false;
    }
    
    
    
	public void locationUpdate(LocationInformation location, LocationProvider provider) {
		int myPoslat = (int)(location.getMC2Position().getDecimalLatitude()*1000000);
		int myPoslon = (int)(location.getMC2Position().getDecimalLongitude()*1000000);
		locationAccurancy = location.getAccuracy();
		geoPoint = new GeoPoint(myPoslat, myPoslon);
		animationState = OwnPositionOverlay.SHOULD_ANIMATE;
		mapView.invalidate();
	}

	public GeoPoint getPositionCoord() {
		if(geoPoint == null){
			Position pos  = ((MapsApplication)mapView.getContext().getApplicationContext()).getLastLocationPosition();
			int myPoslat = (int)(pos.getDecimalLatitude()*1000000);
			int myPoslon = (int)(pos.getDecimalLongitude()*1000000);
			geoPoint = new GeoPoint(myPoslat, myPoslon);
		}
		return geoPoint;
	}
	
}
