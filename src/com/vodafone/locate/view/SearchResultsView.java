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
import android.graphics.Bitmap;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

import com.vodafone.locate.MapsApplication;
import com.vodafone.locate.R;
import com.vodafone.locate.listeners.CategoryLevelChangeListener;
import com.vodafone.locate.persistance.ApplicationSettings;
import com.vodafone.locate.persistance.ApplicationSettings.OnSettingsChangeListener;
import com.vodafone.locate.util.ImageDownloader;
import com.vodafone.locate.util.ImageDownloader.ImageDownloadListener;
import com.wayfinder.core.search.HierarchicalCategory;
import com.wayfinder.core.search.SearchHistory;
import com.wayfinder.core.search.SearchInterface;
import com.wayfinder.core.search.SearchQuery;
import com.wayfinder.core.search.onelist.OneListSearchListener;
import com.wayfinder.core.search.onelist.OneListSearchReply;
import com.wayfinder.core.search.onelist.OneListSearchReply.MatchList;
import com.wayfinder.core.search.onelist.OneListSearchReply.SearchMatch;
import com.wayfinder.core.shared.Position;
import com.wayfinder.core.shared.RequestID;
import com.wayfinder.core.shared.error.CoreError;
import com.wayfinder.core.shared.poidetails.PoiDetail;

public class SearchResultsView extends AbstractResultView implements OneListSearchListener, OnSettingsChangeListener, ImageDownloadListener {

	private static final int PAGING_RESULTS_COUNT = 10;
    
	private CategoryLevelChangeListener listener;
	private HierarchicalCategory topCategory;
    private ListView list;
    private SearchMatch loadingResult;
    private Vector<SearchMatch> content;
    private SearchResultArrayListAdapter adapter;
    private ImageView catImage;
	private LinearLayout layoutLoading;
	private int currMaxIndex;
	private MatchList result;
	private int resultsCount;
	
