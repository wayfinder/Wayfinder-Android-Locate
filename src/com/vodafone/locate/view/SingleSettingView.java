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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vodafone.locate.R;

public class SingleSettingView extends LinearLayout {

    private TextView titleView;

    public SingleSettingView(Context context) {
        super(context);
        View.inflate(context, R.layout.settings_list_item, this);
        
        this.titleView = (TextView) this.findViewById(R.id.setting_name);
    }
    
    public void setTitle(String title) {
    	this.titleView.setText(title);
    	this.setFocused(this.isSelected() || this.isPressed());
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
			this.titleView.setTextColor(res.getColor(R.color.now_0));
			if(this.isPressed()) {
				this.setBackgroundColor(res.getColor(R.color.now_299_dark));
			}
			else if(this.isSelected()) {
				this.setBackgroundColor(res.getColor(R.color.now_299));
			}
    	}
    	else {
			this.titleView.setTextColor(res.getColor(R.color.now_6_85));
			this.setBackgroundColor(res.getColor(R.color.now_0));
    	}
	}
}
