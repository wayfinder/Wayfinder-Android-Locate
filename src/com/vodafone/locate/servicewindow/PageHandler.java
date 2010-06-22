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
package com.vodafone.locate.servicewindow;

import android.util.Log;
import android.webkit.WebView;

public class PageHandler {
    
    protected static final int COMMONPARAMS_EXPECTED_LEN = 122;
    private static final String WF_SERVICES_URL = "http://startpage/";
    private static final String FIRSTPAGE_URL = "http://firstpage/";
    private static final String NEW_FIRSTPAGE_URL = "http://firstpage/vf_eula_static.php";

    private final WebView iView;
    private final WFWebClient iWebClient;
    private String detailsId;

    public PageHandler(WebView aView, WFWebClient aHandler) {
        iView = aView;
        iWebClient = aHandler;
    }
    
    public void openAcceptPage() {
        this.detailsId = null;

        iWebClient.loadUrl(iView, FIRSTPAGE_URL);
    }
    
    public void openFirstPage() {
    	this.detailsId = null;
    	
    	iWebClient.loadUrl(iView, NEW_FIRSTPAGE_URL);
    }
    
    public void openServices() {
        this.detailsId = null;
        iWebClient.loadUrl(iView, makeWFServicesURL("index", null));
    }
    
    public void openServices(String aUrl) {
        this.detailsId = null;

        StringBuffer sb = new StringBuffer();
        sb.append(aUrl);
        iWebClient.loadUrl(iView, sb.toString());
    }
    
    public void openPlaceDetails(String detailsId) {
        this.detailsId = detailsId;

        //http://services-1-eu.services.wayfinder.com/show_info.php?srvstring=C:612349435:-3267130:0:Epsom%20General%20Hospital
        StringBuffer sb = new StringBuffer(100);
        sb.append("srvstring=");
        sb.append(detailsId);
        String url = makeWFServicesURL("show_android", sb);
        Log.i("PageHandler", "openPlaceDetails() url: " + url);
        iWebClient.loadUrl(iView, url);
    }
    
    public String getDetailsId() {
        if(this.detailsId == null) {
            this.detailsId = "";
        }
        return this.detailsId;
    }
    
    /**
     * equivalent to makeWFServicesURL(WF_SERVICES_URL, aPageBase, aParams)
     *
     * used for pages outside the start-up flow.
     */
    private static String makeWFServicesURL(String aPageBase, StringBuffer aParams) {
        return makeWFServicesURL(WF_SERVICES_URL, aPageBase, aParams);
    }

    /**
     * the url is formed as aURLBase + aPage + "." +
     * languageCode + ".php?" + aParams (if it is not null) +
     * parameters added for all pages.
     *
     * @param aURLBase must not be null
     * @param aPageBase must not start with "/", must not be null and
     * must not contain any parameters
     * @param aParams must not start (or end) with "?" or "&" since
     * there might be a need in the future to add a certain parameter
     * first of all. A StringBuffer is used since most callers need to
     * add several parameters by using a StringBuffer and it is
     * unnecessary to convert that into a String and then append that
     * to an internal buffer in this method. If no parameters are
     * needed, aParams may be null.
     */
    private static String makeWFServicesURL(String aURLBase,
                                       String aPageBase,
                                       StringBuffer aParams) {

        int n = aURLBase.length() + aPageBase.length();
        if (aParams != null) {
            n += aParams.length();
        }

        n += 5  // .php?
            + COMMONPARAMS_EXPECTED_LEN
            + 10; // slack

        StringBuffer sb = new StringBuffer(n);
        sb.append(aURLBase);
        sb.append(aPageBase);
        sb.append(".php?");
        if (aParams != null && aParams.length() > 0) {
            sb.append(aParams.toString());
        }

        String s = sb.toString();

        return s;
    }
}
