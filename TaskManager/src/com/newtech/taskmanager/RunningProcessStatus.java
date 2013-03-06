/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * RunningProcessStatus.java
 */

package com.newtech.taskmanager;

import java.util.ArrayList;
import java.util.List;

import com.newtech.taskmanager.util.Constants;
import com.newtech.taskmanager.util.TMLog;
import com.newtech.taskmanager.util.Utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Debug.MemoryInfo;
import android.text.TextUtils;
import android.util.SparseArray;

public class RunningProcessStatus {
	private final static String TAG = "RunningProcessStatus";

	private Context mContext;
	private ActivityManager mAm;
	private PackageManager mPm;
	private ContentResolver mContentResolver;

	RunningProcessStatus(Context context) {
		mContext = context;
		mAm = (ActivityManager) mContext
				.getSystemService(Activity.ACTIVITY_SERVICE);
		mPm = (PackageManager) mContext.getPackageManager();
		mContentResolver = mContext.getContentResolver();
	}

	/**
	 * Get the current proccess's information. Notice: this method will spend
	 * long time. Use it in the work thread.
	 *
	 * @return ArrayList<ProcessInfo> the list of all running application's
	 *         information
	 */
	public synchronized ArrayList<ProcessInfo> getRunningApplication() {
		TMLog.begin(TAG);
		ArrayList<ProcessInfo> runningProcess = new ArrayList<ProcessInfo>();
		final SparseArray<ProcessInfo> tmpAppProcesses = new SparseArray<ProcessInfo>();
		List<RunningAppProcessInfo> runnings = mAm.getRunningAppProcesses();
		PackagesInfo packageInfo = new PackagesInfo(mContext);
		boolean enableIgnore = Utils.isEnableIgnore(mContext);
		// initialize the running application process
		for (RunningAppProcessInfo info : runnings) {
			ApplicationInfo appInfo = packageInfo.getInfo(info.processName);
			if (appInfo != null) {
			    if( enableIgnore && isIgoreProcess(info.processName)) {
			        //If enable ignore list and process is in ignore list
			        continue;
			    }
				ProcessInfo processInfo = new ProcessInfo(info.processName);
				processInfo.setAppInfo(appInfo);
				processInfo.setRunningInfo(info);
                processInfo.updateBasicInfo(mPm);
                tmpAppProcesses.put(info.pid, processInfo);
                runningProcess.add(processInfo);
                if (!processInfo.isSystemProcess()) {
                    TMLog.d(TAG, processInfo.getName(mPm) + " "
                            + appInfo.processName + " Importance:"
                            + processInfo.getImportance());
                }
            }

        }

		// find the running service for process
		List<RunningServiceInfo> services = mAm
				.getRunningServices(Integer.MAX_VALUE);
		for (RunningServiceInfo service : services) {
//			TMLog.d(TAG, service.process + " PID:" + service.pid );
			if (service.started && service.clientLabel != 0 && service.pid > 0) {
				ProcessInfo processInfo = tmpAppProcesses.get(service.pid);
				processInfo.addService(service);
			}
		}
		TMLog.d(TAG, "Memory Check Pointer");
		// update the Memory Info
		// Poor performance here, spend about 3-5s to load memory info
		final int count = runningProcess.size();
		int[] pids = new int[count];
		for (int i = 0; i < count; i++) {
			ProcessInfo processInfo = runningProcess.get(i);
			pids[i] = processInfo.getPid();
		}
		MemoryInfo[] meminfo = mAm.getProcessMemoryInfo(pids);

		for (int i = 0; i < count; i++) {
			ProcessInfo processInfo = runningProcess.get(i);
			processInfo.setMemory(meminfo[i].getTotalPss());
		}
		TMLog.end(TAG);
		return runningProcess;
	}

	   /**
     * Get the current proccess's information. Notice: this method will spend
     * long time. Use it in the work thread.
     *
     * @return ArrayList<ProcessInfo> the list of all running application's
     *         information
     */
    public synchronized ArrayList<ProcessInfo> getRunningAppforService() {
        TMLog.begin(TAG);
        ArrayList<ProcessInfo> runningProcess = new ArrayList<ProcessInfo>();
        final SparseArray<ProcessInfo> tmpAppProcesses = new SparseArray<ProcessInfo>();
        List<RunningAppProcessInfo> runnings = mAm.getRunningAppProcesses();
        PackagesInfo packageInfo = new PackagesInfo(mContext);
        // initialize the running application process
        boolean enableIgnore = Utils.isEnableIgnore(mContext);
        for (RunningAppProcessInfo info : runnings) {
            //ignroe itself
            if(TextUtils.equals(info.processName, "com.newtech.taskmanager")) {
                continue;
            }
            if( enableIgnore && isIgoreProcess(info.processName)) {
                //If enable ignore list and process is in ignore list
                continue;
            }
            ApplicationInfo appInfo = packageInfo.getInfo(info.processName);
            if (appInfo != null && !isIgoreProcess(info.processName)) {
                ProcessInfo processInfo = new ProcessInfo(info.processName);
                processInfo.setAppInfo(appInfo);
                processInfo.setRunningInfo(info);
                processInfo.updateBasicInfo(mPm);
                if (processInfo.isSystemProcess()) {
                    // ignore all system process for service
                    continue;
                }
                tmpAppProcesses.put(info.pid, processInfo);
                TMLog.d(TAG, "Process Name: " + processInfo.getProcessName()
                        + "IMPORTANCE:" + processInfo.getImportance());
                runningProcess.add(processInfo);
            }
        }

        // find the running service for process
        List<RunningServiceInfo> services = mAm
                .getRunningServices(Integer.MAX_VALUE);
        for (RunningServiceInfo service : services) {
            if (service.started && service.clientLabel != 0 && service.pid > 0) {
                ProcessInfo processInfo = tmpAppProcesses.get(service.pid);
                processInfo.addService(service);
            }
        }
        return runningProcess;
    }

	private boolean isIgoreProcess(String processName) {
		Cursor cr = null;
		try {
			cr = mContentResolver.query(Constants.IGNORE_LIST_URI,
					Utils.getIgnoreProject(), Constants.PACKAGE_NAME + "=?",
					new String[] { processName }, null);
			if (cr.moveToFirst()) {
				return true;
			}
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (cr != null) {
				cr.close();
			}
		}
		return false;
	}

	public class PackagesInfo {
		private List<ApplicationInfo> appList;

		public PackagesInfo(Context context) {
			appList = mPm
					.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		}

		public ApplicationInfo getInfo(String name) {
			if (name == null) {
				return null;
			}
			for (ApplicationInfo appinfo : appList) {
				if (name.equals(appinfo.processName)) {
					return appinfo;
				}
			}
			return null;
		}
	}
}
