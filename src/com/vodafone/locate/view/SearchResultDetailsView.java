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
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vodafone.locate.MapsApplication;
import com.vodafone.locate.R;
import com.vodafone.locate.util.ImageDownloader;
import com.vodafone.locate.util.ImageDownloader.ImageDownloadListener;
import com.wayfinder.core.search.HierarchicalCategory;
import com.wayfinder.core.search.onelist.MatchDetailsRequestListener;
import com.wayfinder.core.search.onelist.OneListSearchReply.SearchMatch;
import com.wayfinder.core.shared.RequestID;
import com.wayfinder.core.shared.error.CoreError;
import com.wayfinder.core.shared.poidetails.PoiDetail;
import com.wayfinder.core.shared.util.UnitsFormatter.FormattingResult;

public class SearchResultDetailsView extends LinearLayout implements ImageDownloadListener, MatchDetailsRequestListener{
	private HierarchicalCategory category;
	private SearchMatch searchResult;
	private CombinedDetailsView parent;
	private MapsApplication mapsApplication;
	private ImageView detailsImage;
	private String detailsImageName;
	private ImageView providerImage;
	private String providerImageName;
	private TextView title;
	private Button showOnMap;
	private TextView categoryLabel;
	private TextView address;
	private TextView distance;
	private TextView phoneNbr;
	private TextView webLink;
	private TextView emailAddr;
	private TextView descrText;

