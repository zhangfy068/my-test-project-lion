/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * SettingsPreferenceActivity.java
 */

package com.newtech.taskmanager;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsPreferenceActivity extends PreferenceActivity {

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.addPreferencesFromResource(R.xml.settings);
    }
}
