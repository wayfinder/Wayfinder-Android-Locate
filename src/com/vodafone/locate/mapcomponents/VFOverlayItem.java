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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.vodafone.locate.R;
import com.vodafone.locate.data.Landmark;
import com.vodafone.locate.util.AddressFormatter;
import com.vodafone.locate.util.ImageDownloader;
import com.vodafone.locate.util.ImageDownloader.ImageDownloadListener;
import com.vodafone.locate.view.VFMapComponentView;
import com.wayfinder.core.search.HierarchicalCategory;
import com.wayfinder.core.search.onelist.OneListSearchReply.SearchMatch;

public class VFOverlayItem extends OverlayItem implements ImageDownloadListener {

	private static final int paddingX = 10;
	private static final int shadowWidth = 16;
	private static final int pointerOffsetY = 11;
	
	private static Paint topBoxPaint;
	private static Paint subBoxPaint;
	private static Paint subTextPaint;
	private static Paint imagePaint;
	private static TextPaint topTextPaint;
	
	private BitmapDrawable image;
	private VFMapComponentView mapView;
	private boolean isInflated;
	private Point point;
	private Rect topTextBoxRect;
	private int imageWidth;
	private int imageHeight;
	private String itemName;
	private String itemDesc;
	private Rect subTextBoxRect;
	private String itemNameToDraw;
	private String itemDescToDraw;
	
	private static Resources rec;
	private static Drawable backgroundInflated;
	private static Drawable background;
	private static Drawable arrowMore;
	private int boxWidth;
	private int halfImageInPixels;
	private boolean isTextCalculated = false;
	private int textMargin;
	private int arrowWidth;
	private int arrowHeight;
	private SearchMatch searchMatch;
	
	public VFOverlayItem(GeoPoint geoPoint, SearchMatch searchMatch, HierarchicalCategory category, VFMapComponentView mapView) {
		super(geoPoint, "", "");
		this.searchMatch = searchMatch;
		this.mapView = mapView;
		
		if(rec== null) {
			rec= mapView.getContext().getResources();
		}
		
		if(backgroundInflated == null){
			backgroundInflated = mapView.getContext().getResources().getDrawable(R.drawable.bg_poi_single_selected);		 
		}
		
		if(background == null){
			background = mapView.getContext().getResources().getDrawable((R.drawable.bg_poi_single_closed));
			
		}
		
		if (arrowMore == null) {
			arrowMore = mapView.getResources().getDrawable(R.drawable.arrow_more);
		}
		
		
		if(imagePaint == null){
			imagePaint = new Paint();
			imagePaint.setAntiAlias(true);
		}
		int textColor = mapView.getContext().getResources().getColor(R.color.now_0);
	
		if(topTextPaint == null){
			topTextPaint = new TextPaint();
			topTextPaint.setTextSize(17);
			topTextPaint.setFakeBoldText(true);
			topTextPaint.setAntiAlias(true);
			topTextPaint.setColor(textColor);
		}
		
		if(subTextPaint == null){
			subTextPaint = new TextPaint();
			subTextPaint.setTextSize(15);
			subTextPaint.setAntiAlias(true);
			subTextPaint.setColor(textColor);
		}
		
		if(topBoxPaint == null){
			topBoxPaint = new Paint();
			topBoxPaint .setColor(mapView.getContext().getResources().getColor(R.color.color_blue_dark_semitransparent));
			topBoxPaint .setStyle(Style.FILL);
		}
		
		if(subBoxPaint == null){
			subBoxPaint = new Paint();
			subBoxPaint.setColor(mapView.getContext().getResources().getColor(R.color.color_blue_light_semitransparent));
			subBoxPaint.setStyle(Style.FILL);
		}
		
		this.image = getImage();

		this.point = mapView.getProjection().toPixels(getPoint(), null);
		
		this.topTextBoxRect = new Rect();
		this.subTextBoxRect = new Rect();
		
		
		this.itemName = AddressFormatter.getSearchMatchName(searchMatch);
		this.itemDesc = AddressFormatter.getSearchMatchAddress(searchMatch);
		if(this.itemDesc == null){
			this.itemDesc = category.getCategoryName();
		}
	}
	
	public BitmapDrawable getImage() {
		if(this.image == null){
			String imageName = searchMatch.getMatchBrandImageName();
			if(imageName == null || imageName.length() == 0){
				imageName = searchMatch.getMatchCategoryImageName();
			}
			this.image = new BitmapDrawable(ImageDownloader.get().queueDownload(this.mapView.getContext(), imageName , this));
		}
		return this.image;
	}
	
	public SearchMatch getSearchMatch() {
		return searchMatch;
	}

	public void onImageDownloaded(Bitmap origBitmap, String imageName) {
		this.image = new BitmapDrawable(origBitmap);
		this.mapView.invalidate();
	}

