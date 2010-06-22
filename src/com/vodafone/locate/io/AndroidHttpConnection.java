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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

public class AndroidHttpConnection {

    public static final String POST = "POST";
    public static final int HTTP_OK = 200;

    private HttpURLConnection connection;

    public AndroidHttpConnection(String address) throws IOException {
        URL url = new URL(address);
        Log.i("AndroidHttpConnection", "AndroidHttpConnection() " + address);
        this.connection = (HttpURLConnection) url.openConnection();
        this.connection.setConnectTimeout(30000);
        this.connection.setDoInput(true);
        this.connection.setDoOutput(true);
    }

    public void close() throws IOException {
        this.connection.disconnect();
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(this.connection.getInputStream());
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(this.connection.getOutputStream());
    }

    public InputStream openInputStream() throws IOException {
        return this.connection.getInputStream();
    }

    public OutputStream openOutputStream() throws IOException {
        return this.connection.getOutputStream();
    }

	public String getHeaderField(String key) throws IOException {
		return this.connection.getHeaderField(key);
	}

	public long getLength() {
		return this.connection.getContentLength();
	}

	public int getResponseCode() throws IOException {
		return this.connection.getResponseCode();
	}

	public void setRequestMethod(String method) throws IOException {
		this.connection.setRequestMethod(method);
	}

	public void setRequestProperty(String key, String value) throws IOException {
		this.connection.setRequestProperty(key, value);
	}
}
