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

import java.util.NoSuchElementException;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.vodafone.locate.MapsApplication;
import com.vodafone.locate.R;
import com.vodafone.locate.data.CategoryLevelData;
import com.vodafone.locate.listeners.CategoryLevelChangeListener;
import com.vodafone.locate.listeners.GeoCodeChangeListener;
import com.wayfinder.core.Core;
import com.wayfinder.core.positioning.LocationInformation;
import com.wayfinder.core.positioning.LocationListener;
import com.wayfinder.core.positioning.LocationProvider;
import com.wayfinder.core.search.Category;
import com.wayfinder.core.search.CategoryTree;
import com.wayfinder.core.search.CategoryTreeChangeListener;
import com.wayfinder.core.search.CategoryTreeException;
import com.wayfinder.core.search.CategoryTreeIterator;
import com.wayfinder.core.search.CategoryTreeUpdateRequestListener;
import com.wayfinder.core.search.HierarchicalCategory;
import com.wayfinder.core.search.SearchInterface;
import com.wayfinder.core.search.onelist.OneListSearchReply.SearchMatch;
import com.wayfinder.core.shared.Position;
import com.wayfinder.core.shared.RequestID;
import com.wayfinder.core.shared.error.CoreError;
import com.wayfinder.core.shared.geocoding.AddressInfo;

public class CategoryTreeView extends ViewFlipper implements CategoryLevelChangeListener, CategoryTreeChangeListener, GeoCodeChangeListener, LocationListener, CategoryTreeUpdateRequestListener {

	private CategoryTree categoryTree;
    private SearchInterface searchInterface;
	private String topRegionID;
//    private MapsApplication application;
    private int currentTreeLevel = 0;
	private VFMapComponentView mapComponentView;
//	private VFMapView mapView;
    private boolean isFirstStart = true; //hack-ish solution, for HTC Legend

    public CategoryTreeView(Context context) {
		super(context);
		init(context);
	}

