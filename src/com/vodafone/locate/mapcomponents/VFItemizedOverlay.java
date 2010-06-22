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

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.vodafone.locate.view.VFMapComponentView;

public class VFItemizedOverlay extends Overlay  {

	private ArrayList<VFOverlayItem> overlayItems = new ArrayList<VFOverlayItem>();
	private VFMapComponentView mapView;
	private int inflatedIndex = -1;
	
	public VFItemizedOverlay(VFMapComponentView mapView) {
		super();
		this.mapView = mapView;

	}

	public void addOverlay(VFOverlayItem overlay) {
		overlayItems.add(overlay);
	}

	public void clear() {
		this.overlayItems.clear();
		inflatedIndex = -1;
		mapView.invalidate();
	}

	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow,	long when) {
		int overlaySize = overlayItems.size();
		for (int i = 0; i < overlaySize; i++) {
			overlayItems.get(i).draw(canvas, mapView, shadow, when);
		}
		return false;
	}
	
	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		int overlaySize = overlayItems.size();
		Point point = mapView.getProjection().toPixels(p, null);
		int hitCount = 0;
		int lastHitIndex = -1;
		for (int i = 0; i < overlaySize; i++) {
			Rect bounds = overlayItems.get(i).getBounds();
			if(point.x >= bounds.left && point.x <= bounds.right && point.y >= bounds.top && point.y <= bounds.bottom){
				hitCount++;
				lastHitIndex = i;
			}
		}
		if(hitCount == 1){
			return onItemTap(lastHitIndex);
		}
		else if(hitCount > 1){
			//TODO: implement this when we have multiple results in map at same coords
			return super.onTap(p, mapView);
		}
		
		else{
			if(inflatedIndex >= 0 ){
				overlayItems.get(inflatedIndex).setInflated(false);
				inflatedIndex = -1;
			}
			return super.onTap(p, mapView);
		}
		
	}
	
	
	protected boolean onItemTap(int index) {
		if (index == inflatedIndex) {
			//TODO: when handling may pois we should implement to show new details to keep back behavior. 
			mapView.showDetails();
		}
		else {
			if(inflatedIndex >= 0){
				overlayItems.get(inflatedIndex).setInflated(false);
			}
			overlayItems.get(index).setInflated(true);	
			this.inflatedIndex = index;	
		}
		mapView.invalidate();
		return true;
	}

	public int getInflatedPoi() {
		return this.inflatedIndex;
	}
	
	public void setInflatedPoi(int index) {
		if(this.overlayItems.size() > index && index >= 0){
			this.inflatedIndex = index;
			this.overlayItems.get(index).setInflated(true);
		}		
	}
}
