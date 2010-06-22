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

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.vodafone.locate.MapsApplication;
import com.wayfinder.core.shared.RequestID;
import com.wayfinder.core.shared.error.CoreError;
import com.wayfinder.core.wfserver.WFServerInterface;
import com.wayfinder.core.wfserver.tunnel.TunnelHeaders;
import com.wayfinder.core.wfserver.tunnel.TunnelRequest;
import com.wayfinder.core.wfserver.tunnel.TunnelResponse;
import com.wayfinder.core.wfserver.tunnel.TunnelResponseListener;

public class WFWebClient 
    extends WebViewClient
    implements TunnelResponseListener{
    
    private final static String LOGTAG = WFWebClient.class.getSimpleName();
    
    private WFUriActionHandler iActionHandler;
    private WebView iWebView;
    private ProgressDialogControl progressDialogControl;
    private Context iContext;
    private RequestID lastRequestId;
    
    private MapsApplication application;
    
    

	private boolean isServiceWindowVisible;

    public WFWebClient(Context aContext, WFUriActionHandler actionHandler, ProgressDialogControl aProgressDialogControl) {
        this.progressDialogControl = aProgressDialogControl;
        this.iActionHandler = actionHandler;
        this.iContext = aContext;
        this.application = (MapsApplication) iContext.getApplicationContext();
    }
    
    
    @Override
    public boolean shouldOverrideUrlLoading(WebView aView, final String aUrl) {
        if(Log.isLoggable(LOGTAG, Log.DEBUG)) {
            Log.d(LOGTAG, "URL requested: " + aUrl);
        }
        if(WFUriAction.isAction(aUrl)) {
            if(Log.isLoggable(LOGTAG, Log.DEBUG)) {
                Log.d(LOGTAG, "WF link detected");
            }
            WFUriAction uriAction = new WFUriAction(aUrl, iActionHandler);
            
            String action = uriAction.getAction(aUrl);
            if(WFUriAction.ACTION_WF.equals(action)) {
                try {
                    boolean handled = uriAction.handleWFlink();
                    if(!handled) {
                        Log.e(LOGTAG, "wf-link not handled: " + aUrl);
                    }
                } catch(IllegalArgumentException e) {
                    Log.e(LOGTAG, "wf-link not handled: " + aUrl + ". Error: " + e);
                }
            }
            else if(WFUriAction.ACTION_WTAI_WP_MC.equals(action)) {
                String content = uriAction.removeAction(aUrl);
                iActionHandler.invokePhoneCall(content);
            }
            else if(WFUriAction.ACTION_CALLTO.equals(action)) {
                String content = uriAction.removeAction(aUrl);
                iActionHandler.invokePhoneCall(content);
            }
            else if(WFUriAction.ACTION_MAILTO.equals(action)) {
                String content = uriAction.removeAction(aUrl);
                iActionHandler.openEmailApplication(content);
            } 
            else if(WFUriAction.ACTION_OPENEXT.equals(action)) {
            	 String url = uriAction.removeAction(aUrl);
            	 iActionHandler.openInExternalBrowser(url);
            }
        } else {
        	this.loadUrl(aView, aUrl);
        }
        return true;
    }

    @Override
    public void onPageFinished(WebView aView, String aUrl) {
        super.onPageFinished(aView, aUrl);
        progressDialogControl.dismissLoadingProgress(true);
    }
    
    public void loadUrl(WebView view, String url) {
    	progressDialogControl.displayLoadingProgress();
    	application.getServiceWindowHistory().add(url);
        this.iWebView = view;
        
        WFServerInterface server = application.getCore().getWfServerInterface();
        TunnelRequest request = server.getTunnelFactory().createGETQuery(url);
        Log.i("WFWebClient.loadUrl(...)", "URL: " + url);
        lastRequestId = server.tunnel(request, this);
	}
	
	public void tunnelResponse(RequestID id, TunnelResponse response) {
        int size = application.getServiceWindowHistory().size() - 1;
        if(size < 0) {
            size = 0;
        }
        if(response != null && id.equals(lastRequestId) && isServiceWindowVisible) {
            Log.i(LOGTAG, "tunnelResponse() haz result");
            TunnelHeaders proxHead = response.getResponseHeaders();
            for (int i = 0; i < proxHead.size(); i++) {
                Log.i(LOGTAG, "header: " + proxHead.getKeyAt(i) + "   value:" + proxHead.getValueAt(i));
            }

            if(response.responseWasOK()) {
            	if(iWebView != null && !application.getServiceWindowHistory().isEmpty()){
	                byte[] responseBody = response.getResponseBody();
	                byte[] decodedResponseBody = responseBody;
	                // If the server sent the response encoded as base64, it will be automatically
	                // decoded by core. The transfer encoding should never be base64, but better to check
	                if(response.getBodyTransferEncoding() == TunnelResponse.BODY_ENCODING_BASE64){
	                	String errorMsg = "Server transmit base64 encoded data that core was unable to decode.";
	                	Log.e("Base64", errorMsg);
	                	throw new UnsupportedOperationException(errorMsg);
	                } else if(response.getBodyTransferEncoding() == TunnelResponse.BODY_ENCODING_NONE){
	                	decodedResponseBody = responseBody;
	                }
	
	                String data = new String(decodedResponseBody);
	                data = data.substring(0, data.lastIndexOf(">"));
	                iWebView.loadDataWithBaseURL(application.getServiceWindowHistory().lastElement(), data, "text/html", "UTF-8", application.getServiceWindowHistory().lastElement());
	                Log.i("WFWebClient", "Response body: " + data);
            	}
            } else {
            	progressDialogControl.dismissLoadingProgress(false);
            	application.getServiceWindowHistory().setSize(size);
                Log.e("WFWebClient", "tunnelResponse() error message: " + response.getResponseCode() + ":" + response.getResponseMessage());
                if(response.getResponseCode() >= 300 && response.getResponseCode() < 400){
                	 String newLocation = response.getResponseHeaders().getHeaderValue("location");
                	 if(newLocation != null){
                		 loadUrl(iWebView, newLocation);
                	 }
                }
            }
        } else {
        	progressDialogControl.dismissLoadingProgress(false);
        	application.getServiceWindowHistory().setSize(size);
            Log.e("WFWebClient", "tunnelResponse() returns null or requestIds don't match");
        }
    }

	public void error(RequestID requestID, CoreError error) {
		Log.e("WFWebClient", "tunnelResponse() error message: " + error.getInternalMsg());
		progressDialogControl.dismissLoadingProgress(false);
		int size = application.getServiceWindowHistory().size() - 1;
		if(size < 0) {
			size = 0;
		}
		application.getServiceWindowHistory().setSize(size);
		application.error(requestID, error);
	}

	public boolean canGoBack() {
		return application.getServiceWindowHistory().size() > 1;
	}

	public void goBack() {
		String backUrl = application.getServiceWindowHistory().get(application.getServiceWindowHistory().size() - 2);
		application.getServiceWindowHistory().setSize(application.getServiceWindowHistory().size() - 2);
		loadUrl(iWebView, backUrl);
	}
	
	public void reload(WebView aWebView) {
		iWebView = aWebView;
		String reloadUrl = application.getServiceWindowHistory().get(application.getServiceWindowHistory().size() - 1);
		application.getServiceWindowHistory().setSize(application.getServiceWindowHistory().size() - 1);
		loadUrl(iWebView, reloadUrl);
	}
	

	/**
	 * @param isServiceWindowVisible the isServiceWindowVisible to set
	 */
	public void setServiceWindowVisible(boolean isServiceWindowVisible) {
		this.isServiceWindowVisible = isServiceWindowVisible;
	} 
	
	public interface ProgressDialogControl{
		void dismissLoadingProgress(boolean statusIsOk);
		void displayLoadingProgress();
	}
}
