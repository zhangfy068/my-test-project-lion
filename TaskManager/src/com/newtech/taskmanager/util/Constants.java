/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * Constants.java
 */

package com.newtech.taskmanager.util;

import android.net.Uri;

import com.newtech.taskmanager.provider.TaskmanagerContentProvider;
import com.newtech.taskmanager.provider.TaskmanagerDatabaseHelper;
import com.newtech.taskmanager.provider.TaskmanagerDatabaseHelper.TableColumns;

public class Constants {

	// These constants are used for settings/share preference
	public static final String SETTINGS_AUTO_RUN = "auto_run";
	public static final String SETTINGS_AUTO_KILL = "auto_kill";
	public static final String SETTINGS_SHOW_SYSTEM_PROCESS = "show_system_process";
	public static final String SETTINGS_SWIPE_ENABLE = "swipe_kill_enable";

	public static final int USER_PROCESS_ID = 10000;

	// for ContentProvider
	public static final String PACKAGE_NAME = TableColumns.PACKAGE_NAME;
	public static final String _ID = TableColumns._ID;
	/** The Uri for ignore list */
	public static final Uri IGNORE_LIST_URI = Uri.withAppendedPath(
			TaskmanagerContentProvider.CONTENT_URI,
			TaskmanagerDatabaseHelper.Tables.IGNORELIST);

	/** The Uri for auto list */
	public static final Uri AUTO_LIST_URI = Uri.withAppendedPath(
			TaskmanagerContentProvider.CONTENT_URI,
			TaskmanagerDatabaseHelper.Tables.AUTOLIST);

}
