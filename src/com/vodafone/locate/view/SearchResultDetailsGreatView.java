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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.EventLogTags.Description;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageButton;
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
import com.wayfinder.core.shared.poidetails.DetailField;
import com.wayfinder.core.shared.poidetails.PoiDetail;
import com.wayfinder.core.shared.poiinfo.PoiInfo;
import com.wayfinder.core.shared.util.UnitsFormatter.FormattingResult;

public class SearchResultDetailsGreatView extends LinearLayout implements ImageDownloadListener, OnClickListener{
	private HierarchicalCategory category;
	private SearchMatch searchResult;
	private MapFlipInterface parent;
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
	private ImageView[] rating;
	private ImageButton callBtn;
	private ImageButton emailBtn;
	private ImageButton webBtn;
	private LinearLayout openingHoursLayout;
	private LinearLayout picturesLayout;
	private LinearLayout reviewLayout;	
	private TextView hoursTxt;
	private ImageView img1;
	private ImageView img2;
	private ImageView img3;
	private TextView descriptionText; 
	private TextView nbrReviewsText;
	private ArrayList<Bitmap> srImages;
	private Gallery imageGallery;

	public SearchResultDetailsGreatView(Context context, MapFlipInterface parent) {
		super(context);		
		View.inflate(context, R.layout.search_result_details_great, this);
		srImages = new ArrayList<Bitmap>();
		mapsApplication = (MapsApplication) context.getApplicationContext();
		this.parent = parent;
		this.rating = new ImageView[5];
		this.title = (TextView)findViewById(R.id.sr_name);
		this.showOnMap = (Button) findViewById(R.id.toggle_map);
		this.categoryLabel = (TextView)findViewById(R.id.sr_category);		
		this.address = (TextView)findViewById(R.id.sr_address);
		this.distance = (TextView)findViewById(R.id.sr_distance);
		this.providerImage = (ImageView) findViewById(R.id.sr_provider);
		this.callBtn = (ImageButton) findViewById(R.id.btn_call);
		this.emailBtn = (ImageButton) findViewById(R.id.btn_email);
		this.webBtn = (ImageButton) findViewById(R.id.btn_web);
		this.callBtn.setOnClickListener(this);
		this.emailBtn.setOnClickListener(this);
		this.webBtn.setOnClickListener(this);
		this.rating[0] = (ImageView) findViewById(R.id.rating_1);
        this.rating[1] = (ImageView) findViewById(R.id.rating_2);
        this.rating[2] = (ImageView) findViewById(R.id.rating_3);
        this.rating[3] = (ImageView) findViewById(R.id.rating_4);
        this.rating[4] = (ImageView) findViewById(R.id.rating_5);
        this.openingHoursLayout = (LinearLayout) findViewById(R.id.opening_hours);
        this.picturesLayout = (LinearLayout) findViewById(R.id.q_images);
        this.hoursTxt = (TextView) findViewById(R.id.opening_hours_txt);
        this.img1 = (ImageView) findViewById(R.id.q_img1);
        this.img2 = (ImageView) findViewById(R.id.q_img2);
        this.img3 = (ImageView) findViewById(R.id.q_img3);
        this.reviewLayout = (LinearLayout) findViewById(R.id.reviews_layout);
        this.descriptionText = (TextView) findViewById(R.id.description_body);
        this.nbrReviewsText = (TextView) findViewById(R.id.rev_text);
        this.imageGallery = (Gallery) findViewById(R.id.img_galery);
	}
	
