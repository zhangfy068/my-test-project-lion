/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * TaskManagerService.java
 */

package com.newtech.taskmanager;

import com.newtech.taskmanager.util.TMLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TaskManagerService extends Service {
	private final static String TAG = "TaskManagerService";
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        TMLog.d(TAG, "Service start");
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        TMLog.d(TAG, "Service stopped");
    }
}
