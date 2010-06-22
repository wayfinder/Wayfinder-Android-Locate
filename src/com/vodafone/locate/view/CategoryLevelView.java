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

import java.util.TreeMap;
import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.vodafone.locate.R;
import com.vodafone.locate.listeners.CategoryLevelChangeListener;
import com.vodafone.locate.util.ImageDownloader;
import com.vodafone.locate.util.ImageDownloader.ImageDownloadListener;
import com.wayfinder.core.search.CategoryTreeException;
import com.wayfinder.core.search.CategoryTreeIterator;
import com.wayfinder.core.search.HierarchicalCategory;

public class CategoryLevelView extends AbstractResultView implements ImageDownloadListener {

	private HierarchicalCategory category;
    private ListView list;
    private Vector<HierarchicalCategory> content;
    private CategoryArrayListAdapter adapter;
    private CategoryTreeIterator iterator;
    private Vector<HierarchicalCategory> extraCategories;
	private ImageView catImage;
	private CategoryLevelChangeListener listener;
	private TitleView titleView;
	
	public CategoryLevelView(Context context, HierarchicalCategory category, CategoryTreeIterator iterator, CategoryLevelChangeListener listener) {
		super(context);
		View.inflate(context, R.layout.category_level_layout, this);
        this.titleView = (TitleView) this.findViewById(R.id.title);
        this.listener = listener;
        this.list = (ListView) this.findViewById(R.id.category_list);
        this.catImage = (ImageView) this.findViewById(R.id.image_category);
		this.iterator = iterator;
		this.category = category;
		this.content = new Vector<HierarchicalCategory>();
		this.adapter = new CategoryArrayListAdapter(context, this.category, this.content, listener);
        init();
        if(this.iterator != null) {
		    this.resetCategoryTreeIterator(iterator);
		}
	}

	public CategoryLevelView(Context context, CategoryLevelChangeListener listener) {
		super(context);
		this.listener = listener;
		View.inflate(context, R.layout.category_level_layout, this);
        this.titleView = (TitleView) this.findViewById(R.id.title);
        this.list = (ListView) this.findViewById(R.id.category_list);
        this.catImage = (ImageView) this.findViewById(R.id.image_category);
	}
	
	private void init() {
		if(category != null) {
		    titleView.setTitle(category.getCategoryName());
		    GeoCodeView geoCodeView = (GeoCodeView) this.findViewById(R.id.geo_code_view);
		    geoCodeView.stopListener();
		}
		else {
		    titleView.setTitle(this.getResources().getString(R.string.qtn_andr_368_find_txt));
		}
		
		
		if(this.category != null) {
			this.catImage.setVisibility(View.VISIBLE);
			Bitmap image = ImageDownloader.get().queueDownload(this.getContext(), this.category.getCategoryImageName(), this);
		    if(image != null) {
		    	this.catImage.setImageBitmap(image);
		    }
		}
		else {
			this.catImage.setVisibility(View.GONE);
		}
		

		this.list.setAdapter(this.adapter);        
		this.list.setOnItemClickListener(this.adapter);        
//		if(this.iterator != null) {
//		    this.resetCategoryTreeIterator(iterator);
//		}
	}
	
	@SuppressWarnings("unchecked")
	public void restoreInstanceState(Vector<Object> restoreVector) {
		this.category = (HierarchicalCategory) restoreVector.get(0);
	    this.iterator = (CategoryTreeIterator) restoreVector.get(1);
	    this.content =  (Vector<HierarchicalCategory>) restoreVector.get(2);
	    this.catImage.setImageDrawable((Drawable) restoreVector.get(3));
		this.adapter = new CategoryArrayListAdapter(this.getContext(), this.category, this.content, listener);
		restoreVector = null;
	    init();
	}
	