	public void setSearchResult(SearchMatch searchResult, HierarchicalCategory category) {
		this.category = category;
		this.searchResult = searchResult;

		this.img1.setVisibility(GONE);
		this.img2.setVisibility(GONE);
		this.img3.setVisibility(GONE);
		this.reviewLayout.setVisibility(GONE);
		this.picturesLayout.setVisibility(GONE);	
		this.openingHoursLayout.setVisibility(GONE);
		LinearLayout actioLayout = (LinearLayout) findViewById(R.id.actions_layout);
        actioLayout.setVisibility(GONE);
        LinearLayout ratingsLayout = (LinearLayout) findViewById(R.id.ratings_layout);
        ratingsLayout.setVisibility(GONE);
        LinearLayout descriptionLayout = (LinearLayout) findViewById(R.id.description_layout);
        descriptionLayout.setVisibility(GONE);
		
        this.title.setText(searchResult.getMatchName());
		this.showOnMap.setOnClickListener(new OnClickListener(){
			public void onClick(View view) {
				SearchResultDetailsGreatView.this.parent.onFlip();
			}
		});
		
		
		this.categoryLabel.setText(this.category.getCategoryName());
		this.address.setText(AddressFormatter.getSearchMatchAddress(searchResult));
		this.distance.setText(formatPrecisionText(searchResult.getPosition().distanceTo(mapsApplication.getLastLocationPosition())));
		
		this.detailsImage = (ImageView) findViewById(R.id.details_image);
		this.detailsImage.setImageResource(R.drawable.cat_all);
		
	    this.detailsImageName = category.getCategoryImageName();
        Bitmap image = ImageDownloader.get().queueDownload(this.getContext(), detailsImageName, this);
        if(image != null) {
            this.detailsImage.setImageBitmap(image);
        }
        this.providerImageName = searchResult.getMatchProviderImageName();
        image = ImageDownloader.get().queueDownload(this.getContext(), providerImageName, this);
        if(image != null) {
            this.providerImage.setImageBitmap(image);
        }
        
        // set actions (phone, email, web)
        PoiDetail searchResultInfo = searchResult.getFilteredInfo();
        DetailField phoneNbr = searchResultInfo.getPhone();
        DetailField webAddr = searchResultInfo.getWebsite();
        DetailField emailAddr = searchResultInfo.getEmail();
        if (phoneNbr != null || webAddr != null || emailAddr != null) {
            actioLayout.setVisibility(VISIBLE);
            setPhoneNumber(phoneNbr);
            setEmailAddr(emailAddr);
            setWebAddr(webAddr);
        }
        
        //set rating
        if (searchResultInfo.getAverageRating() != null) {
            ratingsLayout.setVisibility(VISIBLE);
            int rating = Integer.parseInt(searchResultInfo.getAverageRating().getValue());
            setRating(rating);
        }
        
        //set hours
        if (searchResultInfo.getOpenHours() != null) {
            openingHoursLayout.setVisibility(VISIBLE);
            hoursTxt.setText(searchResultInfo.getOpenHours().getValue());
        }
        
        //set description
        if (searchResultInfo.getDescription() != null) {
            descriptionLayout.setVisibility(VISIBLE);
            descriptionText.setText(searchResultInfo.getDescription().getValue());
        }
        
        //set nbr reviews
        if (searchResultInfo.getReviewGroup() != null) {
            nbrReviewsText.setText(searchResultInfo.getReviewGroup().getNbrOfReviews() + "reviews");
            //XXX: temporary!! needs localisation
        }
        

        
	}
	
	
	
    private void setRating(int rating) {
        for (int i = 0; i < this.rating.length; i++) {
            this.rating[i].setVisibility(VISIBLE);
            if (i < rating - 1) {
                this.rating[i].setImageResource(R.drawable.rating_star_white);
            } else {
                this.rating[i].setImageResource(R.drawable.rating_star_grey);
            }
        }
    }
	
	/**
     * @param webAddr
     */
    private void setWebAddr(DetailField webAddr) {
        webBtn.setVisibility(VISIBLE);
        if (webAddr != null ) {            
            webBtn.setEnabled(true);
        } else {
            webBtn.setEnabled(false);
        }
    }

    /**
     * @param emailAddr
     */
    private void setEmailAddr(DetailField emailAddr) {
        emailBtn.setVisibility(VISIBLE);
        if (emailAddr != null ) {            
            emailBtn.setEnabled(true);
        } else {
            emailBtn.setEnabled(false);
        }
        
    }