	public CategoryTreeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	@Override
	protected Parcelable onSaveInstanceState() {
		MapsApplication mapsApplication = (MapsApplication) getContext().getApplicationContext();
		mapsApplication.clearCategoryLevelData();
		for(int i = 0 ; i < this.getChildCount() ; i++){
			View view = this.getChildAt(i);
			if(view instanceof CategoryLevelView){
				mapsApplication.addLevel(CategoryLevelData.TYPE_CATEGORY_LEVEL, ((CategoryLevelView)view).saveInstanceState());
			}else if(view instanceof SearchResultsView){
				mapsApplication.addLevel(CategoryLevelData.TYPE_SEARCH_RESULTS, ((SearchResultsView)view).saveInstanceState());
			}else if(view instanceof CombinedDetailsView){
				mapsApplication.addLevel(CategoryLevelData.TYPE_COMBINED_DETAILS, ((CombinedDetailsView)view).saveInstanceState());
			}
		}
		return super.onSaveInstanceState();
	}
	
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		MapsApplication mapsApplication = (MapsApplication) getContext().getApplicationContext();
		while(mapsApplication.hasNextCategoryLevelData()){
        	CategoryLevelData nextCategoryLevelData = mapsApplication.getNextCategoryLevelData();
        	if(nextCategoryLevelData.getType() == CategoryLevelData.TYPE_CATEGORY_LEVEL){
        		CategoryLevelView nextCatView = new CategoryLevelView(getContext(), this);
        		nextCatView.restoreInstanceState(nextCategoryLevelData.getObjectVector());
                this.addView(nextCatView);
                this.showNext();
			}
        	else if(nextCategoryLevelData.getType() == CategoryLevelData.TYPE_SEARCH_RESULTS){
                SearchResultsView searchResultsView = new SearchResultsView(getContext(), this);                
                searchResultsView.restoreInstanceState(nextCategoryLevelData.getObjectVector());
                this.addView(searchResultsView);
                this.showNext();
			}
        	else if(nextCategoryLevelData.getType() == CategoryLevelData.TYPE_COMBINED_DETAILS){
        		CombinedDetailsView combinedDetailsView = new CombinedDetailsView(getContext(), this, mapComponentView);
                combinedDetailsView.restoreInstanceState(nextCategoryLevelData.getObjectVector());
                this.addView(combinedDetailsView);
                this.showNext();
			}
	    }
		mapsApplication.clearCategoryLevelData();
		super.onRestoreInstanceState(state);
	}
	
	private void init(Context context) {
		MapsApplication mapsApplication = (MapsApplication) getContext().getApplicationContext();
		mapsApplication = (MapsApplication) context.getApplicationContext();
        mapsApplication.setGeoCodeChangeListener(this);
        
        mapComponentView = mapsApplication.getMapView();
        if(mapComponentView == null){
        	mapComponentView = new VFMapComponentView(context);
        	mapsApplication.setMapComponentiew(mapComponentView);
        }
        
        AddressInfo lastAddressInfo = mapsApplication.getLastAddressInfo();
		if(lastAddressInfo != null) {
        	topRegionID = lastAddressInfo.getTopRegionID();
		}
        Core core = mapsApplication.getCore();
        this.searchInterface = core.getSearchInterface();
        this.searchInterface.addCategoryTreeChangeListener(this);
        this.categoryTree = this.searchInterface.getCurrentCategoryTree();

	        if(this.categoryTree != null) {
	            CategoryTreeIterator iterator;
	            try {
	                iterator = this.categoryTree.getRootLevelCategories();
	                CategoryLevelView catLevel = new CategoryLevelView(context, null, iterator, this);
	                this.addView(catLevel);
	                this.setDisplayedChild(0);
	            } catch (CategoryTreeException e) {
	                Log.e("CategoryTreeView", "init() " + e);
	                e.getCause().printStackTrace();
	            }
	        }
        this.setFocusable(true);
        this.setClickable(true);
   }
	
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		MapsApplication mapsApplication = (MapsApplication) getContext().getApplicationContext();
		mapsApplication.getCore().getLocationInterface().addLocationListener(mapsApplication.getCriteria(), this);
	}

	@Override
	protected void onDetachedFromWindow() {
	    if (!isFirstStart) {
	        //XXX: this here might be a temporary solution
	        // On HTC Legend (1.21.161.1) the application crashes when started
	        // in landscape mode, when super.onDetachedFromWindow() is called,
	        // so we prevent calling the method on first start-up.
	        // However, this seems to be firmware related, re-testing needed!!
	        super.onDetachedFromWindow();
	        isFirstStart = false;
	    }
		((MapsApplication) getContext().getApplicationContext()).getCore().getLocationInterface().removeLocationListener(this);
	}

    public void onCategoryChoose(HierarchicalCategory category, boolean isAllCategory) {
        try {
            int startIndex = this.currentTreeLevel + 1;
            this.removeViews(startIndex, this.getChildCount() - startIndex);

            if(category.nbrSubCategories() > 0 && !isAllCategory) {
                CategoryTreeIterator iterator = this.categoryTree.getSubCategoriesOf(category);
                CategoryLevelView nextCatView = new CategoryLevelView(getContext(), category, iterator, this);
                this.addView(nextCatView);
                this.showNext();
            }
            else {
                Position searchPosition = ((MapsApplication) getContext().getApplicationContext()).getLastLocationPosition();
                if(searchPosition != null) {
                    SearchResultsView searchResultsView = new SearchResultsView(getContext(), category, this);
                    this.addView(searchResultsView);
                    this.showNext();
                }
                else {
                    Toast.makeText(this.getContext(), R.string.qtn_andr_368_2_search_position_needed_txt, Toast.LENGTH_LONG).show();
                }
            }
        } catch (NoSuchElementException e) {
            Log.e("CategoryTreeView", "onGotoCategory() " + e);
            e.printStackTrace();
        } catch (CategoryTreeException e) {
            Log.e("CategoryTreeView", "onGotoCategory() " + e);
            e.getCause().printStackTrace();
        }
	}

    public void onSearchMatchChoose(SearchMatch searchResult, HierarchicalCategory category) {
        int startIndex = this.currentTreeLevel + 1;
        this.removeViews(startIndex, this.getChildCount() - startIndex);
        CombinedDetailsView combinedDetailsView = new CombinedDetailsView(getContext(), this, mapComponentView);
    	combinedDetailsView.setSearchResult(searchResult, category, true); 
		this.addView(combinedDetailsView);
    	this.showNext();
    }
    
    public void onShowOnMap(SearchMatch searchResult) {
//        int startIndex = this.currentTreeLevel + 1;
//        this.removeViews(startIndex, this.getChildCount() - startIndex);
//        vfMapView.getMapComponent().showSingleLandMark(searchResult, true);
    }
	
    public void onCancel() {
        this.showPrevious();
    }
    
	public boolean onKeyBack() {
	    View view = this.getCurrentView();
	    if(view instanceof AbstractResultView) {
            Category cat = ((AbstractResultView) view).getCategory();
            ((AbstractResultView)view).setSearchCanceled(true);
            if(cat != null) {
                this.showPrevious();
                return true;
            }
        }
	    if(view instanceof VFMapView){
	    	this.showPrevious();
	    	return true;
	    }
	    if(view instanceof MapFlipInterface){
	    	if(((MapFlipInterface) view).shouldShowPreviousLevel()){
	    		this.showPrevious();
	    	}
	    	return true;
	    }
	    return false;
	}

	@Override
	public void setDisplayedChild(int whichChild) {
	    int displayedChild = this.getDisplayedChild();
        int dir = (displayedChild == whichChild ? 0 : displayedChild < whichChild ? 1 : -1);
	    this.setupFlipperAnimations(dir);
	    super.setDisplayedChild(whichChild);
	}
	
	@Override
	public void showNext() {
	    this.currentTreeLevel ++;
	    int childCount = this.getChildCount();
        if(this.currentTreeLevel >= childCount) {
	        this.currentTreeLevel = childCount - 1;
	    }
        this.setupFlipperAnimations(1);
	    super.showNext();
	}
	
	@Override
	public void showPrevious() {
	    this.currentTreeLevel --;
	    if(this.currentTreeLevel < 0) {
	        this.currentTreeLevel = 0;
	    }
	    this.setupFlipperAnimations(-1);
	    super.showPrevious();
	    View view = this.getCurrentView();
	    if(view instanceof CategoryLevelView){
	    	((CategoryLevelView)view).setClicked(false);
	    }
	}
	
	@Override
	public boolean onTrackballEvent (MotionEvent event) {
		View view = this.getCurrentView();
		if (view instanceof CombinedDetailsView) {
			return view.onTrackballEvent(event);
		}
		view.requestFocus();
		return view.onTrackballEvent(event);
	}
	
    private void setupFlipperAnimations(final int dir) {
        if(dir != 0) {
            float toXDelta = this.getWidth();
            int duration = 300;
            
            TranslateAnimation inAnimation = new TranslateAnimation(toXDelta * dir, 0, 0, 0);
            inAnimation.setDuration(duration);
            inAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            int displayedChild = getDisplayedChild();
            if(dir < 0 && displayedChild > 0) {
                getChildAt(displayedChild - 1).setEnabled(true);
            }
            this.setInAnimation(inAnimation);
    
            TranslateAnimation outAnimation = new TranslateAnimation(0, -toXDelta * dir, 0, 0);
            outAnimation.setDuration(duration);
            outAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            outAnimation.setAnimationListener(new AnimationListener() {
                public void onAnimationStart(Animation animation) {}
                public void onAnimationRepeat(Animation animation) {}
                
                public void onAnimationEnd(Animation animation) {
                    Handler handler = getHandler();
                    if(handler != null) {
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                int displayedChild = getDisplayedChild();
                                if(dir < 0 && displayedChild + 1 < getChildCount()) {
                                    removeViewAt(displayedChild + 1);
                                }
                                else if(displayedChild > 0) {
                                    getChildAt(displayedChild - 1).setEnabled(false);
                                }
                            }
                        }, 100);
                    }
                }
            });
            
            this.setOutAnimation(outAnimation);
        }
    }

    public void categoryTreeChanged(CategoryTree catTree) {
		Log.i("CategoryTreeView", "categoryTreeChagned() new tree available");
        this.categoryTree = catTree;
        
        View view = this.getCurrentView();
        
        boolean justCreated = false;
        if(view == null) {
            view = new CategoryLevelView(this.getContext(), null, null, this);
            this.addView(view);
            justCreated = true;
        }
        
        if(view instanceof CategoryLevelView) {
            CategoryLevelView catLevelView = (CategoryLevelView) view;
            HierarchicalCategory cat = catLevelView.getCategory();
            try {
                CategoryTreeIterator iterator;
                if(cat == null) {
                    iterator = this.categoryTree.getRootLevelCategories();
                }
                else {
                    iterator = this.categoryTree.getSubCategoriesOf(cat);
                }
                catLevelView.resetCategoryTreeIterator(iterator);
            } catch (CategoryTreeException e) {
                Log.e("CategoryTreeView", "categoryTreeChanged() " + e);
                e.printStackTrace();
            }
        }
        
        if(justCreated) {
            this.setDisplayedChild(0);
        }
        else {
            this.invalidate();
            this.requestLayout();
        }
    }

	public void onGeoCodeChange(AddressInfo addressInfo) {
		if(topRegionID == null || topRegionID != addressInfo.getTopRegionID()){
			this.topRegionID = addressInfo.getTopRegionID();
		}
	}

	public void locationUpdate(LocationInformation locationInformation, LocationProvider locationProvider) {
		this.searchInterface.updateCategoryTree(locationInformation.getMC2Position(), this);
	}

	public void categoryTreeUpdateDone(RequestID requestID, boolean hasChanged) {
		Log.i("CategoryTreeView", "categoryTreeUpdateDone() hasChanged: " + hasChanged);
	}

	public void error(RequestID requestID, CoreError error) {
		Log.e("CategoryTreeView", "error() " + error);
	}
}
