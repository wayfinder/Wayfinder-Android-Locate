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

import android.content.Context;

import com.vodafone.locate.R;
import com.wayfinder.core.shared.util.UnitsFormatter;
import com.wayfinder.core.shared.util.UnitsFormatterSettings;

public class AndroidUnitsFormatter extends UnitsFormatter {

    public AndroidUnitsFormatter(int unitSystem, Context context) {
        super(new AndroidUnitsFormatterSettings(unitSystem, context));
    }
    
    private static class AndroidUnitsFormatterSettings extends UnitsFormatterSettings {
        
    	private Context context;
        /**
         * Create a new instance.
         * 
         * @param unitSystem - the unit system to use.
         * @see UnitsFormatterSettings#UnitsFormatterConfig(int)
         */
        public AndroidUnitsFormatterSettings(int unitSystem, Context context) {
            super(unitSystem);
            this.context = context;
        }
    
        protected UnitsFormatterSettings getFrozenInstance() {
            return this; // we have no mutable data 
        }
        
        /**
         * Returns the string for long distance. E.g. "miles".
         * 
         * @return the string for long distance.
         */
        public String getLongDistance() {
            if (getUnitSystem() == UNITS_MILES_FEET
                || getUnitSystem() == UNITS_MILES_YARDS) {
                // STR_MILES
            	return context.getString(R.string.qtn_andr_368_miles_txt);
            } else {
                // use metric as default for unknowns.
                // KILOMETERS
            	return context.getString(R.string.qtn_andr_368_km_txt);
            }
        }
        
        /**
         * Returns the abbreviation for long distance. E.g. "mi", "km".
         * 
         * @return the abbreviation for long distance.
         */
        public String getLongDistanceAbbr() {
            if (getUnitSystem() == UNITS_MILES_FEET
                || getUnitSystem() == UNITS_MILES_YARDS) {
                // STR_MILES_ABBREVIATION
            	return context.getString(R.string.qtn_andr_368_miles_abbrev_txt);
            } else {
                // use metric as default for unknowns.
                // KILOMETERS
            	return context.getString(R.string.qtn_andr_368_km_txt);
            }
        }
    
        /**
         * Returns the string for short distance. E.g. "yards", "feet".
         * 
         * @return the string for short distance.
         */
        public String getShortDistance() {
            switch (getUnitSystem()) {
            case UNITS_MILES_FEET:
                // STR_FEET
            	return context.getString(R.string.qtn_andr_368_feet_txt);
    
            case UNITS_MILES_YARDS:
                // STR_YARDS
            	return context.getString(R.string.qtn_andr_368_yards_txt);
                
            default: // UNITS_METRIC and default
                // STR_METERS_ABBREVIATION
            	return context.getString(R.string.qtn_andr_368_metre_txt);
            }
        }
    
        /**
         * Returns the abbreviation for short distance. E.g. "yds", "ft", "m".
         * 
         * @return the abbreviation for short distance.
         */
        public String getShortDistanceAbbr() {
            switch (getUnitSystem()) {
            case UNITS_MILES_FEET:
                // STR_FEET_ABBREVIATION
                return context.getString(R.string.qtn_andr_368_feet_abbrev_txt);
    
            case UNITS_MILES_YARDS:
                // yards_abbr
                return context.getString(R.string.qtn_andr_368_yards_abbrev_txt);
                
            default: // UNITS_METRIC and default
                // STR_METERS_ABBREVIATION
            	return context.getString(R.string.qtn_andr_368_metre_txt);
            }
        }
    
        /**
         * Returns the localized decimal marker. Currently, always initialized to
         * "." due to limited font support in old jWMMG-code.
         * (Subject to change without notice.)
         * 
         * @return the localized decimal marker.
         */
        public String getDecimalMarker() {
            return context.getString(R.string.qtn_andr_368_decimal_marker_txt);
        }
    
        public String getHoursAbbr() {
            // wf_ROUTEV_ETG_HOURS
        	return context.getString(R.string.qtn_andr_368_hours_txt);
        }
        
        public String getMinutesAbbr() {
            // mins_abbr
        	return context.getString(R.string.qtn_andr_368_minutes_txt);
        }
        
        public String getSecondsAbbr() {
            // STR_SECONDS_ABBREVIATION
            return context.getString(R.string.qtn_andr_368_seconds_txt);
        }
    
        public String getSpeedAbbr() {
            if (getUnitSystem() == UNITS_MILES_FEET
                || getUnitSystem() == UNITS_MILES_YARDS) {
                // wayfinder_mph_text
                return context.getString(R.string.qtn_andr_368_miles_per_hour_txt);
            } else {
                // use metric as default for unknowns.
                // wayfinder_kmh_text
            	return context.getString(R.string.qtn_andr_368_km_per_hour_txt);
            }
        }
    }
}