	public SearchResultsView(Context context, HierarchicalCategory category, CategoryLevelChangeListener listener) {
		super(context);
		View.inflate(context, R.layout.search_results_layout, this);
	    
		
		this.topCategory = category;
		this.content = new Vector<SearchMatch>();
	    
        String catName = category.getCategoryName();
        
        TitleView titleView = (TitleView) this.findViewById(R.id.title);
        titleView.setTitle(catName);

        this.layoutLoading = (LinearLayout) this.findViewById(R.id.layout_loading);
        this.layoutLoading.setVisibility(View.GONE);
        
        this.listener = listener;
        this.loadingResult = new LoadingSearchMatch();
        

        GeoCodeView geoCodeView = (GeoCodeView)findViewById(R.id.geo_code_view);
        geoCodeView.stopListener();
        
        this.catImage = (ImageView) this.findViewById(R.id.image_category);
        if(category != null) {
        	this.catImage.setVisibility(View.VISIBLE);
    		Bitmap image = ImageDownloader.get().queueDownload(this.getContext(), category.getCategoryImageName(), this);
            if(image != null) {
            	this.catImage.setImageBitmap(image);
            }
        }
        else {
        	this.catImage.setVisibility(View.GONE);
        }

        MapsApplication application = (MapsApplication) context.getApplicationContext();
        
        SearchInterface searchInterface = application.getCore().getSearchInterface();
        
        this.adapter = new SearchResultArrayListAdapter(context, category, this.content, listener, this.loadingResult);
        this.list = (ListView) this.findViewById(R.id.search_results_list);
        this.list.setAdapter(this.adapter);
        this.list.setOnItemClickListener(this.adapter);
        this.list.setOnScrollListener(new OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {}
            
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int lastVisibleItem = firstVisibleItem + visibleItemCount;
                if(lastVisibleItem >= totalItemCount && totalItemCount > 0 && currMaxIndex < resultsCount - 1) {
                	if(!content.contains(loadingResult)) {
	       			    content.add(loadingResult);
	        			adapter.notifyDataSetChanged();

	        			postDelayed(new Runnable() {
							public void run() {
								content.remove(loadingResult);
		                        updateContent(PAGING_RESULTS_COUNT);
		            			adapter.notifyDataSetChanged();
							}
	        			}, 500);
                	}
                }
            }
        });
        
        
        Position searchPosition = application.getLastLocationPosition();
        if(searchPosition != null) {
            SearchQuery query = SearchQuery.createPositionalQuery("", category, searchPosition, SearchQuery.RADIUS_SERVER_DEFAULT, true);
            searchInterface.getOneListSearch().search(query, this);

            this.layoutLoading.setVisibility(View.VISIBLE);
        }
        else {
            Log.e("SearchResultsView", "constr() No position, can�t initiate search");
            listener.onCancel();
        }
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		return super.onSaveInstanceState();
	}

	public SearchResultsView(Context context, CategoryLevelChangeListener listener) {
		super(context);
		View.inflate(context, R.layout.search_results_layout, this);
		this.listener = listener;
	}

	public Vector<Object> saveInstanceState() {
		Vector<Object> restoreVector = new Vector<Object>();
		restoreVector.add(this.topCategory);
		restoreVector.add(this.content);
		restoreVector.add(this.loadingResult);
		restoreVector.add(this.catImage);
		restoreVector.add(new Integer(currMaxIndex));
		restoreVector.add(this.catImage);
		restoreVector.add(this.result);
		restoreVector.add(this.resultsCount);
		
		return restoreVector;
	}
	
	@SuppressWarnings("unchecked")
	public void restoreInstanceState(Vector<Object> restoreVector) {
		this.topCategory = (HierarchicalCategory) restoreVector.get(0);
	    this.content =  (Vector<SearchMatch>) restoreVector.get(1);
	    this.loadingResult = (SearchMatch) restoreVector.get(2);
	    this.catImage = (ImageView) restoreVector.get(3);
	    this.currMaxIndex = ((Integer) restoreVector.get(4)).intValue();
	    this.adapter = new SearchResultArrayListAdapter(this.getContext(), topCategory, content, this.listener, loadingResult);
	    this.catImage = (ImageView) restoreVector.get(5);
		this.result = (MatchList) restoreVector.get(6);
		this.resultsCount = ((Integer) restoreVector.get(7)).intValue();
	    
	    String catName = topCategory.getCategoryName();
        
        TitleView titleView = (TitleView) this.findViewById(R.id.title);
        titleView.setTitle(catName);

        this.layoutLoading = (LinearLayout) this.findViewById(R.id.layout_loading);
        this.layoutLoading.setVisibility(View.GONE);
        
		GeoCodeView geoCodeView = (GeoCodeView)findViewById(R.id.geo_code_view);
        geoCodeView.stopListener();
        
        this.catImage = (ImageView) this.findViewById(R.id.image_category);
        if(topCategory != null) {
        	this.catImage.setVisibility(View.VISIBLE);
    		Bitmap image = ImageDownloader.get().queueDownload(this.getContext(), topCategory.getCategoryImageName(), this);
            if(image != null) {
            	this.catImage.setImageBitmap(image);
            }
        }
        else {
        	this.catImage.setVisibility(View.GONE);
        }

        this.list = (ListView) this.findViewById(R.id.search_results_list);
        this.list.setAdapter(this.adapter);
        this.list.setOnItemClickListener(this.adapter);
        this.list.setOnScrollListener(new OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {}
            
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int lastVisibleItem = firstVisibleItem + visibleItemCount;
                if(lastVisibleItem >= totalItemCount && totalItemCount > 0 && currMaxIndex < resultsCount - 1) {
                	if(!content.contains(loadingResult)) {
	       			    content.add(loadingResult);
	        			adapter.notifyDataSetChanged();

	        			postDelayed(new Runnable() {
							public void run() {
								content.remove(loadingResult);
		                        updateContent(PAGING_RESULTS_COUNT);
		            			adapter.notifyDataSetChanged();
							}
	        			}, 500);
                	}
                }
            }
        });
        restoreVector = null;
	}
	
	public void searchDone(RequestID reqID, OneListSearchReply reply) {
        this.searchUpdated(reqID, reply);
        
        if(this.content.size() == 0) {
            if(!this.isSearchCanceled()) {
                this.listener.onCancel();
                Toast.makeText(this.getContext(), R.string.qtn_andr_368_no_places_found_txt, Toast.LENGTH_LONG).show();
            }
        }
	}
	
	public void searchUpdated(RequestID reqID, OneListSearchReply reply) {
		
		Log.e("SearchResultsView", "searchUpdated() " + this.hashCode());
    	this.content.clear();
		this.adapter.notifyDataSetChanged();
		this.currMaxIndex = 0;
        this.result = reply.getMatchList();
        this.resultsCount = result.getNbrOfMatches();

        if(!this.isSearchCanceled()) {
	        this.updateContent(PAGING_RESULTS_COUNT);
        }

        this.layoutLoading.setVisibility(View.GONE);
	}

	private void updateContent(int addResults) {
		int startIndex = this.currMaxIndex;
		this.currMaxIndex += addResults;
		if(this.currMaxIndex > this.resultsCount) {
			this.currMaxIndex = this.resultsCount;
		}
		
		Log.i("SearchResultsView", "updateContent() currMaxIndex: " + this.currMaxIndex);
		if(startIndex < this.currMaxIndex - 1) {
			for(int i = startIndex; i < this.resultsCount && i < this.currMaxIndex - 1; i ++) {
			    this.content.add(result.getMatch(i));
			}
			this.adapter.notifyDataSetChanged();
		}
	}

	public void searchHistoryUpdated(SearchHistory history) {
		Log.i("SearchResultsView", "searchHistoryUpdated() history updated");
	}

    public void error(RequestID requestID, CoreError error) {
    	Log.e("SearchResultsView", "error() " + error.getInternalMsg());
    	if(!this.isSearchCanceled()) {
            this.listener.onCancel();
        }
        this.layoutLoading.setVisibility(View.GONE);
        ((MapsApplication) getContext().getApplicationContext()).error(requestID, error);
    }

    public HierarchicalCategory getCategory() {
        return this.topCategory;
    }

    public void onSettingsChangeListener(ApplicationSettings settings) {
        this.adapter.notifyDataSetInvalidated();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ApplicationSettings.get().setOnSettingsChangeListener(this);
    }
    
    @Override
    protected void onDetachedFromWindow() {
        ApplicationSettings.get().removeOnSettingsChangeListener(this);
        super.onDetachedFromWindow();
  	}
    

    private static class SearchResultArrayListAdapter extends ArrayAdapter<SearchMatch> implements OnItemClickListener {
        private HierarchicalCategory category;
        private CategoryLevelChangeListener listener;
		private SearchMatch loadingSearchMatch;

        public SearchResultArrayListAdapter(Context context, HierarchicalCategory category, Vector<SearchMatch> content, CategoryLevelChangeListener listener, SearchMatch loadingSearchMatch) {
            super(context, R.id.text, content);
            this.category = category;
            this.listener = listener;
            this.loadingSearchMatch = loadingSearchMatch;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Context context = this.getContext();
            View view = convertView;
            SearchMatch match = this.getItem(position);
            if(match.getMatchName().equals(this.loadingSearchMatch.getMatchName())) {
            	view = new LoadingView(context);
            }
            else {
	            if(view == null || !(view instanceof SearchResultView)) {
	                view = new SearchResultView(context, this.category, match);
	            }
	            else {
	                ((SearchResultView) view).setSearchResult(this.category, match);
	            }
            }
            
            return view;
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(view instanceof SearchResultView) {
                SearchResultView matchView = (SearchResultView) view;
                SearchMatch searchResult = matchView.getSearchResult();
                this.listener.onSearchMatchChoose(searchResult, category);
            }
        }
    }

	public void onImageDownloaded(Bitmap origBitmap, String imageName) {
		this.catImage.setImageBitmap(origBitmap);
	}

    private static class LoadingSearchMatch implements SearchMatch {

		public static final String NAME = "LoadingSearchMatch";

		public boolean additionalInfoExists() {
			return false;
		}
		

		public String getMatchBrandImageName() {
			return null;
		}

		public String getMatchCategoryImageName() {
			return null;
		}

		public String getMatchProviderImageName() {
			return null;
		}

		public String getMatchID() {
			return null;
		}

		public String getMatchLocation() {
			return null;
		}

		public String getMatchName() {
			return NAME;
		}

		public Position getPosition() {
			return null;
		}

        public PoiDetail getFilteredInfo() {
            return null;
        }
	}
}
