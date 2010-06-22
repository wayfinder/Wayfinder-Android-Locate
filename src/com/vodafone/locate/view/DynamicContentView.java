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
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.vodafone.locate.MapsApplication;
import com.vodafone.locate.R;
import com.vodafone.locate.servicewindow.WFUriActionHandler;
import com.vodafone.locate.servicewindow.WFWebClient;

public class DynamicContentView extends FrameLayout implements WFUriActionHandler, WFWebClient.ProgressDialogControl{

	private WebView iWebView;
	private WFWebClient iWebClient;
	private ProgressBar progress;
	
	public DynamicContentView(Context context) {
		super(context);
		init(context);
	}

	public DynamicContentView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	}


	private void init(Context context) {
		View.inflate(context, R.layout.dinamic_content_view, this);
		progress = (ProgressBar) findViewById(R.id.progressbar);
		iWebView = (WebView) this.findViewById(R.id.web_view);
		iWebClient = new WFWebClient(this.getContext(), this, this);
		iWebView.setWebViewClient(iWebClient);
        
        //XXX javaScript is disabled
        iWebView.getSettings().setJavaScriptEnabled(false);
	}

	public boolean activated() {
	    Log.e("DinamicContentView", "activated() Implement me!!!");
	    return false;
	}

	public boolean continueToMainMenu() {
		Log.e("DinamicContentView", "continueToMainMenu() Implement me!!!");
		return false;
	}

	public boolean invokePhoneCall(String phoneNumber) {
        Intent dial = new Intent(Intent.ACTION_DIAL); 
        dial.setData(Uri.parse("tel:" + phoneNumber));
        getContext().startActivity(dial);
		return true;
	}

	public boolean openEmailApplication(String anEmailAddress) {
        Intent email = new Intent(Intent.ACTION_VIEW); 
        email.setData(Uri.parse("mailto:" + anEmailAddress));
        getContext().startActivity(email);
        return true;
	}

	public boolean openInExternalBrowser(String anUrl) {
	    Intent web = new Intent(Intent.ACTION_VIEW);
	    web.setData(Uri.parse(anUrl));
	    getContext().startActivity(web);
	    return true;
	}

	public boolean route(int waypoint, String name, int lat, int lon) {
		Log.e("DinamicContentView", "route(...) Implement me!!!");
		return false;
	}

	public boolean runTrial() {
        Log.e("DinamicContentView", "runTrial() Implement me!!!");
		return false;
	}

	public boolean savefavorite(String name, String desc, int lat, int lon) {
	    Log.e("DinamicContentView", "saveFavorite() Implement me!!!");
		return false;
	}

	public boolean sendSMS(String phoneNumber, String text) {
		Log.e("DinamicContentView", "sendSMS() Implement me!!!");
	    return false;
	}

	public boolean setNewServerList(String list) {
        Log.e("DinamicContentView", "setNewServerList() Implement me!!!");
		return false;
	}

	public boolean setUin(String anUin) {
		Log.e("DinamicContentView", "setUin() Implement me!!!");
	    return false;
	}

	public void setWFIDActivationHoldOffParams(int consecutiveStartups, int minDaysRequired, int maxDaysAllowed) {
        Log.e("DinamicContentView", "setWFIDActivationHoldOffParams() Implement me!!!");
	}

	public boolean showOnMap(int lat, int lon, int zoomLevel) {
		Log.e("DinamicContentView", "showOnMap() Implement me!!!");
	    return false;
	}

	public boolean upgradeApplication(String key, String name, String phoneNbr, boolean allowSpam, String email) {
        Log.e("DinamicContentView", "upgradeApplication() Implement me!!!");
		return false;
	}

	public boolean upgradeClient(String anUpgradeUrl) {
        Log.e("DinamicContentView", "upgradeClient() Implement me!!!");
		return false;
	}

	public boolean userTermsAccepted() {
        Log.e("DinamicContentView", "userTermsAccepted() Implement me!!!");
		return false;
	}

	public boolean exitApplication() {
		Log.e("DinamicContentView", "exitApplication() Implement me!!!");
		return false;
	}

	public void dismissLoadingProgress(boolean statusIsOk) {
		progress.setVisibility(View.GONE);
	}

	public void displayLoadingProgress() {
		progress.setVisibility(View.VISIBLE);
	}
	
	public void loadContent(String aUrl){
		iWebClient.loadUrl(iWebView, aUrl);
	}

}
