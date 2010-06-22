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

import java.util.Hashtable;

import android.util.Log;

import com.wayfinder.core.shared.Position;
import com.wayfinder.core.shared.util.URITool;

/**
 * Parses and handles an WFUri link from the service window
 */
class WFUriAction {
    
    protected static final int PARAM_COORD_LAT = 0;
    protected static final int PARAM_COORD_LON = 1;
    
    public static final String ACTION_WF = "wf://";
    public static final String ACTION_CALLTO = "callto://";
    public static final String ACTION_WTAI_WP_MC = "wtai://wp/mc;";
    public static final String ACTION_MAILTO = "mailto:";
    public static final String ACTION_OPENEXT = "openext:";

    private static final String[] iActions = new String[] {ACTION_WF, ACTION_CALLTO, ACTION_WTAI_WP_MC, ACTION_MAILTO, ACTION_OPENEXT};

    private WFUriActionHandler handler;
    private String actionName;
    private Hashtable<String, String> parameterTable;
    private boolean browserShouldClose;
    
    /**
     * Standard constructor
     * <p>
     * This class will take and parse an wf:// uri. 
     * 
     * @param wfUri The URL as a String. The 
     * @param handler The application-specific WFUriActionHandler
     * @throws IllegalArgumentException if the URI could not be parsed or if the URI was null
     */
    WFUriAction(String wfUri, WFUriActionHandler handler) throws IllegalArgumentException {
        this.handler = handler;
        
        if(!isAction(wfUri)) {
            throw new IllegalArgumentException("No Uri supplied: " + wfUri);
        }
        
        /* remove "wf://" */
        String workUri = this.removeAction(wfUri);
        
        /* find the action */
        int index = workUri.indexOf('?');
        if (index < 0) {
            //if there are no params we just take the actionName
            index = workUri.length();
        }
        String actionName = workUri.substring(0, index);
        if(actionName.endsWith("/")) {
            actionName = workUri.substring(0, index - 1);
        } else {
            this.actionName = actionName;
        }
        String params = null;
        if (index != workUri.length()){
            /* find params list */
            params = workUri.substring(index + 1, workUri.length());
        }
        
        this.parameterTable = new Hashtable<String, String>();
        if(params != null) {
            URITool.parseHTTPQueryURL(wfUri, this.parameterTable);
        }
    }
    
    public String removeAction(String wfUri) {
        for(String action: iActions) {
            if(wfUri.startsWith(action)) {
                String wfWork = wfUri.substring(action.length());
                return wfWork;
            }
        }
        
        return wfUri;
    }
    
    public String getAction(String wfUri) {
        for(String action: iActions) {
            if(wfUri.startsWith(action)) {
                return action;
            }
        }
        
        return null;
    }

