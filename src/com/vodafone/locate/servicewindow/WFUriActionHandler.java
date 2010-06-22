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


/**
 * This interface should be implemented by an application wanting to use the
 * service window and supporting the wf:// uri actions.
 * 
 */
public interface WFUriActionHandler {
    
    
    /**
     * Indicates that the request to show a position on the map has no
     * prefered zoom level.
     */
    public static final int ZOOM_INVALID = Integer.MIN_VALUE;
    

    /**
     * A request to the implementing application that it should show a position
     * on the map.
     * <p>
     * The zoomlevel is currently sent in the Mercaror format and it is
     * optional for the implementation to honor it. It is however recommended
     * that the zoomlevel is such that the user can actually see the position.
     * 
     * @param aMC2Lat a latitude in MC2 coordinates
     * @param aMC2Lon a longitude in MC2 coordinates
     * @param aZoomLevel a zoomlevel in mercator format or the constant ZOOM_INVALID
     * to indicate that no specific zoomlevel is requested
     * 
     * @return true if and only if the request is fulfilled
     */
    boolean showOnMap(int aMC2Lat, int aMC2Lon, int aZoomLevel);
    
    
    /**
     * Indicates that the waypoint is the origin of route
     */
    public static final int WAYPOINT_ORIGIN      = 0;
    
    
    /**
     * Indicates that the waypoint is the destination of route
     */
    public static final int WAYPOINT_DESTINATION = 1;
    
    
    /**
     * A request to the implementing application that it should set a waypoint
     * in the route.
     * <p>
     * Depending on the value of the aWaypoint parameter, the implementing
     * application should set either start or destination to the position
     * passed to the method.
     * <p>
     * If the outcome of the method is such that both start and destination are
     * set, the implementing application should immediately try to make a route.
     * 
     * @param aWaypoint One of the WAYPOINT constants in this interface
     * @param aName The name of the waypoint.
     * @param aMC2Lat a latitude in MC2 coordinates
     * @param aMC2Lon a longitude in MC2 coordinates
     * 
     * @return true if and only if the request is fulfilled
     */
    boolean route(int aWaypoint, String aName, int aMC2Lat, int aMC2Lon);
    
    
    /**
     * A request to the implementing application that it should save a
     * favorite to persistant memory.
     * <p>
     * It's up to the implementing application to decide if the user should be
     * asked to confirm the name and description of the favorite before it's
     * actually saved into persistant memory.
     * 
     * @param aName The name of the favorite
     * @param aDesc A description of the favorite
     * @param aMC2Lat a latitude in MC2 coordinates
     * @param aMC2Lon a longitude in MC2 coordinates
     * 
     * @return true if and only if the request is fulfilled
     */
    boolean savefavorite(String aName, String aDesc, int aMC2Lat, int aMC2Lon);
    
    
    /**
     * A request that the application should proceed to the main menu.
     * <p>
     * This is actually a signal that the application should proceed and allow
     * the user to go about it's business. It's up to the application which
     * view this actually results in and wether more steps such as loading data
     * etc must be taken before the process is ready.
     * 
     * @return true if and only if the request is fulfilled
     */
    boolean continueToMainMenu();
    
    
    /**
     * A request to the implementation that an SMS should be sent.
     * <p>
     * This method is a request from the webpages that a SMS should be sent
     * with the predefined information. It should not be allowed for the user
     * to see or edit the message since it may contain preformated 
     * information intended to be interpreted by a machine.
     * <p>
     * The actual SMS should be sent in the background and should not hold up
     * the process. It is allowed for the implementation to only take
     * the immediate circumstances such as permissions and radio status when
     * determining if the sending of the SMS was a success. Failures due to
     * network issues may be silently discarded.
     * <p>
     * The implementation may assume that the server is responsible for 
     * ensuring that the content fits in an sms assuming that the transport 
     * uses GSM 7-bit alphabet as defined by the GSM 03.38 -standard. The
     * implementation may also assume that the message is <i>NOT</i> encoded 
     * into GSM SMS 7-bit alphabet and that any such encoding should take place
     * in the client.
     * 
     * @param aPhoneNumber The phone number to send to
     * @param aText The payload of the SMS
     * 
     * @return true if and only if immediate circumstances makes it 
     * theoretically possible to send an SMS
     */
    boolean sendSMS(String aPhoneNumber, String aText);
    
    
    /**
     * A request that the application should proceed as if the client was
     * just activated.
     * <p>
     * The main difference between this method and continueToMainMenu() is that
     * if this method is called, the application should assume that the UIN of
     * the application has been changed and take action accordingly to retrieve
     * all data as if the client was freshly activated. 
     * 
     * @return true if and only if the request is fulfilled
     */
    boolean activated();
    
    
    /**
     * A direct order to the application to quit immediately.
     * <p>
     * As opposed to the rest of the methods in this interface, this order is
     * not up for debate by the application. Upon receiving this call, the
     * application should immediately take any action to save all data and then
     * close down. The user should not be asked to confirm quitting.
     * <p>
     * The usual case when this method is called is when the users account has
     * expired.
     * 
     * @return This method should always return true. returning false signifies
     * a severe error in the application
     */
    boolean exitApplication();
    
    
    /**
     * A request to the client to immediately update the server lists
     * <p>
     * The serverlist is passed in the format:
     * <pre>
     * host:port:type[[,|;]host:port:type]*
     * </pre>
     * <ul>
     * <li>The ',' character is used to separate servers in a group.</li> 
     * <li>The ';' character is used to separate groups of servers.</li> 
     * <li>The type are the same as in server_list in xml-api.</li>
     * </ul>
     * <p>
     * Please note that the service window will not evaluate the list before
     * passing it to this method. Therefor the implementation may not assume 
     * that the server list is formated correctly, and should not accept 
     * malformed data. Also, the implementation should not accept an empty
     * server list since that would cripple the application.
     * 
     * @param aList The serverlist in the above format
     * @return true if and only if the request is fulfilled and the server list
     * is correctly formated.
     */
    boolean setNewServerList(String aList);
    
    
    /**
     * A signal to the application that the user has selected to run in trial
     * mode.
     * 
     * @return true if and only if the request is fulfilled
     */
    boolean runTrial();
    
    
    /**
     * A signal to the application to set the uin as the current uin.
     * <p>
     * After this method is called, the old uin should have been discarded and
     * all future requests towards the server should be made using the new
     * UIN.
     * 
     * @param anUin a UIN
     * @return true if and only if the request is fulfilled
     */
    boolean setUin(String anUin);
    
    
    /**
     * A signal to the application to send an upgrade request to the server.
     * <p>
     * This method is present for legacy reasons, since all applications using
     * Wayfinder ID will never upgrade the account directly.
     * 
     * @param aKey The activation code or activation key
     * @param aName The name of the user
     * @param aPhoneNbr The users phonenumber
     * @param allowSpam true if Wayfinder is allowed to send spam to the user's
     * email address
     * @param email The user's email address
     * 
     * @return true if and only if the request is fulfilled
     */
    boolean upgradeApplication(String aKey, String aName, String aPhoneNbr, boolean allowSpam, String email);
    
    
    /**
     * A signal to the application that the user has accepted the user terms.
     * <p>
     * This method is present for legacy reasons, since all applications using
     * Wayfinder ID will not handle this situation. This will be handled by the
     * webpage
     * 
     * @return true if and only if the request is fulfilled
     */
    public boolean userTermsAccepted();
    
    
    /**
     * A signal to the application that a call should be placed to a phone number.
     * <p>
     * This method is present for legacy reasons, since all applications using
     * Wayfinder ID will not handle this situation. This will be handled by the
     * webpage
     * 
     * @return true if and only if the request is fulfilled
     */
    public boolean invokePhoneCall(String aPhoneNumber);
    
    
    /**
     * A request to the application that the platform email application should
     * be opened for email sending. 
     * <p>
     * If possible the "to" field in the email should be pre-populated with the 
     * email address supplied in the anEmailAddress parameter. If the field
     * cannot be populated this method should return false
     * <p>
     * Since no data such as subject or body is sent, the application should not
     * assume that the email should be sent immediately, but rather leave that
     * action up to the user.
     * 
     * @param anEmailAddress An email address as a String
     *
     * @return true if and only if the request is fulfilled
     */
    public boolean openEmailApplication(String anEmailAddress);
    
    
    /**
     * Saves persistently the WFID firstpage hold off parameters that are sent
     * by the server (N_max, T_dmin, T_dmax). These control how often the
     * firstpage is shown to the user in case of WFID activation. For the
     * algorithm that uses these parameters, see WFIDActivationType.
     * 
     * Called by WFUriAction.getWFIDActivationHoldOffParams();
     * 
     * @param aWFIDConsecutiveStartups - max no of consecutive startups allowed
     * without showing firstpage, (N_max from server)
     * 
     * @param aWFIDMinDaysRequired - no of days that should pass between showing
     * firstpage (T_dmin from server)
     * 
     * @param aWFIDMaxDaysAllowed - max no of days that may pass between showing
     * firstpage (T_dmax from server)
     */
    public void setWFIDActivationHoldOffParams(int aWFIDConsecutiveStartups, int aWFIDMinDaysRequired, int aWFIDMaxDaysAllowed);


    /**
     * A directive to the client that it should initiate an upgrade of the
     * currently installed client.
     * <p>
     * The exact method for initiating the upgrade is up to each platform to
     * decide, but the method used should make every effort to ensure that any
     * stored settings etc remains after the installation.
     * 
     * @param anUpgradeUrl A url pointing to the new installation package
     * @return true if and only if the request is fulfilled
     */
    public boolean upgradeClient(String anUpgradeUrl);


    /**
     * A directive to the client that it should open the supplied url in the
     * external device browser
     * 
     * @param anUrl A url that should be opened in the external device browser
     * @return true if and only if the request was fulfilled
     */
    public boolean openInExternalBrowser(String anUrl);
    
    
}