	public void draw(Canvas canvas, MapView mapView2, boolean shadow, long when) {
		float density = rec.getDisplayMetrics().density;
		
		if(this.imageHeight <= 0 ){
			//hardcoded image size untill we get the right assest from server. 
			this.imageHeight = (int)(50 * density);
			this.imageWidth = (int)(50 * density);
			this.halfImageInPixels = this.imageHeight >> 1;
			topTextPaint.setTextSize((int)(18 * density));
			subTextPaint.setTextSize((int)(14 * density));
			
			if(this.itemName != null){
				topTextPaint.getTextBounds(this.itemName, 0, this.itemName.length(), this.topTextBoxRect);
			}
			if(this.itemDesc != null){
				subTextPaint.getTextBounds(this.itemDesc, 0, this.itemDesc.length(), this.subTextBoxRect);
			}
		}
		
		if(!isTextCalculated && this.isInflated){
			isTextCalculated = true;
			//Get the maximum allowed width of the text to use on the map
			int layoutPadding = (int)(30 * density);
			this.arrowWidth = arrowMore.getMinimumWidth();
			this.arrowHeight = arrowMore.getMinimumHeight();
			int maxWidth = this.mapView.getWidth()-this.imageWidth-2*VFOverlayItem.paddingX - layoutPadding - arrowWidth;
			int breakTextIndexTop = 1000;
			int breakTextIndexSub = 1000;
			
			int maxLength = Math.max(this.topTextBoxRect.width(), this.subTextBoxRect.width());
			if(maxLength > maxWidth){
				breakTextIndexTop = topTextPaint.breakText(this.itemName, true, maxWidth, null);
				if(this.itemDesc != null){
					breakTextIndexSub = subTextPaint.breakText(this.itemDesc, true, maxWidth, null);
				}
			}
			
			if(breakTextIndexTop < this.itemName.length()-1){ //-1 becouse we don't what to remove one char to set .. 
				this.itemNameToDraw = this.itemName.substring(0, breakTextIndexTop)+"..";
				this.boxWidth = this.mapView.getWidth()-this.imageWidth - layoutPadding + arrowWidth; 
			}else {
				this.itemNameToDraw = this.itemName;
				this.boxWidth = this.topTextBoxRect.width() + arrowWidth;
			}
			
			if(this.itemDesc != null){
				if(breakTextIndexSub < this.itemDesc.length()-1){ //-1 becouse we don't what to remove one char to set ..
					this.itemDescToDraw = this.itemDesc.substring(0, breakTextIndexSub)+"..";
					if(this.boxWidth < this.subTextBoxRect.width()+ arrowWidth){
						this.boxWidth =  this.subTextBoxRect.width() + arrowWidth;
					}
				}else {
					this.itemDescToDraw = this.itemDesc;
					if(this.boxWidth < this.subTextBoxRect.width() + arrowWidth){
						this.boxWidth =  this.subTextBoxRect.width() + arrowWidth;
					}
				}
			}
			this.textMargin = (int)(5.0 * density);
			
		}
		
		int arrowWidth = this.arrowWidth;
		int arrowHeight = this.arrowHeight;
		this.point = this.mapView.getProjection().toPixels(getPoint(), null);
		Point point = this.point;
		int pointerOffsetY = (int)((float)VFOverlayItem.pointerOffsetY * density);
		int shadowWidth = (int)((float)VFOverlayItem.shadowWidth * density);
		int textMargin = this.textMargin;
		int halfImageInPixels = this.halfImageInPixels;
		if(shadow){
			
			int imageHeight = this.imageHeight;
			if(this.isInflated){
				backgroundInflated.setBounds(point.x - imageHeight, point.y - imageHeight - pointerOffsetY, point.x + this.boxWidth + shadowWidth  + 2*textMargin + arrowWidth, point.y);
				backgroundInflated.draw(canvas);
				Rect bounds = backgroundInflated.getBounds();
				arrowMore.setBounds(bounds.right - shadowWidth - textMargin - arrowWidth, 	//left
									bounds.top + topTextBoxRect.height() - arrowHeight + (int)(6.0 * density),//top
									bounds.right - shadowWidth - textMargin,				//right
									bounds.top + topTextBoxRect.height() + (int)(6.0 * density));  	//bottom
				arrowMore.draw(canvas);
			}
			else{//Not inflated
				background.setBounds(point.x - halfImageInPixels, point.y - imageHeight - pointerOffsetY , point.x + halfImageInPixels  + shadowWidth, point.y);
				background.draw(canvas);
			}
		}
		else{
			if(this.isInflated){
				this.image.setBounds(point.x - this.imageHeight, point.y - this.imageHeight - pointerOffsetY, point.x, point.y - pointerOffsetY);
				this.image.draw(canvas);
				
				if(this.itemNameToDraw != null){
					canvas.drawText(this.itemNameToDraw,  this.image.getBounds().right + textMargin, this.image.getBounds().top + halfImageInPixels - (int)(3.0 * density) , topTextPaint);
				}
				if(this.itemDescToDraw != null){
					canvas.drawText(this.itemDescToDraw,  this.image.getBounds().right + textMargin,  this.image.getBounds().bottom - (int)(8.0 * density) , subTextPaint);
				}				
				
			}
			else{ //Not inflated
				this.image.setBounds(point.x - halfImageInPixels, point.y - this.imageHeight - pointerOffsetY , point.x + halfImageInPixels, point.y - pointerOffsetY);
				this.image.draw(canvas);
			}
			
		}
		
	}

	public void setInflated(boolean isInflated) {
		this.isInflated = isInflated;
	}

	public boolean isInflated() {
		return this.isInflated;
	}

	public Rect getBounds() {
		if(this.isInflated){
			return new Rect(this.point.x - this.imageHeight, this.point.y - this.imageHeight - VFOverlayItem.pointerOffsetY, this.point.x + this.boxWidth + VFOverlayItem.shadowWidth  + this.textMargin * 3, this.point.y);
		}
		else {
			return new Rect(this.point.x - this.halfImageInPixels, (int) (this.point.y - this.imageHeight - VFOverlayItem.pointerOffsetY) , this.point.x + this.halfImageInPixels  + VFOverlayItem.shadowWidth, this.point.y);
		}
	}
	
	
	
}