    public static boolean isAction(String aAction) {
        if (aAction == null || aAction.length() == 0) {
            return false;
        }
        for (int i = 0; i < iActions.length; i ++) {
            if (aAction.startsWith(iActions[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Will attempt to service the action contained in the wf:// uri
     * <p>
     * Calling this method will only work if a WFUriActionHandler handling the
     * actions has been passed to the constructor. If no WFUriActionHandler has
     * been passed to the object, this method will always return false
     * 
     * @return true if and only if the WFUriActionHandler reports that the
     * request was fulfilled.
     * @throws IllegalArgumentException if the request was missing mandatory
     * parameters
     */
    boolean handleWFlink() throws IllegalArgumentException {
        if(this.handler == null) {
            // the action cannot be handled
            return false;
        }
        
        if("startup".equals(actionName)) {
            getWFIDActivationHoldOffParams();
            return handleStartup();
        } else if ("mapview".equals(actionName)) {
            int lat = getUrlParamCoordinate(PARAM_COORD_LAT);
            int lon = getUrlParamCoordinate(PARAM_COORD_LON);
            int zoom;
            try {
                zoom = getOptionalUrlParamInt("zoom");
                if (zoom > 5) {
                    zoom = 5;
                }
            } catch (Exception e) {
                // zoom absent or not parsable
                zoom = WFUriActionHandler.ZOOM_INVALID;
            }
            return handler.showOnMap(lat, lon, zoom);
        
        } else if("route".equals(actionName)) {
            String action = getMandatoryUrlParamString("action");
            int waypoint;
            if ("dest".equals(action) || "navto".equals(action)) {
                waypoint = WFUriActionHandler.WAYPOINT_DESTINATION;
            } else if ("orig".equals(action)) {
                waypoint = WFUriActionHandler.WAYPOINT_ORIGIN;
            } else {
                throw new IllegalArgumentException("unknown action for route");
            }
            browserShouldClose = true;
            return handler.route(waypoint, 
                    getMandatoryUrlParamString("name"),
                    getUrlParamCoordinate(PARAM_COORD_LAT), 
                    getUrlParamCoordinate(PARAM_COORD_LON));
        } else if ("favorite".equals(actionName) || "favourite".equals(actionName)) {
            if ("add".equals(getMandatoryUrlParamString("action"))) {  
                String desc;
                try {
                    desc = getMandatoryUrlParamString("desc");
                } catch(IllegalArgumentException iae) {
                    desc = null;
                }
                return handler.savefavorite(
                        getMandatoryUrlParamString("name"),
                        desc,
                        getUrlParamCoordinate(PARAM_COORD_LAT), 
                        getUrlParamCoordinate(PARAM_COORD_LON));
            }
        } else if ("mainmenu".equals(actionName)) {
            getWFIDActivationHoldOffParams();
            browserShouldClose = true;
            return handler.continueToMainMenu();
        } else if("sendsms".equals(actionName)) {
            return handler.sendSMS(getMandatoryUrlParamString("phone"), 
                                    getMandatoryUrlParamString("smstext"));
        } else if("upgradeclient".equals(actionName)) {
            return handler.upgradeClient(getMandatoryUrlParamString("url"));
        } else if("openext".equals(actionName)) {
            return handler.openInExternalBrowser(getMandatoryUrlParamString("url"));
        }

        throw new IllegalArgumentException("No handling of current action: " + actionName);
    }
    
    private boolean handleStartup() throws IllegalArgumentException {
        String startupAction = getMandatoryUrlParamString("action");
        if("activated".equals(startupAction) || "reactivated".equals(startupAction)) {
            browserShouldClose = true;
            return handler.activated();
        } else if("exit".equals(startupAction)) {
            browserShouldClose = true;
            return handler.exitApplication();
        } else if("newserverlist".equals(startupAction)) {
            // succesuri is mandatory
            if(parameterTable.containsKey("successuri")) {
                return handler.setNewServerList(getMandatoryUrlParamString("serverlist"));
            }
        } else if("run_trial".equals(startupAction)) {
            browserShouldClose = true;
            return handler.runTrial();
        } else if("setuin".equals(startupAction)) {
            return handler.setUin(getMandatoryUrlParamString("uin"));
        } else if("shownews_complete".equals(startupAction) || "startup_complete".equals(startupAction)) {
            browserShouldClose = true;
            return handler.continueToMainMenu();
        } else if("upgrade".equals(startupAction)) {
            return handler.upgradeApplication(
                                        getOptionalUrlParamString("keystr"),
                                        getOptionalUrlParamString("name"),
                                        getOptionalUrlParamString("phone"),
                                        getOptionalUrlParamBoolean("allow_email"),
                                        getOptionalUrlParamString("email"));
        } else if("upgrade_wayfinder_done".equals(startupAction)) {
            browserShouldClose = true;
            return handler.activated();
        } else if("userterms_accept".equals(startupAction) || "us_disclaimer_accept".equals(startupAction)) {
            return handler.userTermsAccepted();
        } else if("userterms_reject".equals(startupAction) || "us_disclaimer_reject".equals(startupAction)) {
            browserShouldClose = true;
            return handler.exitApplication();
        }
        
        throw new IllegalArgumentException("No handling of current startup action: " + startupAction);
    }
    
    /**
     * Returns the uri to go to after the request was handled.
     * <p>
     * <ul>
     * <li>If <code>wasSuccessful</code> is true, this method will return the 
     * value of the uri successuri parameter or null if no successuri 
     * exists.</li>
     * <li>If <code>wasSuccessful</code> is false, this method will return the
     * value of the uri failureuri paramter or null if no failureuri exists</li>
     * 
     * @param wasSuccessful true if the request was succesful
     * @return The next uri or null
     */
    String getNextUrl(boolean wasSuccessful) {
        if(wasSuccessful) {
            return getOptionalUrlParamString("successuri");
        }
        return getOptionalUrlParamString("failureuri");
    }
    
    /**
     * Returns true if the browser should be closed after the action
     * <p>
     * This should only be checked if no failure or successurl was detected
     * 
     * @return true if the browser should be closed.
     */
    boolean browserShouldClose() {
        return browserShouldClose;
    }
    
    //-------------------------------------------------------------------------
    // Getting parameters from the table
    
    /**
     * retrieves the URL parameter aParamName from iUrlParams and
     * return it as a string. If the parameter is not found, an 
     * IllegalArgumentException is thrown
     */
    private String getMandatoryUrlParamString(String aParamName) 
    throws IllegalArgumentException {
        
        Object val = parameterTable.get(aParamName);
        if(val instanceof String) {
            // ensure that the string is not empty
            String s = ((String)val).trim();
            if(s.length() > 0) {
                return s;
            }
        }
        
        throw new IllegalArgumentException(aParamName + " not found.");
    }
    
    private String getOptionalUrlParamString(String aParamName) {
        Object val = parameterTable.get(aParamName);
        if(val instanceof String) {
            return (String) val;
        }
        return null;
    }

    /**
     * retrieves the URL parameter aParamName from iUrlParams and
     * return it as a boolean. If the parameter is not found or has
     * any other value than "true", false is returned.
     */
    private boolean getOptionalUrlParamBoolean(String aParamName) {
        Object val = parameterTable.get(aParamName);
        if(val instanceof String) {
            String s = (String) val;
            if (s.equalsIgnoreCase("true")) {
                return true;
            }
        }
        return false;
    }

    /**
     * the URL parameter aParamName from iUrlParams and return it as a
     * int. If the parameter is not found, IllegalArgumentException is thrown.
     */
    private int getOptionalUrlParamInt(String aParamName) {
        Object val = parameterTable.get(aParamName);
        if(val instanceof String) {
            String s = (String) val;
            try {
                return Integer.parseInt(s);
            } catch(NumberFormatException nfe) {
                throw new IllegalArgumentException(aParamName + " has value '" + s + "' - NOT AN INT");
            }
        }
        throw new IllegalArgumentException(aParamName + " not found.");
    }

    /**
     * @param aLatLon: PARAM_COORD_LAT or PARAM_COORD_LON
     */
    protected int getUrlParamCoordinate(int aLatLon) {
        String paramNameMC2;
        String paramNameWGS84Deg;

        if (aLatLon == PARAM_COORD_LON) {
            paramNameMC2 = "lon";
            paramNameWGS84Deg = "wgs84lon";
        }
        else {
            paramNameMC2 = "lat";
            paramNameWGS84Deg = "wgs84lat";
        }

        Object val = parameterTable.get(paramNameMC2);
        if (val != null) {
            return Integer.parseInt((String) val);
        }
        
        val = parameterTable.get(paramNameWGS84Deg);
        if (val != null) {
            double wgs84dd = Double.parseDouble((String) val);
            return Position.decimalDegresToMc2(wgs84dd);
        }
        
        throw new IllegalArgumentException();
    }
    /**
     * Method used to check if the server sent the parameters for showing the
     * firstpage and save them accordingly. The parameters are:
     * <b>N_max</b> = no of consecutive startups allowed without showing firstpage
     * <b>T_dmin</b> = no of days that should pass between showing firstpage
     * <b>T_dmax</b> = max no of days that may pass between showing firstpage
     * These parameters are parsed and stored for the following uris:
     * wf://startup and wf://firstpage
     */
    protected void getWFIDActivationHoldOffParams() {
    	// max no of consecutive startups allowed without showing firstpage
        int aWFIDConsecutiveStartups;
        // no of days that should pass between showing firstpage
        int aWFIDMinDaysRequired;
        // max no of days that may pass between showing firstpage
        int aWFIDMaxDaysAllowed;
        
        try {
        	aWFIDConsecutiveStartups = getOptionalUrlParamInt("N_max");
        	aWFIDMinDaysRequired = getOptionalUrlParamInt("T_dmin");
        	aWFIDMaxDaysAllowed = getOptionalUrlParamInt("T_dmax");
        	//save the params here
        	handler.setWFIDActivationHoldOffParams(aWFIDConsecutiveStartups, aWFIDMinDaysRequired, aWFIDMaxDaysAllowed);
        } catch (Exception e) {
        	//we have all params or none so ignore them here
        	Log.e("WFUriAction", "FIRSTPAGE PARAMS not sent, keep old behavior");
        }
    }
}
