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

import java.util.Vector;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import com.vodafone.locate.animation.DisplayNextViewAnimationListener;
import com.vodafone.locate.animation.Flip3dAnimation;
import com.wayfinder.core.search.HierarchicalCategory;
import com.wayfinder.core.search.onelist.OneListSearchReply.SearchMatch;

public class CombinedDetailsView extends FrameLayout implements MapFlipInterface {

	private VFMapView vfMapView;
	private SearchResultDetailsView detailsView;
	private int showingView;
	private Flip3dAnimation rotationClock;
	private Flip3dAnimation rotationCounterClock;
	private float centerX;
	private float centerY;
	private boolean initDone = false;
	private VFMapComponentView mapComponent;
	private SearchMatch searchResult;
	private HierarchicalCategory category;

	public CombinedDetailsView(Context context,	CategoryTreeView categoryTreeView, VFMapComponentView mapComponentView) {
		super(context);
		showingView = DETAILS_VIEW;
		this.vfMapView = new VFMapView(context, mapComponentView);
		detailsView = new SearchResultDetailsView(context, this);
		mapComponent = vfMapView.getMapComponent();
		vfMapView.setEnabled(false);
		this.addView(detailsView);
		this.setFocusable(true);
		this.setClickable(true);
		this.vfMapView.setParent(this);
	}
	
	public void setSearchResult(SearchMatch searchResult, HierarchicalCategory category, boolean zoomToBox) {
		this.searchResult = searchResult;
		this.category = category;
		this.detailsView.setSearchResult(searchResult, category);
		this.mapComponent.showSingleLandMark(searchResult, category, zoomToBox);
		this.mapComponent.preLoad();
	}
	
	public Vector<Object> saveInstanceState() {
		Vector<Object> resultVector = new Vector<Object>();
		resultVector.add(this.searchResult);
		resultVector.add(this.category);
		resultVector.add(this.showingView);
		resultVector.add(mapComponent.saveInstanceState());
		
		return resultVector;
	}
	
	@SuppressWarnings("unchecked")
	public void restoreInstanceState(Vector<Object> restoreVector) {
		this.searchResult = (SearchMatch) restoreVector.get(0);
		this.category = (HierarchicalCategory) restoreVector.get(1);
		this.showingView = ((Integer)restoreVector.get(2)).intValue();
		setSearchResult(searchResult, category, false);
		if(showingView == MAP_VIEW){
			this.detailsView.setVisibility(View.GONE);
			this.addView(vfMapView);
			this.vfMapView.setVisibility(View.VISIBLE);
			this.mapComponent.restoreInstanceState((Vector<Object>) restoreVector.get(3));
			vfMapView.getMapComponent().setMapEnabled(true);
		}
		this.vfMapView.setParent(this);
		restoreVector = null;
	}
	
	public void showMyLocation () {
		this.mapComponent.showMyLocation();
	}

	@Override
	protected void onAnimationEnd() {
		initRotation();
		super.onAnimationEnd();
	}
	
	private void rotateGrid(final boolean flipGridIn) {
		initRotation();
		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		if(flipGridIn)
			this.startAnimation(rotationCounterClock);
		else 
			this.startAnimation(rotationClock);
	}



	private void initRotation() {
		if(!initDone){
			initDone = true;
			centerX = this.getWidth() / 2.0f;
			centerY = this.getHeight() / 2.0f;
			rotationClock = new Flip3dAnimation(0, 90, centerX, centerY);
			rotationClock.setDuration(500);
			rotationClock.setFillAfter(true);
			rotationClock.setInterpolator(new AccelerateInterpolator());
			rotationClock.setAnimationListener(new DisplayNextViewAnimationListener(
					this.getHandler(), false, this,
					this.detailsView, this.vfMapView));
			
			
			rotationCounterClock = new Flip3dAnimation(0, -90, centerX, centerY);
			rotationCounterClock.setDuration(500);
			rotationCounterClock.setFillAfter(true);
			rotationCounterClock.setInterpolator(new AccelerateInterpolator());
			rotationCounterClock.setAnimationListener(new DisplayNextViewAnimationListener(
					this.getHandler(), true, this,
					this.detailsView, this.vfMapView));
		}
	}

	public boolean shouldShowPreviousLevel() {
		if (this.showingView == DETAILS_VIEW) {
			return true;
		} else {
			this.onFlip();
			return false;
		}
	}
	
	public boolean isMapVisible() {
		if (this.showingView == MAP_VIEW) {
			return true;
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
     * @see com.vodafone.locate.view.MapFlipInterface#onFlip()
     */
	public void onFlip() {
		if(this.showingView == MAP_VIEW){
			this.showingView = DETAILS_VIEW;
            vfMapView.getMapComponent().setMapEnabled(false);
			this.rotateGrid(false);
		}
		else{
	        ViewGroup oldParent = (ViewGroup) vfMapView.getParent();
	        if(oldParent != null) {
	            oldParent.removeView(this.vfMapView);
	        }
	        this.addView(vfMapView);
	        this.vfMapView.setParent(this);
			this.showingView = MAP_VIEW;
			vfMapView.getMapComponent().setMapEnabled(true);
			this.rotateGrid(true);
		}
		
	}
	
	public VFMapComponentView getMapView() {
	    return this.vfMapView.getMapComponent();
	}

	public void enableZoomButtons() {
		vfMapView.enableZoomButtons();
	}
	
	@Override
	public boolean onTrackballEvent (MotionEvent event) {
		if(this.showingView == MAP_VIEW){
			return getMapView().onTrackballEvent(event);
		} else {
			return detailsView.onTrackballEvent(event);
		}
	}
}
