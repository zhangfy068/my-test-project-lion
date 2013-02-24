/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * Utils.java
 */

package com.newtech.taskmanager.util;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Utils {
	public static int MB_SIZE = 1024 * 1024;

	private static float mTotalMemory = 0;

	/** The ContentResolver for query */
	private final static String[] PROJECT_SELECTION = { Constants._ID,
			Constants.PACKAGE_NAME };

	/**
	 * get the Total Memory of Phone.
	 *
	 * @return total Memory of Phone. The size of memory is MB.
	 */
	public static float getTotalMemory() {
		if (mTotalMemory == 0) {
			MemInfoReader reader = new MemInfoReader();
			reader.readMemInfo();
			mTotalMemory = (float) reader.getTotalSize() / MB_SIZE;
		}
		return mTotalMemory;
	}

	/**
	 * @param a
	 *            instance of AvtivityManager
	 * @return Current free memory of phone. The size of memory is MB.
	 */
	public static float getLastestFreeMemory(ActivityManager am) {
		float freeMemory = 0;
		MemoryInfo mi = new MemoryInfo();
		am.getMemoryInfo(mi);
		freeMemory = (float) mi.availMem / MB_SIZE;
		return freeMemory;
	}

	public static boolean isSupportSwipe(Context context) {
		SharedPreferences sPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return sPref.getBoolean(Constants.SETTINGS_SWIPE_ENABLE, true);
	}

	public static boolean isShowSystemProcess(Context context) {
		SharedPreferences sPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return sPref.getBoolean(Constants.SETTINGS_SHOW_SYSTEM_PROCESS, true);
	}

	public static boolean isAutoRun(Context context) {
		SharedPreferences sPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return sPref.getBoolean(Constants.SETTINGS_AUTO_RUN, true);
	}

	public static boolean isShowService(Context context) {
	    SharedPreferences sPref = PreferenceManager
                .getDefaultSharedPreferences(context);
        return sPref.getBoolean(Constants.SETTINGS_SHOW_SERVICE, true);
	}

	public static boolean isAutoKill(Context context) {
		SharedPreferences sPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return sPref.getBoolean(Constants.SETTINGS_AUTO_KILL, true);
	}

	public static String[] getIgnoreProject() {
		return PROJECT_SELECTION.clone();
	}
}