	public SearchResultDetailsView(final Context context, CombinedDetailsView parent) {
		super(context);
		View.inflate(context, R.layout.search_result_details, this);
		mapsApplication = (MapsApplication) context.getApplicationContext();
		this.parent = parent;
		this.title = (TextView)findViewById(R.id.sr_name);
		this.showOnMap = (Button) findViewById(R.id.toggle_map);
		this.categoryLabel = (TextView)findViewById(R.id.sr_category);		
		this.address = (TextView)findViewById(R.id.sr_address);
		this.distance = (TextView)findViewById(R.id.sr_distance);
		this.providerImage = (ImageView) findViewById(R.id.sr_provider);
		
		this.phoneNbr = (TextView) findViewById(R.id.sr_phone_nbr);
		this.phoneNbr.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + phoneNbr.getText().toString()));
				context.startActivity(Intent.createChooser(intent, null));
			}
		});
		
		this.emailAddr = (TextView) findViewById(R.id.sr_email);
		this.emailAddr.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent sendIntent = new Intent(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {emailAddr.getText().toString()});
				sendIntent.putExtra(Intent.EXTRA_TEXT, "");
				sendIntent.putExtra(Intent.EXTRA_SUBJECT, "");
				sendIntent.setType("message/rfc822");
				context.startActivity(Intent.createChooser(sendIntent, null));
			}
		});
		
		this.webLink = (TextView) findViewById(R.id.sr_webaddress);
		this.webLink.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webLink.getText().toString()));
				context.startActivity(Intent.createChooser(intent, null));
			}
		});
		
		this.descrText = (TextView) findViewById(R.id.description_body);
	}
	
	public void setSearchResult(SearchMatch searchResult, HierarchicalCategory category) {
	    if (searchResult.additionalInfoExists()) {
	        //request details if there are any
	        mapsApplication.getCore().getSearchInterface().getOneListSearch().requestDetails(searchResult, this);
	    }
		this.category = category;
		this.searchResult = searchResult;

		this.title.setText(searchResult.getMatchName());
		
		this.showOnMap.setOnClickListener(new OnClickListener(){
			public void onClick(View view) {
				SearchResultDetailsView.this.parent.onFlip();
			}
		});

		this.categoryLabel.setText(this.category.getCategoryName());
		
		this.distance.setText(formatPrecisionText(searchResult.getPosition().distanceTo(mapsApplication.getLastLocationPosition())));
		
		this.detailsImage = (ImageView) findViewById(R.id.details_image);
		this.detailsImage.setImageResource(R.drawable.cat_all);
        
		this.detailsImageName = category.getCategoryImageName();
		Bitmap image = ImageDownloader.get().queueDownload(this.getContext(), detailsImageName, this);
        if(image != null) {
            this.detailsImage.setImageBitmap(image);
        }
        String brandImageName = searchResult.getMatchBrandImageName();
        if (brandImageName != null && brandImageName.length() > 0) {
            this.detailsImageName = brandImageName;
            Bitmap imageBrand = ImageDownloader.get().queueDownload(this.getContext(), brandImageName, this);
            if(imageBrand != null) {
                this.detailsImage.setImageBitmap(imageBrand);
            }   
        }
        this.providerImageName = searchResult.getMatchProviderImageName();
        if(this.providerImageName != null && this.providerImageName.length() > 0) {
            image = ImageDownloader.get().queueDownload(this.getContext(), providerImageName, this);
            if(image != null) {
                this.providerImage.setVisibility(VISIBLE);
                this.providerImage.setImageBitmap(image);
            }
        } else {
            this.providerImage.setVisibility(GONE);
        }
        LinearLayout phoneLayout = (LinearLayout) findViewById(R.id.phone_layout_simple);
        phoneLayout.setVisibility(GONE);
        
        PoiDetail info = searchResult.getFilteredInfo();
        
        //TODO: make two line address
        if (info.getFullAddress() != null) {
            this.address.setText(tryToFormatAddress(info.getFullAddress().getValue()));
        } else {
            //fallback
            this.address.setText(tryToFormatAddress(searchResult.getMatchLocation()));
        }
        
        if (info.getPhone() != null) {
            phoneLayout.setVisibility(VISIBLE);
            phoneNbr.setText(info.getPhone().getValue());
        }
        LinearLayout emailLayout = (LinearLayout) findViewById(R.id.email_layout_simple);
        emailLayout.setVisibility(GONE);
        if (info.getEmail() != null) {
            emailLayout.setVisibility(VISIBLE);
            emailAddr.setText(info.getEmail().getValue());
        }
        LinearLayout webLayout = (LinearLayout) findViewById(R.id.web_layout_simple);
        webLayout.setVisibility(GONE);
        if (info.getWebsite() != null) {           
            webLayout.setVisibility(VISIBLE);
            webLink.setText(info.getWebsite().getValue());
        }
        
        LinearLayout descrLayout = (LinearLayout) findViewById(R.id.description_layout_simple);
        descrLayout.setVisibility(GONE);
        if (info.getDescription() != null) {            
            TextView label = (TextView) findViewById(R.id.localized_description_label);
            descrLayout.setVisibility(VISIBLE);
            label.setText(info.getDescription().getName());
            descrText.setText(info.getDescription().getValue());
        }
        
        
	}
	
	/* (non-Javadoc)
	 * @see android.view.View#onWindowVisibilityChanged(int)
	 */
	protected void onWindowVisibilityChanged(int visibility) {
	    if (visibility == View.VISIBLE) {
	        TextView distance = (TextView)findViewById(R.id.sr_distance);
	        distance.setText(formatPrecisionText(searchResult.getPosition().distanceTo(mapsApplication.getLastLocationPosition())));
	    }
	    super.onWindowVisibilityChanged(visibility);
	}
	
	private String formatPrecisionText(int distance){
		FormattingResult result = mapsApplication.getUnitsFormatter().formatDistance(distance);
		String distanceText = result.getRoundedValue() + " " + result.getUnitAbbr();
		return distanceText;
	}
	
	 public void onImageDownloaded(final Bitmap origBitmap, final String imageName) {
	        this.post(new Runnable() {
	            public void run() {
	            	if(imageName.equals(detailsImageName)){
	               		detailsImage.setImageBitmap(origBitmap);
	            	}
	            	if(imageName.equals(providerImageName)){
	            		providerImage.setImageBitmap(origBitmap);
	            	}
	            }
	        });
	 }
	 
	 private String tryToFormatAddress(String addr) {
	     return addr.replace(", ", "\n");
	 }
	 
	 @Override
	protected void onAttachedToWindow() {
		LinearLayout nameCategLayout = (LinearLayout)findViewById(R.id.name_categ_layout);
		nameCategLayout.setSelected(true);
		super.onAttachedToWindow();
	}

    /* (non-Javadoc)
     * @see com.wayfinder.core.search.onelist.MatchDetailsRequestListener#matchDetailsUpdated(com.wayfinder.core.shared.RequestID, com.wayfinder.core.search.onelist.OneListSearchReply.SearchMatch)
     */
    public void matchDetailsUpdated(RequestID requestID, SearchMatch searchMatch) {
        setSearchResult(searchMatch, this.category);
    }

    /* (non-Javadoc)
     * @see com.wayfinder.core.shared.ResponseListener#error(com.wayfinder.core.shared.RequestID, com.wayfinder.core.shared.error.CoreError)
     */
    public void error(RequestID arg0, CoreError arg1) {

        
    }
}
