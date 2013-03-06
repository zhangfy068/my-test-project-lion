/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * TaskManaerApplication.java
 */
package com.newtech.taskmanager;

import android.app.Application;

public class TaskManagerApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
//		if (Utils.isAutoKill(this.getApplicationContext())) {
//			Intent intent = new Intent(this, TaskManagerService.class);
//			startService(intent);
//		}
	}
}
