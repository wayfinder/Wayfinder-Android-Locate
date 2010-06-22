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

import java.util.StringTokenizer;

import android.util.Log;

public class VersionUtil {
    
    /**
     * compares versions
     * @param version1
     * @param version2
     * @return if version1 > version2, -1 is returned. If version2 > version1, +1 is returned. If version1 = version2, 0 is returned
     */
    public static int compareVersions(String version1, String version2) {
        int majVer1 = 0;
        int medVer1 = 0;
        int minVer1 = 0;
        StringTokenizer tokenizer = new StringTokenizer(version1, ".");
        try {
            if(tokenizer.hasMoreTokens()) {
                majVer1 = Integer.parseInt(tokenizer.nextToken());
                if(tokenizer.hasMoreTokens()) {
                    medVer1 = Integer.parseInt(tokenizer.nextToken());
                    if(tokenizer.hasMoreTokens()) {
                        minVer1 = Integer.parseInt(tokenizer.nextToken());
                    }
                }
            }
        } catch(Exception e) {
            Log.e("VersionUtil", "compareVersions() version1: " + version1 + ", " + e);
        }

        int majVer2 = 0;
        int medVer2 = 0;
        int minVer2 = 0;
        tokenizer = new StringTokenizer(version2, ".");
        try {
            if(tokenizer.hasMoreTokens()) {
                majVer2 = Integer.parseInt(tokenizer.nextToken());
                if(tokenizer.hasMoreTokens()) {
                    medVer2 = Integer.parseInt(tokenizer.nextToken());
                    if(tokenizer.hasMoreTokens()) {
                        minVer2 = Integer.parseInt(tokenizer.nextToken());
                    }
                }
            }
        } catch(Exception e) {
            Log.e("VersionUtil", "compareVersions() version1: " + version1 + ", " + e);
        }

        int compare = 0;
        if(majVer1 > majVer2) {
            compare = -1;
        }
        else if(majVer1 < majVer2) {
            compare = 1;
        }
        else if(medVer1 > medVer2) {
            compare = -1;
        }
        else if(medVer1 < medVer2) {
            compare = 1;
        }
        else if(minVer1 > minVer2) {
            compare = -1;
        }
        else if(minVer1 < minVer2) {
            compare = 1;
        }
        return compare;
    }
}
