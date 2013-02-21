/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * IgnorelistActivity.java
 */

package com.newtech.taskmanager;

import android.net.Uri;

import com.newtech.taskmanager.util.Constants;

public class IgnoreListActivity extends CheckedListActivity {

	@Override
	protected Uri getUri() {
		return Constants.IGNORE_LIST_URI;
	}
}
