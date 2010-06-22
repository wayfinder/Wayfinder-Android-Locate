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
package com.vodafone.locate.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

public class ServerProtocol {
	
	private ServerProtocol() {
	}
	
	public static InputStream sendHttpPost(String serverUrl, String requestHeader, byte[] data) throws IOException {
		AndroidHttpConnection conn = null;
		OutputStream out = null;
		InputStream in = null;
		try {
			String address = "http://" + serverUrl;
			if(requestHeader != null && requestHeader.length() > 0) {
			    address += "?" + requestHeader;
			}
            conn = new AndroidHttpConnection(address);

			conn.setRequestMethod(AndroidHttpConnection.POST);
			conn.setRequestProperty("Content-Type", "text/xml");
            conn.setRequestProperty("Content-Length", "" + data.length);
            conn.setRequestProperty("User-Agent", "");

			out = conn.openDataOutputStream();
			out.write(data);
			out.flush();
			
			int code = conn.getResponseCode();
            if(code != AndroidHttpConnection.HTTP_OK) {
                throw new IOException("HTTP response code: " + code);
            }

            in = conn.openDataInputStream();
            long length = conn.getLength();
            Log.i("ServerProtocol.sendHttpGet()", "Length of response: " + length);

            byte[] tmp = new byte[512];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = in.read(tmp);
            while(read != -1) {
                baos.write(tmp, 0, read);
                read = in.read(tmp);
            }
            byte[] byteArray = baos.toByteArray();
            Log.i("ServerProtocol", "sendHttpPost() " + new String(byteArray));
            ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
        	return bais;
		}
		finally {
			if(in != null) {
				in.close();
			}
			if(out != null) {
				out.close();
			}
			if(conn != null) {
				conn.close();
			}
		}
	}
}
