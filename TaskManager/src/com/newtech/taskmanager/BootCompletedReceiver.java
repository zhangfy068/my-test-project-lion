/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * BootCompletedReceiver.java
 */

package com.newtech.taskmanager;

import com.newtech.taskmanager.util.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent aIntent) {
		if (Utils.isAutoKill(context.getApplicationContext())
				&& Utils.isAutoRun(context.getApplicationContext())) {
			Intent intent = new Intent(context, TaskManagerService.class);
			context.startService(intent);
		}
	}
}
