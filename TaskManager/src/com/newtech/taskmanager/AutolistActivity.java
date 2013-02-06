/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * AutolistActivity.java
 */

package com.newtech.taskmanager;

import android.net.Uri;

import com.newtech.taskmanager.util.Constants;
 
public class AutolistActivity extends CheckedListActivity {

	@Override
	protected Uri getUri() {
		return Constants.AUTO_LIST_URI;
	}
}