    /**
     * @param phoneNbr
     */
    private void setPhoneNumber(DetailField phoneNbr) {
        callBtn.setVisibility(VISIBLE);
        if (phoneNbr != null ) {            
            callBtn.setEnabled(true);
        } else {
            callBtn.setEnabled(false);
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
		String distanceText = result.getRoundedValue() + result.getUnitAbbr();
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
	 
	 private void setImage(String imgName, Bitmap origBitmap) {
         int img = contains(imgName);
         if (img >= 0) {
             if (img == 0) {
                 img1.setImageBitmap(origBitmap);
                 img1.setVisibility(VISIBLE);
                 img2.setVisibility(GONE);
                 img3.setVisibility(GONE);
             } else if (img == 1) {
                 img2.setImageBitmap(origBitmap);                 
                 img1.setVisibility(VISIBLE);
                 img2.setVisibility(VISIBLE);
                 img3.setVisibility(GONE);
             } else if (img == 2) {
                 img3.setImageBitmap(origBitmap);
                 img1.setVisibility(VISIBLE);
                 img2.setVisibility(VISIBLE);
                 img3.setVisibility(VISIBLE);
             }
         }
	 }
	 
	 private int contains(String imgName) {
	     return -1;
	 }
	 
	 @Override
	protected void onAttachedToWindow() {
		LinearLayout nameCategLayout = (LinearLayout)findViewById(R.id.name_categ_layout);
		nameCategLayout.setSelected(true);
		super.onAttachedToWindow();
	}
	 
	@Override
	protected void onDetachedFromWindow() {
    	super.onDetachedFromWindow();
	}
	 
    /**
     * @param review
     */
//    private void populateRatings(View review, int nr_rating) {
//        ImageView[] rating = new ImageView[5];
//        rating[0] = (ImageView) review.findViewById(R.id.rev_rating_1);
//        rating[1] = (ImageView) review.findViewById(R.id.rev_rating_2);
//        rating[2] = (ImageView) review.findViewById(R.id.rev_rating_3);
//        rating[3] = (ImageView) review.findViewById(R.id.rev_rating_4);
//        rating[4] = (ImageView) review.findViewById(R.id.rev_rating_5);
//        for (int i = 0; i < rating.length; i++) {
//            this.rating[i].setVisibility(VISIBLE);
//            if (i < nr_rating - 1) {
//                rating[i].setImageResource(R.drawable.rating_star_yellow);
//            } else {
//                rating[i].setImageResource(R.drawable.rating_star_grey);
//            }
//        }
//    }

    /* (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View view) {
       
        PoiDetail info = searchResult.getFilteredInfo();
        if (view == this.callBtn) {
            if (info.getPhone() != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + info.getPhone().getValue()));
                this.getContext().startActivity(intent);
            }
        } else if (view == this.emailBtn) {
            if (info.getEmail() != null) {
                Intent emailIntent = new Intent(Intent.ACTION_SEND); 
                emailIntent .setType("plain/text"); 
                emailIntent .putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{info.getEmail().getValue()}); 
                this.getContext().startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            }
        } else if (view == this.webBtn) {
            if (info.getWebsite() != null) {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(info.getWebsite().getValue()));
                this.getContext().startActivity(webIntent);
            }
        }
    }
    
    private class ImageAdapter extends ArrayAdapter<Bitmap>{

        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public ImageAdapter(Context context, int textViewResourceId,
                List<Bitmap> objects) {
            super(context, textViewResourceId, objects);
        }
        
        /* (non-Javadoc)
         * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            Context context = getContext();
            View view = convertView;
            if (view == null) {
                ImageView imgView = new ImageView(context);
                imgView.setImageBitmap(srImages.get(position));
                view = imgView;
            } else {
                ((ImageView)view).setImageBitmap(srImages.get(position));
            }
            return view;
        }
        
    }
}
