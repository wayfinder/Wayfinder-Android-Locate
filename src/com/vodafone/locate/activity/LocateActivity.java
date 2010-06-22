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
package com.vodafone.locate.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import com.vodafone.locate.R;
import com.vodafone.locate.view.CategoryTreeView;
import com.vodafone.locate.view.CombinedDetailsView;

public class LocateActivity extends AbstractStartupActivity {

    private static final String BUNDLE_KEY_CAT_TREE = "cat_tree";
    private static final int MENU_ITEM_SETTINGS_ID = 1;
    private static final int MENU_ITEM_MYLOCATION_ID = 2;

    private CategoryTreeView categoryTreeView;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_locate);
        
        this.categoryTreeView = (CategoryTreeView) this.findViewById(R.id.category_tree_view);
    }
	
//	@Override
//	protected void onSaveInstanceState(Bundle outState) {
//	    SparseArray<Parcelable> container = new SparseArray<Parcelable>();
//        this.categoryTreeView.saveHierarchyState(container);
//        outState.putSparseParcelableArray(BUNDLE_KEY_CAT_TREE, container);
//	    
//	    super.onSaveInstanceState(outState);
//	}
//	
//	@Override
//	protected void onRestoreInstanceState(Bundle savedInstanceState) {
//	    super.onRestoreInstanceState(savedInstanceState);
//
//	    if(savedInstanceState != null) {
//            SparseArray<Parcelable> container = savedInstanceState.getSparseParcelableArray(BUNDLE_KEY_CAT_TREE);
//            this.categoryTreeView.restoreHierarchyState(container);
//        }
//	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ITEM_SETTINGS_ID, 5,
				R.string.qtn_andr_368_settings_tk).setIcon(android.R.drawable.ic_menu_preferences);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		menu.removeItem(MENU_ITEM_MYLOCATION_ID);
		View view = this.categoryTreeView.getCurrentView();
		if ((view instanceof CombinedDetailsView)
				&&((CombinedDetailsView)view).isMapVisible()
				&& (menu.findItem(MENU_ITEM_MYLOCATION_ID) == null)) {
			//add the "My Location" button
			menu.add(Menu.NONE, MENU_ITEM_MYLOCATION_ID, 0,
					"My location").setIcon(android.R.drawable.ic_menu_mylocation);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Intent intent;
		switch (item.getItemId()) {
		case MENU_ITEM_SETTINGS_ID: {
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}
		case MENU_ITEM_MYLOCATION_ID: {
			View view = this.categoryTreeView.getCurrentView();
			if (view instanceof CombinedDetailsView) {
				((CombinedDetailsView)view).showMyLocation();
			}
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if(keyCode == KeyEvent.KEYCODE_BACK) {
	        if(this.categoryTreeView.onKeyBack()) {
	            return true;
	        }
	    }
	    return super.onKeyDown(keyCode, event);

	}
	
	@Override
	public boolean onTrackballEvent (MotionEvent event) {
		return this.categoryTreeView.onTrackballEvent(event);
	}
}
