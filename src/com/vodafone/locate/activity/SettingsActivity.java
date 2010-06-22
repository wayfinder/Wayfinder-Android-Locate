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
package com.vodafone.locate.activity;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.vodafone.locate.R;
import com.vodafone.locate.persistance.ApplicationSettings;
import com.vodafone.locate.view.SingleSettingView;
import com.wayfinder.core.shared.settings.GeneralSettings;

public class SettingsActivity extends AbstractActivity implements OnItemClickListener {

    private ApplicationSettings appSettings;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.setContentView(R.layout.settings_activity);
        
        Resources res = this.getResources();
        
        this.appSettings = ApplicationSettings.get();

        this.setupMeasurements();
        
        ArrayList<String> content = new ArrayList<String>();
        content.add(res.getString(R.string.qtn_andr_368_about_tk));
        ListAdapter adapter = new SettingsArrayAdapter(this, content);
        
        ListView list = (ListView) this.findViewById(R.id.list_settings);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        switch(position) {
            case 0: {
                this.startActivity(new Intent(this, AboutActivity.class));
                return;
            }
        }
    }
   
    @Override
    protected void onPause() {
        super.onPause();
        this.appSettings.commit();
    }
    
    private void setupMeasurements() {
        int units = this.appSettings.getMeasurementSystem();
        switch(units) {
            case GeneralSettings.UNITS_METRIC: {
                RadioButton button = (RadioButton) this.findViewById(R.id.radio_metric);
                button.setChecked(true);
                break;
            }
            case GeneralSettings.UNITS_IMPERIAL_US: {
                RadioButton button = (RadioButton) this.findViewById(R.id.radio_miles_feet);
                button.setChecked(true);
                break;
            }
            case GeneralSettings.UNITS_IMPERIAL_UK: {
                RadioButton button = (RadioButton) this.findViewById(R.id.radio_miles_yards);
                button.setChecked(true);
                break;
            }
        }

        RadioGroup rg = (RadioGroup) this.findViewById(R.id.radio_group_units);
        rg.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup rg, int position) {
                int id = rg.getCheckedRadioButtonId();
                switch(id) {
                    case R.id.radio_metric: {
                        appSettings.setMeasurementSystem(GeneralSettings.UNITS_METRIC);
                        break;
                    }
                    case R.id.radio_miles_feet: {
                        appSettings.setMeasurementSystem(GeneralSettings.UNITS_IMPERIAL_US);
                        break;
                    }
                    case R.id.radio_miles_yards: {
                        appSettings.setMeasurementSystem(GeneralSettings.UNITS_IMPERIAL_UK);
                        break;
                    }
                    default: {
                        appSettings.setMeasurementSystem(GeneralSettings.UNITS_METRIC);
                    }
                }
            }
        });
    }
    
    private static class SettingsArrayAdapter extends ArrayAdapter<String> {
        private Activity context;

        public SettingsArrayAdapter(Activity context, ArrayList<String> content) {
            super(context, R.layout.settings_list_item, content);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	SingleSettingView row = new SingleSettingView(context);
            String title = this.getItem(position);
            if(title != null){
                row.setTitle(title);
            }
            
            return row;
        }
    }
}
