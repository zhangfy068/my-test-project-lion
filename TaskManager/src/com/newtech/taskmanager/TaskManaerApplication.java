/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * TaskManaerApplication.java
 */
package com.newtech.taskmanager;

import com.newtech.taskmanager.util.Utils;

import android.app.Application;
import android.content.Intent;

public class TaskManaerApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		if (Utils.isAutoKill(this.getApplicationContext())) {
			Intent intent = new Intent(this, TaskManagerService.class);
			startService(intent);
		}
	}
}
