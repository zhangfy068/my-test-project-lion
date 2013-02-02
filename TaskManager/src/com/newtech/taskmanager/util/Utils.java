/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * Utils.java
 */
package com.newtech.taskmanager.util;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;

public class Utils {
	public static int MB_SIZE = 1024 * 1024;

	private static float mTotalMemory = 0;

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
	 * @param a instance of AvtivityManager
	 * @return Current free memory of phone. The size of memory is MB.
	 */
	public static float getLastestFreeMemory(ActivityManager am) {
		float freeMemory = 0;
		MemoryInfo mi = new MemoryInfo();
		am.getMemoryInfo(mi);
		freeMemory = (float) mi.availMem / MB_SIZE;
		return freeMemory;
	}
}
