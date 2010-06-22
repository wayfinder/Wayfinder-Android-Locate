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

import com.vodafone.locate.R;
import com.vodafone.locate.util.ImageDownloader;
import com.vodafone.locate.util.ImageDownloader.ImageDownloadListener;
import com.wayfinder.core.search.HierarchicalCategory;

public class SingleCategoryView extends LinearLayout implements ImageDownloadListener /*ResourceResponseListener*/ {

    private TextView titleView;
    private ImageView imageView;
    private HierarchicalCategory categoty;
    private boolean isAllCategory;
	private String categoryImageName;

    public SingleCategoryView(Context context, HierarchicalCategory category, String title, boolean isAllCategory) {
        super(context);
        View.inflate(context, R.layout.single_category_layout, this);
        
        this.titleView = (TextView) this.findViewById(R.id.title);
        this.imageView = (ImageView) this.findViewById(R.id.image);
        
        this.setCategory(category, title, isAllCategory);
    }

    public void setCategory(HierarchicalCategory category, String title, boolean isAllCategory) {
        this.categoty = category;
        this.titleView.setText(title);
        this.isAllCategory = isAllCategory;

//        MapsApplication mapsApplication = (MapsApplication) this.getContext().getApplicationContext();
//		  String serverResBasePath = mapsApplication.getServerAddress();
//        String resName = "TMap/B;" + category.getCategoryImageName() + ".png";
//        ResourceRequest request = ResourceRequest.createResourceRequest(ResourceRequest.RESOURCE_TYPE_IMAGE, serverResBasePath, resName, this);
//        mapsApplication.getCore().getCachedResourceManager().requestResource(request);
        
        this.imageView.setImageResource(R.drawable.cat_all);
        
        this.categoryImageName = category.getCategoryImageName();
		Bitmap image = ImageDownloader.get().queueDownload(this.getContext(), categoryImageName, this);
        if(image != null) {
            this.imageView.setImageBitmap(image);
        }
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
		Resources res = getResources();
		if(pressed) {
			int white = res.getColor(R.color.now_0);
			this.titleView.setTextColor(white);
    	}
    	else {
			int black = res.getColor(R.color.color_black);
			this.titleView.setTextColor(black);
    	}
	}

    public HierarchicalCategory getCategoty() {
        return this.categoty;
    }
    
    public boolean isAllCategory() {
        return this.isAllCategory;
    }
    
//    public void resourceAvailable(RequestID reqID, ResourceRequest originalRequest, byte[] resourceData) {
//        Bitmap bitmap = BitmapFactory.decodeByteArray(resourceData, 0, resourceData.length);
//        this.imageView.setImageBitmap(bitmap);
//    }
//
//    public void error(RequestID requestID, CoreError error) {
//        Log.i("SingleCategoryView", "error() " + error.getInternalMsg());
//    }

    public void onImageDownloaded(final Bitmap origBitmap, final String imageName) {
        this.post(new Runnable() {
            public void run() {
            	if(imageName.equals(SingleCategoryView.this.categoryImageName)){
               		imageView.setImageBitmap(origBitmap);
            	}
            }
        });
    }
}
