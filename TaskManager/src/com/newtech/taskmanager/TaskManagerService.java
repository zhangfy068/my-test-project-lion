/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * TaskManagerService.java
 */

package com.newtech.taskmanager;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.newtech.taskmanager.util.TMLog;
import com.newtech.taskmanager.util.Utils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TaskManagerService extends Service {
    private final static String TAG = "TaskManagerService";
    private RunningProcessStatus mRunningStatus;

    private static long INTERVAL = 30 * 1000;
    private static int MEMORY_LEVEL = 50;

    private List<ProcessInfo> mAppListAll;
    private ScheduleTaskThread mAutoKillThread;

    private ActivityManager mAm;
    private float mTotalMemory;
    private AtomicBoolean mRunning;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mRunningStatus = new RunningProcessStatus(this);
        mAutoKillThread = new ScheduleTaskThread();
        mAm = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        mTotalMemory = Utils.getTotalMemory();
        mRunning = new AtomicBoolean();
        mRunning.set(true);
        mAutoKillThread.start();
        TMLog.d(TAG, "Service start");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        TMLog.d(TAG, "Service stopped");
        mRunning.set(false);
        if (mAutoKillThread != null) {
            mAutoKillThread = null;
        }
    }

    private int getPercentAgeOfAvailMemory() {
        float avail = Utils.getLastestFreeMemory(mAm);
        return (int) (avail * 100 / mTotalMemory);
    }

    private synchronized void killProcesses() {
        // At first, all EMPTY PROCESS will be killed.
        TMLog.begin(TAG);
        int count = mAppListAll.size();
        for (int i = 0; i < count; i++) {
            ProcessInfo process = mAppListAll.get(i);
            if (!process.isService()) {
                process.killSelf(this);
                mAppListAll.remove(i);
                i--;
                count--;
            }
        }
        if (getPercentAgeOfAvailMemory() < MEMORY_LEVEL) {
            count = mAppListAll.size();
            for (int i = 0; i < count; i++) {
                ProcessInfo process = mAppListAll.get(i);
                process.killSelf(this);
                mAppListAll.remove(i);
                i--;
                count--;
            }
        }
    }

    class ScheduleTaskThread extends Thread {
        @Override
        public void run() {
            while (mRunning.get()) {
                try {
                    sleep(INTERVAL);
                    TMLog.d(TAG, "Start to autokill task");
                    if (getPercentAgeOfAvailMemory() < MEMORY_LEVEL) {
                        float before = Utils.getLastestFreeMemory(mAm);
                        mAppListAll = mRunningStatus.getRunningAppforService();

                        killProcesses();
                        sleep(5000);
                        float after = Utils.getLastestFreeMemory(mAm);
                        float release = after - before;
                        TMLog.d(TAG, "=========== " + release
                                + " MB Memroy has been released!========");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
