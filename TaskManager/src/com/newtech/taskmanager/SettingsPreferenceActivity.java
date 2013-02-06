/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * SettingsPreferenceActivity.java
 */

package com.newtech.taskmanager;

import com.newtech.taskmanager.util.Constants;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;

public class SettingsPreferenceActivity extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		this.addPreferencesFromResource(R.xml.settings);

		SwitchPreference autoKill = (SwitchPreference) this
				.findPreference(Constants.SETTINGS_AUTO_KILL);
		autoKill.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				if (newValue instanceof Boolean) {
					boolean isAutokill = (Boolean) newValue;
					Intent intent = new Intent(SettingsPreferenceActivity.this,
							TaskManagerService.class);
					if (isAutokill) {
						startService(intent);
					} else {
						stopService(intent);
					}
				}
				return true;
			}

		});
	}
}
