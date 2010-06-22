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
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.vodafone.locate.R;

public class ProgressBar extends ImageView {

    private boolean mAttached;

	public ProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.init(context);
    }

    public ProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init(context);
    }

    public ProgressBar(Context context) {
        super(context);
        this.init(context);
    }

    private void init(Context context) {
    	setImageResource(R.drawable.progress_bar);
    }
    
    public void setImage(int imageId) {
    	setImageResource(imageId);
    	setVisibility(VISIBLE);
    }
    
    @Override
    public void setVisibility(int visibility) {
    	super.setVisibility(visibility);
    	if(mAttached) {
            AnimationDrawable animationDrawable = (AnimationDrawable) getDrawable();
            if (animationDrawable != null) {
				if (getVisibility() == VISIBLE) {
	    	        animationDrawable.start();
	            } else {
	    	        animationDrawable.stop();
	            }
			}
    	}
    }    
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttached = true;
        if (getVisibility() == VISIBLE) {
        	AnimationDrawable animationDrawable = (AnimationDrawable) getDrawable();
            if (animationDrawable != null) {
            	animationDrawable.start();
        	}
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttached = false;
        AnimationDrawable animationDrawable = (AnimationDrawable) getDrawable();
        if (animationDrawable != null) {
        	animationDrawable.stop();
        }
    }
}
