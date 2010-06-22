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

import java.util.List;
import java.util.Vector;

import android.content.Context;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.vodafone.locate.mapcomponents.OwnPositionOverlay;
import com.vodafone.locate.mapcomponents.VFItemizedOverlay;
import com.vodafone.locate.mapcomponents.VFOverlayItem;
import com.vodafone.locate.util.PropertiesManager;
import com.wayfinder.core.search.HierarchicalCategory;
import com.wayfinder.core.search.onelist.OneListSearchReply.SearchMatch;

public class VFMapComponentView extends MapView{

	private List<Overlay> mapOverlays;
	private VFItemizedOverlay itemizedOverlay;
	private MapController mapController;
	private GeoPoint nextResultToShow;
	private OwnPositionOverlay myPosOverlay;
    private MapFlipInterface parent;
	
	public VFMapComponentView(Context context) {
		super(context, PropertiesManager.get().get("map_cert_key"));
		this.setClickable(false);
		this.setFocusable(false);
		this.setEnabled(false);
		mapOverlays = this.getOverlays();
		mapController = this.getController();
		this.setBuiltInZoomControls(false);

		itemizedOverlay = new VFItemizedOverlay(this);
		
		myPosOverlay = new OwnPositionOverlay(this.getContext(), this, 100);
		mapOverlays.add(myPosOverlay);
	}
	
	public Vector<Object> saveInstanceState() {
		Vector<Object> resultVector = new Vector<Object>();
		resultVector.add(itemizedOverlay.getInflatedPoi());
		resultVector.add(this.getMapCenter());
		resultVector.add(this.getZoomLevel());
		return resultVector;
	}
	
	public void restoreInstanceState(Vector<Object> resultVector) {
		this.itemizedOverlay.setInflatedPoi(((Integer) resultVector.get(0)).intValue());
		this.mapController.setCenter((GeoPoint) resultVector.get(1));
		this.mapController.setZoom(((Integer) resultVector.get(2)).intValue());
	}
	
	public void setParent(MapFlipInterface parent) {
	    this.parent = parent;
	}

	@Override
	protected void onAttachedToWindow() {
		myPosOverlay.enableMyLocation();
		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {
		myPosOverlay.disableMyLocation();
		super.onDetachedFromWindow();
	}
	
	/**
	 * Sets zoom to fit current position and the landmark sent to the method
	 * This will only zoom if there is a position 
	 * 
	 * @param landmark landmark to fit into screen
	 * @param animateToFit if map should move to box, probably true 
	 */
    private void zoomToBox(int lat, int lon, boolean animateToFit) {
    	
    	GeoPoint myPosPoint = this.myPosOverlay.getPositionCoord();
		if(myPosPoint != null){
	    	int deltaLat = lat - myPosPoint.getLatitudeE6();
			int deltaLon = lon - myPosPoint.getLongitudeE6();
			mapController.zoomToSpan((int)Math.abs((float)deltaLat * 1.4f), (int)Math.abs((float)deltaLon*1.4f));
			if(animateToFit){
				int moveLat = lat - (deltaLat >> 1);
				int moveLon = lon - (deltaLon >> 1);
				mapController.animateTo(new GeoPoint(moveLat, moveLon));
				if(parent != null){
					parent.enableZoomButtons();
				}
			}
		}
	}
    

	/**
     * Adds a searchResult to the map.
     * 
     * @param landmark The searchResult to add
     * @param category 
     * @param animateToFit calls zoomToBox with coords from landmark and animatToFit=true
     */
	public void addLandmarkToShow(SearchMatch searchMatch, HierarchicalCategory category, boolean animateToFit) {
		int lat = (int)(searchMatch.getPosition().getDecimalLatitude()*1000000);
		int lon = (int)(searchMatch.getPosition().getDecimalLongitude()*1000000);
		nextResultToShow = new GeoPoint(lat,lon);
		VFOverlayItem overlayitem = new VFOverlayItem(nextResultToShow, searchMatch, category, this);
        itemizedOverlay.addOverlay(overlayitem);
        mapOverlays.add(itemizedOverlay);
        if(animateToFit){
        	zoomToBox(nextResultToShow.getLatitudeE6(), nextResultToShow.getLongitudeE6(), true);
        }
	}
	
	public void showMyLocation () {		
		GeoPoint myGeoLocation = myPosOverlay.getPositionCoord();
		mapController.animateTo(myGeoLocation);
	}

	public void showSingleLandMark(SearchMatch searchMatch, HierarchicalCategory category, boolean animateToFit) {
		this.mapOverlays.clear();
		this.mapOverlays.add(myPosOverlay);
		this.itemizedOverlay.clear();
		addLandmarkToShow(searchMatch, category, animateToFit);
	}
    
    public void setMapEnabled(boolean enabled){
        if(enabled){
            this.setClickable(true);
            this.setFocusable(true);
            this.setEnabled(true);
        }
        else {
            this.setClickable(false);
            this.setFocusable(false);
            this.setEnabled(false);
        }
    }

	@Override
	protected void onAnimationEnd() {
		if(this.isShown()){
			this.setClickable(true);
			this.setFocusable(true);
			this.setEnabled(true);
			zoomToBox(nextResultToShow.getLatitudeE6(), nextResultToShow.getLongitudeE6(), true);
		}
		super.onAnimationEnd();
	}

	public void showDetails() {
		this.parent.onFlip();
	}

	public boolean zoom(boolean zoomIn) {
		if(zoomIn){
			return getController().zoomIn();
		}
		else{
			return getController().zoomOut();
		}
	}
}
