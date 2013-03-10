/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * TaskManagerService.java
 */

package com.newtech.taskmanager;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.newtech.taskmanager.util.TMLog;
import com.newtech.taskmanager.util.Utils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class TaskManagerService extends Service {
    private final static String TAG = "TaskManagerService";
    private RunningProcessStatus mRunningStatus;

    private static final String NOTIFICATION_TAG = "com.newtech.taskmanager";
    private static final int NOTIFICATION_ID = 0;

    private static final int RELEASE_COMPLETE = 1;
    private static final String RELEASE_MEMORY = "release_memeory";
    private Handler mHandler;

    private static long INTERVAL = 60 * 1000 * 3;
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
    public void onCreate() {
        super.onCreate();
        mRunningStatus = new RunningProcessStatus(this);
        mAm = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        mTotalMemory = Utils.getTotalMemory();
        mRunning = new AtomicBoolean();
        mRunning.set(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == RELEASE_COMPLETE) {
                    Bundle bundle = msg.getData();
                    float relmem = bundle.getFloat(RELEASE_MEMORY);
                    if (Build.VERSION.SDK_INT >= 11) {
                        sendNotificationForICS(relmem);
                    } else {
                        sendNotification(relmem);
                    }
                }
            }
        };
        mAutoKillThread = new ScheduleTaskThread();
        mAutoKillThread.start();
        TMLog.d(TAG, "Service start");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        TMLog.d(TAG, "Service stopped");
        if (mHandler != null) {
            mHandler = null;
        }
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
        TMLog.begin(TAG);
        int count = mAppListAll.size();
        for (int i = 0; i < count; i++) {
            ProcessInfo process = mAppListAll.get(i);
            if (!process.isService()) {
                process.killSelf(this);
                mAppListAll.remove(i);
                TMLog.d(TAG, "Kill Process:" + process.getProcessName());
                i--;
                count--;
            }
        }
//        if (getPercentAgeOfAvailMemory() < MEMORY_LEVEL) {
//            count = mAppListAll.size();
//            for (int i = 0; i < count; i++) {
//                ProcessInfo process = mAppListAll.get(i);
//                process.killSelf(this);
//                TMLog.d(TAG, "Kill Process:" + process.getProcessName());
//                mAppListAll.remove(i);
//                i--;
//                count--;
//            }
//        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(11)
    private void sendNotificationForICS(float releasMemory) {

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        /* clear earlier notification */
        notificationManager.cancel(NOTIFICATION_TAG, NOTIFICATION_ID);

        Intent intent = new Intent(this, TaskmanagerActivity.class);
        PendingIntent mPendingIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= 11) {
            /** Create the notification */
            Notification.Builder builder = new Notification.Builder(this);
            builder.setWhen(System.currentTimeMillis())
                    .setContentIntent(mPendingIntent).setAutoCancel(true);
            String contextText = null;

            contextText = String.format(
                    this.getString(R.string.string_nofitication_content),
                    String.format("%.2f", releasMemory));

            builder.setContentText(contextText).setContentTitle(
                    this.getString(R.string.string_auto_kill_complete));

            builder.setSmallIcon(R.drawable.ic_launcher).setTicker(contextText);

            notificationManager.notify(NOTIFICATION_TAG, NOTIFICATION_ID,
                    builder.getNotification());
        }
    }

    @SuppressWarnings("deprecation")
    private void sendNotification(float releasMemory) {
        String contextText = null;
        contextText = String.format(
                this.getString(R.string.string_nofitication_content),
                String.format("%.2f", releasMemory));

        Notification notification = new Notification(R.drawable.ic_launcher,
                contextText, System.currentTimeMillis());
        notification.defaults = Notification.DEFAULT_ALL;
        PendingIntent pt = PendingIntent.getActivity(this, 0, new Intent(this,
                TaskmanagerActivity.class), 0);
        notification.setLatestEventInfo(this,
                this.getResources().getString(R.string.app_name), contextText,
                pt);
        NotificationManager notificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_TAG, NOTIFICATION_ID,
                notification);
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
                        if (Utils.isEnableNotification(TaskManagerService.this)
                                && release > 0) {
                            Message msg = new Message();
                            msg.what = RELEASE_COMPLETE;
                            Bundle bundle = new Bundle();
                            bundle.putFloat(RELEASE_MEMORY, release);
                            msg.setData(bundle);
                            if (mHandler != null) {
                                mHandler.sendMessage(msg);
                            }
                        }
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
