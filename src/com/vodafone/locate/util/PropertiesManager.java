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
package com.vodafone.locate.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class PropertiesManager {

    private static final PropertiesManager INSTANCE = new PropertiesManager();
    private Properties prop;
    
    private PropertiesManager() {
    }
    
    public static final PropertiesManager get() {
        return INSTANCE;
    }
    
    public void init(Context context) {
        AssetManager assets = context.getResources().getAssets();

        this.prop = new Properties();
        InputStream in = null;
        try {
            in = assets.open("properties.prop");
            this.prop.load(in);
        } catch (IOException e) {
            Log.e("MapsApplication", "Static() " + e);
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
        }

        Properties personalProp = new Properties();
        in = null;
        try {
            in = assets.open("personal_properties.prop");
            personalProp.load(in);

            //replace the values that exists in personal_properties.prop
            Set<Object> keySet = personalProp.keySet();
            for(Object key: keySet) {
                String strKey = (String) key;
                this.prop.setProperty(strKey, personalProp.getProperty(strKey));
            }
        } catch (IOException e) {
            Log.e("MapsApplication", "Static() " + e);
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
        }
    }
    
    public String get(String key) {
        return this.prop.getProperty(key);
    }
}
