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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.vodafone.locate.R;

public class VFMapView extends LinearLayout {

	private VFMapComponentView mapComponent; 
	private MapFlipInterface parent;
	private ImageView zoomInButton;
	private ImageView zoomOutButton;
	
	public VFMapView(Context context, VFMapComponentView mapComponentView) {
		super(context);
		View.inflate(context, R.layout.map_view_layout, this);
		RelativeLayout layout = (RelativeLayout)this.findViewById(R.id.map_top_layout);
		ImageView listButton = (ImageView)this.findViewById(R.id.toggle_map);
		listButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				VFMapView.this.parent.onFlip();
			}
		});
		
		zoomInButton = (ImageView)this.findViewById(R.id.zoom_in);
		zoomInButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				VFMapView.this.zoom(true);
			}
		});
		
		zoomOutButton = (ImageView)this.findViewById(R.id.zoom_out);
		zoomOutButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				VFMapView.this.zoom(false);
			}
		});
		
		mapComponent = mapComponentView;
		if(mapComponent.getParent() != null){
			((ViewGroup) mapComponent.getParent()).removeView(mapComponent);
		}
		layout.addView(mapComponent, 0);
		setFocusable(true);
	}

	protected void zoom(boolean zoomIn) {
		if(!this.mapComponent.zoom(zoomIn)) {
			if(zoomIn){
				zoomOutButton.setEnabled(true);
				zoomInButton.setEnabled(false);
			}
			else{
				zoomOutButton.setEnabled(false);
				zoomInButton.setEnabled(true);
			}
 		}else {
			if(zoomIn){
				zoomOutButton.setEnabled(true);
			}
			else{
				zoomInButton.setEnabled(true);
			}
		}
		
		
	}

	public void setParent(MapFlipInterface parent) {
	    this.parent = parent;
	    this.mapComponent.setParent(parent);
	}
	
	public VFMapComponentView getMapComponent() {
		return mapComponent;
	}

	public void enableZoomButtons() {
		zoomOutButton.setEnabled(true);
		zoomInButton.setEnabled(true);
	}
}