	public Vector<Object> saveInstanceState() {
		Vector<Object> restoreVector = new Vector<Object>();
		restoreVector.add(this.category);
		restoreVector.add(this.iterator);
		restoreVector.add(this.content);
		if(this.catImage != null){
			restoreVector.add(this.catImage.getDrawable());
		}else{ 
			restoreVector.add(null);
		}

		return restoreVector;
	}
    
	public HierarchicalCategory getCategory() {
        return this.category;
    }

    public void setClicked(boolean clicked){
    	this.adapter.setClicked(clicked);
    }
    
    public void resetCategoryTreeIterator(CategoryTreeIterator iterator) {
        this.content.clear();
        if(this.category != null) {
            this.content.add(this.category);
        }
        this.updateCategoryTreeListener(iterator);
    }
    
    public void addCategory(HierarchicalCategory category) {
        if(this.extraCategories == null) {
            this.extraCategories = new Vector<HierarchicalCategory>();
        }
        if(!this.extraCategories.contains(category)) {
            this.extraCategories.add(category);
            this.content.add(category);
            this.adapter.notifyDataSetChanged();
        }
    }

    private void updateCategoryTreeListener(CategoryTreeIterator iterator) {
        if(iterator != null) {
        	
        	TreeMap<String, HierarchicalCategory> sortedCats = new TreeMap<String, HierarchicalCategory>(); 
        	
            this.iterator = iterator;
            try {
                for(int i = 0; iterator.hasNext(); i ++) {
                    HierarchicalCategory cat = iterator.next();
                  	sortedCats.put(cat.getCategoryName().toLowerCase(), cat);
                }
            } catch (CategoryTreeException e) {
                Log.e("CategoryLevelView", "updateCategoryTreeIterator() " + e);
                e.getCause().printStackTrace();
            }

            if(this.extraCategories != null) {
                for(HierarchicalCategory cat: this.extraCategories) {
                	sortedCats.put(cat.getCategoryName().toLowerCase(), cat);
                }
            }
            
            for(String name: sortedCats.keySet()) {
            	HierarchicalCategory cat = sortedCats.get(name);
            	this.content.add(cat);
            }

            this.adapter.notifyDataSetChanged();
            this.invalidate();
            this.requestLayout();
        }
    }

	private static class CategoryArrayListAdapter extends ArrayAdapter<HierarchicalCategory> implements OnItemClickListener {
        private CategoryLevelChangeListener listener;
        private HierarchicalCategory category;
		private boolean isClicked;
		
        public CategoryArrayListAdapter(Context context, HierarchicalCategory category, Vector<HierarchicalCategory> content, CategoryLevelChangeListener listener) {
            super(context, R.id.text, content);
            this.listener = listener;
            this.category = category;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Context context = this.getContext();
            View view = convertView;
            HierarchicalCategory category = this.getItem(position);
            boolean isAllCategory = category.equals(this.category);

            String title = category.getCategoryName();
            if(isAllCategory) {
                title = this.getContext().getString(R.string.qtn_andr_368_all_categories_txt, this.category.getCategoryName());
            }
            
            if(view == null || !(view instanceof SingleCategoryView)) {
                view = new SingleCategoryView(context, category, title, isAllCategory);
            }
            else {
                ((SingleCategoryView) view).setCategory(category, title, isAllCategory);
            }
            return view;
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(view instanceof SingleCategoryView) {
            	if(!this.isClicked()){
            		setClicked(true);
	        		SingleCategoryView catView = (SingleCategoryView) view;
	        		HierarchicalCategory category = catView.getCategoty();
	                boolean isAllCategory = catView.isAllCategory();
	                this.listener.onCategoryChoose(category, isAllCategory);
            	}
            }
        }

		public void setClicked(boolean isClicked) {
			this.isClicked = isClicked;
		}

		public boolean isClicked() {
			return isClicked;
		}		
    }

	public void onImageDownloaded(Bitmap origBitmap, String imageName) {
    	this.catImage.setImageBitmap(origBitmap);
	}

	
}
