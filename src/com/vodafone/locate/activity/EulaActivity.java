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

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.vodafone.locate.R;

public class EulaActivity extends ServiceWindowActivity {
	
	 @Override
	    protected void onCreate(Bundle aSavedInstanceState) {
	        super.onCreate(aSavedInstanceState);

	        Button accept = (Button)findViewById(R.id.accept_button);
	        accept.setOnClickListener(new OnClickListener(){

				public void onClick(View arg0) {
					userTermsAccepted();
				}
	        	
	        });
	        Button reject = (Button)findViewById(R.id.reject_button);
	        reject.setOnClickListener(new OnClickListener(){

				public void onClick(View arg0) {
					exitApplication();
				}
	        	
	        });
	        
	    }

	protected void setContentView() {
        this.setContentView(R.layout.eula);
    }
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean canGoBack = iWebClient.canGoBack();
		if (keyCode == KeyEvent.KEYCODE_BACK && canGoBack) {
			iWebClient.goBack();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK && !canGoBack) {
			application.getServiceWindowHistory().clear();
			exitApplication();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}
