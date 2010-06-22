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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vodafone.locate.MapsApplication;
import com.vodafone.locate.R;
import com.vodafone.locate.util.AddressFormatter;
import com.vodafone.locate.util.ImageDownloader;
import com.vodafone.locate.util.ImageDownloader.ImageDownloadListener;
import com.wayfinder.core.search.HierarchicalCategory;
import com.wayfinder.core.search.onelist.OneListSearchReply.SearchMatch;
import com.wayfinder.core.shared.Position;
import com.wayfinder.core.shared.poidetails.PoiDetail;
import com.wayfinder.core.shared.util.UnitsFormatter;
import com.wayfinder.core.shared.util.UnitsFormatter.FormattingResult;

public class SearchResultView extends LinearLayout implements ImageDownloadListener {
    
    private SearchMatch searchResult;
    private TextView name;
    private TextView address;
    private ImageView img;
    private ImageView providerImg;
    private TextView distance;
    private String imageName;
    private boolean imageSet;
	private String providerImageName;
//    private TextView category;
    private ImageView[] rating;
    

    /**
     * @param context
     */
    public SearchResultView(Context context, HierarchicalCategory category, SearchMatch searchResult) {
        super(context);
        init(context, category, searchResult);
    }
    
    private void init(Context context, HierarchicalCategory category, SearchMatch searchResult) {
        View.inflate(context, R.layout.search_result_layout, this);
        this.rating = new ImageView[5];
        this.name = (TextView) findViewById(R.id.text_name);
        this.address = (TextView) findViewById(R.id.text_address);
        this.img = (ImageView) findViewById(R.id.category_image);
        this.providerImg = (ImageView) findViewById(R.id.image_provider);
        this.distance = (TextView) findViewById(R.id.text_distance);
        this.rating[0] = (ImageView) findViewById(R.id.ratings_image_1);
        this.rating[1] = (ImageView) findViewById(R.id.ratings_image_2);
        this.rating[2] = (ImageView) findViewById(R.id.ratings_image_3);
        this.rating[3] = (ImageView) findViewById(R.id.ratings_image_4);
        this.rating[4] = (ImageView) findViewById(R.id.ratings_image_5);
        this.setSearchResult(category, searchResult);
    }

    @Override
    public void setPressed(boolean pressed) {
    	super.setPressed(pressed);
    	this.setFocused(pressed);
    }
    
	@Override
	public void setSelected(boolean selected) {
		super.setSelected(selected);
    	this.setFocused(selected);
	}

	private void setFocused(boolean pressed) {
		Resources res = this.getResources();
		if(pressed) {
			int white = res.getColor(R.color.now_0);
			this.name.setTextColor(white);
			this.address.setTextColor(white);
			this.distance.setTextColor(white);
    	}
    	else {
			int black = res.getColor(R.color.color_black);
			int gray = res.getColor(R.color.now_6_85);
			this.name.setTextColor(black);
			this.address.setTextColor(gray);
			this.distance.setTextColor(gray);
    	}
	}

    public void setSearchResult(HierarchicalCategory category, SearchMatch searchResult) {
        this.searchResult = searchResult;
        this.name.setText(AddressFormatter.getSearchMatchName(searchResult));

        PoiDetail info = searchResult.getFilteredInfo();
        if(info.getFullAddress() != null) {
            this.address.setText(info.getFullAddress().getValue());
        } else {
            //fallback
            this.address.setText(AddressFormatter.getSearchMatchAddress(searchResult));
        }
        
        this.img.setImageResource(R.drawable.cat_all);
        
        MapsApplication application = (MapsApplication) this.getContext().getApplicationContext();
        UnitsFormatter unitsFormatter = application.getUnitsFormatter();
        Position searchPosition = application.getLastLocationPosition();
        if(searchPosition != null) {
            int distance = searchResult.getPosition().distanceTo(searchPosition);
            FormattingResult formattingResult = unitsFormatter.formatDistance(distance);
            this.distance.setText(formattingResult.getRoundedValue() +" "+ formattingResult.getUnitAbbr());
        }

        Bitmap catImage = ImageDownloader.get().queueDownload(this.getContext(), category.getCategoryImageName(), this);
        if(catImage != null) {
            this.img.setImageBitmap(catImage);
        }

        this.imageName = searchResult.getMatchBrandImageName();
        if(this.imageName == null || this.imageName.length() == 0) {
	        this.imageName = searchResult.getMatchCategoryImageName();
        }
	        
        Bitmap image = ImageDownloader.get().queueDownload(this.getContext(), this.imageName, this);
        if(image != null) {
            this.img.setImageBitmap(image);
            this.imageSet = true;
        }
        
        this.providerImageName = searchResult.getMatchProviderImageName();
        if(this.providerImageName != null && this.providerImageName.length() > 0) {
        	this.providerImg.setVisibility(View.VISIBLE);
	        Bitmap providerImage = ImageDownloader.get().queueDownload(this.getContext(), this.providerImageName, this);
	        if(providerImage != null) {
	            this.providerImg.setImageBitmap(providerImage);
	        }
        }
        else {
        	this.providerImg.setVisibility(View.GONE);
        }
        
//        this.category.setText(category.getCategoryName());
    }
    
//    private void setRating(int rating) {
//        for (int i = 0; i < this.rating.length; i++) {
//            this.rating[i].setVisibility(VISIBLE);
//            if (i < rating-1) {
//                this.rating[i].setImageResource(R.drawable.rating_star_yellow);
//            } else {
//                this.rating[i].setImageResource(R.drawable.rating_star_grey);
//            }
//        }
//    }
    
    public SearchMatch getSearchResult() {
        return this.searchResult;
    }

    public void onImageDownloaded(final Bitmap origBitmap, final String imageName) {
        this.post(new Runnable() {
            public void run() {
            	if(imageName.equals(SearchResultView.this.providerImageName)){
           		   	SearchResultView.this.providerImg.setImageBitmap(origBitmap);
            	}
            	else if(imageName.equals(SearchResultView.this.imageName)) {
                    SearchResultView.this.img.setImageBitmap(origBitmap);
                    SearchResultView.this.imageSet = true;
                }
                else if(!SearchResultView.this.imageSet) {
                    SearchResultView.this.img.setImageBitmap(origBitmap);
                }
            }
        });
    }
}
