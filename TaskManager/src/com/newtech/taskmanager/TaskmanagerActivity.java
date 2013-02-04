/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * TaskmanagerActivity.java
 */

package com.newtech.taskmanager;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.ActivityManager;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.newtech.taskmanager.util.Constants;
import com.newtech.taskmanager.util.TMLog;
import com.newtech.taskmanager.util.Utils;

public class TaskmanagerActivity extends ListActivity implements
		View.OnClickListener, OnItemClickListener,
		View.OnCreateContextMenuListener {
	private static final String TAG = "TaskmanagerActivity";

	private static final int CONTEXT_MENU_KILL = 0;

	private static final int CONTEXT_MENU_SWICHTO = 1;

	private static final int CONTEXT_MENU_IGNORE = 2;

	private AppItemAdapter mAdapter;

	private int mMaxMemory = 0;

	private View mHeadView;

	private ListView mListView;

	private ArrayAdapter<?> mSpinnerAdapter;

	private List<ProcessInfo> mAppList;

	//two list for withProcess and all process. These two list share object
	private List<ProcessInfo> mAppListWithoutSystemProcess;
	private List<ProcessInfo> mAppListAll;

	private Spinner mSpinner;

	private Button mRefreshButton;

	private ActivityManager mAm;

	private PackageManager mPm;

	private SwipeDismissListViewTouchListener mTouchListener;

	private LinearColorBar mColorBar;

	private View mProcessView;

	private float mTotalMemory;
	private float mAvailMemory;

	private TextView mUsedMemoryTextView;
	private TextView mAvailMemTextView;

	private boolean mOnRefresh;

	private RunningProcessStatus mRunningStatus;

    private SharedPreferences mPrefs;

    private ContentResolver mContentResolver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_manager);
		mContentResolver = this.getContentResolver();
		mListView = getListView();
		mColorBar = (LinearColorBar) findViewById(R.id.color_bar);
		mUsedMemoryTextView = (TextView) findViewById(R.id.used_memory);
		mAvailMemTextView = (TextView) findViewById(R.id.avail_memory);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		mTouchListener = new SwipeDismissListViewTouchListener(mListView,
				new SwipeDismissListViewTouchListener.OnDismissCallback() {
					@Override
					public void onDismiss(ListView listView,
							int[] reverseSortedPositions) {
						for (int position : reverseSortedPositions) {
							killProcess(position);
						}
					}
				});
		mTouchListener.setKeyForTagofForbidSwip(R.id.app_summary);

		mListView.setOnItemClickListener(this);

		mAm = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

		mPm = getApplicationContext().getPackageManager();
		mHeadView = this.findViewById(R.id.header_view);
		// mHeadView = inflater.inflate(R.layout.header_view, null);
		// mListView.addHeaderView(mHeadView);
		mSpinner = (Spinner) this.findViewById(R.id.spinner_filter);
		mRefreshButton = (Button) this.findViewById(R.id.refresh_btn);
		mProcessView = this.findViewById(R.id.loading_process);
		mRefreshButton.setOnClickListener(this);
		mSpinnerAdapter = ArrayAdapter.createFromResource(this,
				R.array.snipper_filter_arrays,
				android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(mSpinnerAdapter);
		registerForContextMenu(getListView());
		initProcess();
	}

	@Override
	public void onDestroy() {
		if (mAdapter != null) {
			mAdapter = null;
		}
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
		// refresh process when resumed.
//		if (!mOnRefresh) {
//			RefreshTask task = new RefreshTask();
//			task.execute();
		// }
		if (mPrefs.getBoolean(Constants.SETTINGS_SWIPE_ENABLE, true)) {
			mListView.setOnTouchListener(mTouchListener);
			mListView.setOnScrollListener(mTouchListener.makeScrollListener());
		} else {
			mListView.setOnTouchListener(null);
			mListView.setOnScrollListener(null);
		}

		if (mAdapter != null && mAppListWithoutSystemProcess != null
				&& mAppListAll != null) {
			if (isShowSystemProcess()) {
				mAppList = mAppListAll;
			} else {
				mAppList = mAppListWithoutSystemProcess;
			}
			mMaxMemory = mAppList.get(0).getMemory();
			mAdapter.notifyDataSetChanged();
		}
	}

	private boolean isShowSystemProcess() {
		return mPrefs.getBoolean(Constants.SETTINGS_SHOW_SYSTEM_PROCESS, true);
	}
	private void initProcess() {
		mRunningStatus = new RunningProcessStatus(this.getApplicationContext());
		mTotalMemory = Utils.getTotalMemory();
		LoadProcossTask task = new FirstLoading();
		task.execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.task_manager, menu);
		return true;
	}

	@Override
	public void onCreateContextMenu(final ContextMenu aMenu, final View aView,
			final ContextMenuInfo aMenuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) aMenuInfo;
		} catch (ClassCastException e) {
			return;
		}
		ProcessInfo processInfo = mAppList.get(info.position);
		aMenu.setHeaderIcon(processInfo.getIcon(mPm));
		aMenu.setHeaderTitle(processInfo.getName(mPm));
		aMenu.add(Menu.NONE, CONTEXT_MENU_KILL, 0,
				R.string.string_context_menu_kill_txt);
		MenuItem switchMenu = aMenu.add(Menu.NONE, CONTEXT_MENU_SWICHTO, 0,
				R.string.string_context_menu_switchto);

		aMenu.add(Menu.NONE, CONTEXT_MENU_IGNORE, 0, R.string.string_context_menu_ingore);
		if (processInfo.getIntent() == null) {
			switchMenu.setEnabled(false);
		}
	}

	@Override
	public boolean onContextItemSelected(final MenuItem aItem) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) aItem.getMenuInfo();
		} catch (ClassCastException e) {
			return false;
		}

		int pos = info.position;

		switch (aItem.getItemId()) {
		case CONTEXT_MENU_KILL:
			killProcess(pos);
			break;
		case CONTEXT_MENU_SWICHTO:
			switchToProcess(pos);
		    break;
		case CONTEXT_MENU_IGNORE:
			ignoreProcess(pos);
			return true;
		default:
			break;
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClass(this, SettingsPreferenceActivity.class);
			startActivity(intent);
			break;

		}
		return true;
	}

	private void killProcess(int position) {
		if (mAppList != null) {
			ProcessInfo info = mAppList.get(position);
			removeProcessFromLis(info.getPackageName());
			info.killSelf(this);
			sortList(mAppList);
			mMaxMemory = mAppList.get(0).getMemory();
			mAvailMemory += (float) info.getMemory() / 1024;
			setMemBar();

			TMLog.d(TAG,
					"Kill process, release memory :"
							+ (float) (info.getMemory() / 1024) + "MB");
		}
	}

	private void ignoreProcess(int pos) {
		if (mAppList != null) {
			String name = mAppList.get(pos).getPackageName();
			ContentValues values = new ContentValues();
			values.put(Constants.PACKAGE_NAME, name);
			try {
				mContentResolver.insert(Constants.IGNORE_LIST_URI, values);
				removeProcessFromLis(name);
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
		}
	}

	//Could not find the better way to do this.
	private void removeProcessFromLis(String processName) {
		for(ProcessInfo info : mAppListWithoutSystemProcess) {
			if(TextUtils.equals(info.getPackageName(), processName)){
				mAppListWithoutSystemProcess.remove(info);
				break;
			}
		}

		for(ProcessInfo info : mAppListAll) {
			if(TextUtils.equals(info.getPackageName(), processName)){
				mAppListAll.remove(info);
				break;
			}
		}
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	private void switchToProcess(int position) {
		if (mAppList != null) {
			Intent intent = mAppList.get(position).getIntent();
			if (intent != null) {
				startActivity(intent);
			}
		}
	}

	//
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		TMLog.begin(TAG);
		super.onConfigurationChanged(newConfig);
		if (mAppList != null && mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
		;
		TMLog.end(TAG);
	}

	// public List<ProcessInfo> getRunningProcess() {
	// long startTime = System.currentTimeMillis();
	// List<RunningAppProcessInfo> run = mAm.getRunningAppProcesses();
	// PackageManager pm = this.getPackageManager();
	// List<ProcessInfo> list = new ArrayList<ProcessInfo>();
	// for (RunningAppProcessInfo runningInfo : run) {
	// if (runningInfo.processName.equals("system")
	// || runningInfo.processName.equals("com.android.phone")) {
	// continue;
	// }
	//
	// String packageName = runningInfo.processName;
	// ProcessInfo processInfo = new ProcessInfo(packageName);
	// PackagesInfo allUsedPackage = new PackagesInfo(this);
	// ApplicationInfo appInfo = allUsedPackage.getInfo(packageName);
	// int pid = runningInfo.pid;
	// if (appInfo != null) {
	// processInfo.setIcon(mPm.getApplicationIcon(appInfo));
	// processInfo.setName(appInfo.loadLabel(pm).toString());
	// processInfo.setUid(runningInfo.uid);
	// processInfo.setImportance(runningInfo.importance);
	// processInfo.setPid(pid);
	// processInfo.setSystemProcess((appInfo.flags &
	// ApplicationInfo.FLAG_SYSTEM) != 0);
	//
	// // Poor performance here.
	// MemoryInfo[] meminfo = mAm
	// .getProcessMemoryInfo(new int[] { pid });
	// MemoryInfo pInfo = meminfo[0];
	// processInfo.setMemoryInfo(pInfo);
	// processInfo.setMemory(pInfo.getTotalPss());
	// TMLog.d(TAG, appInfo.loadLabel(pm).toString() + " UID: "
	// + processInfo.getUid() + " PID " + processInfo.getPid()
	// + "  System Process: " + processInfo.isSystemProcess()
	// + " Process name: " + runningInfo.processName);
	// // Log.i(TAG, "Pss Memeory ");
	// // if (processInfo.Uid > USER_PROCESS_ID
	// // && processInfo.importance
	// // > RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE) {
	// list.add(processInfo);
	// }
	// }
	// List<RunningServiceInfo> listSerivce = mAm
	// .getRunningServices(Integer.MAX_VALUE);
	// for (RunningServiceInfo info : listSerivce) {
	// TMLog.d(TAG,
	// info.process
	// + " UID: "
	// + info.uid
	// + "  PID: "
	// + info.pid
	// + (info.flags == RunningServiceInfo.FLAG_SYSTEM_PROCESS ?
	// " >>>>SYSTEM PROCESS<<<<<<<"
	// : " Not System Process"));
	// }
	// long lastTime = System.currentTimeMillis();
	// TMLog.d(TAG, "last Time for getRunningProcess: " + (lastTime -
	// startTime));
	// return list;
	// }

	class AppItemAdapter extends BaseAdapter {

		private Context mContext;

		public AppItemAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return mAppList.size();
		}

		public Object getItem(int position) {
			return mAppList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				LayoutInflater inflater = LayoutInflater.from(mContext);

				holder = new ViewHolder();
				convertView = inflater.inflate(R.layout.app_percentage_item,
						null);
				holder.mIcon = (ImageView) convertView
						.findViewById(R.id.app_icon);
				holder.mProgressBar = (ProgressBar) convertView
						.findViewById(R.id.app_progress);
				holder.mTitle = (TextView) convertView
						.findViewById(R.id.app_title);
				holder.mSummary = (TextView) convertView
						.findViewById(R.id.app_summary);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			ProcessInfo appInfo = (ProcessInfo) mAppList.get(position);
			holder.mIcon.setImageDrawable(appInfo.getIcon(mPm));
			holder.mTitle.setText(appInfo.getName(mPm));
			holder.mSummary.setText(String.format("%.2f MB",
					(float) appInfo.getMemory() / 1024));
			int mem = appInfo.getMemory();
			int pos = mem * 100 / mMaxMemory;
			holder.mProgressBar.setProgress(pos);
			convertView.setTag(R.id.app_summary,
					Boolean.valueOf(appInfo.isSystemProcess()));
			return convertView;
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.refresh_btn:
			if (!mOnRefresh) {
				RefreshTask task = new RefreshTask();
				task.execute();
			}
			break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {
		// if (view != null) {
		// ProcessInfo info = mAppList.get(position - 1);
		// Intent intent = mPm.getLaunchIntentForPackage(info.packagename);
		// if (intent != null) {
		// startActivity(intent);
		// }
		// }
	}

	private void sortList(List<ProcessInfo> list) {
		Collections.sort(list, ProcessInfo.getComparator());
	}

	private void setMemBar() {
		float used = (mTotalMemory - mAvailMemory);

		mColorBar.setRatios(used / mTotalMemory, 0, mAvailMemory / mTotalMemory);

		String usedString = getResources().getString(
				R.string.string_memory_used_txt)
				+ String.format("%.2f MB", used);

		String availString = getResources().getString(
				R.string.string_memory_avail_txt)
				+ String.format("%.2f MB", mAvailMemory);
		mUsedMemoryTextView.setText(usedString);
		mAvailMemTextView.setText(availString);
	}

	static private class ViewHolder {
		ImageView mIcon;

		TextView mTitle;

		TextView mSummary;

		ProgressBar mProgressBar;
	}

	public abstract class LoadProcossTask extends
			AsyncTask<Void, Void, List<ProcessInfo>> {

		@Override
		protected List<ProcessInfo> doInBackground(Void... params) {
			List<ProcessInfo> list = mRunningStatus.getRunningApplication();
			sortList(list);
			mAppListWithoutSystemProcess = new ArrayList<ProcessInfo>();
			for (ProcessInfo info : list) {
				if (!info.isSystemProcess()) {
					mAppListWithoutSystemProcess.add(info);
				}
			}
			return list;
		}

		@Override
		protected void onPostExecute(List<ProcessInfo> list) {
			mAppListAll = list;
			if (isShowSystemProcess()) {
				mAppList = mAppListAll;
			}else {
				mAppList = mAppListWithoutSystemProcess;
			}
			mMaxMemory = mAppList.get(0).getMemory();
			mAvailMemory = Utils.getLastestFreeMemory(mAm);
			mOnRefresh = false;
			setMemBar();
		}

		protected void onPreExecute() {
			mOnRefresh = true;
		}
	}

	public class FirstLoading extends LoadProcossTask {
		@Override
		protected void onPostExecute(List<ProcessInfo> list) {
			super.onPostExecute(list);
			mAdapter = new AppItemAdapter(TaskmanagerActivity.this);
			mProcessView.setVisibility(View.GONE);
			setListAdapter(mAdapter);
			mHeadView.setVisibility(View.VISIBLE);
			mColorBar.setVisibility(View.VISIBLE);
		}
	}

	public class RefreshTask extends LoadProcossTask {

		@Override
		protected void onPostExecute(List<ProcessInfo> list) {
			super.onPostExecute(list);
			if (mAdapter != null) {
				mAdapter.notifyDataSetChanged();
			}
			mRefreshButton.setEnabled(true);
			Toast.makeText(TaskmanagerActivity.this,
					R.string.string_refresh_complete_txt, Toast.LENGTH_SHORT)
					.show();
		}

		protected void onPreExecute() {
			super.onPreExecute();
			mRefreshButton.setEnabled(false);
			Toast.makeText(TaskmanagerActivity.this,
					R.string.string_start_refresh_txt, Toast.LENGTH_SHORT)
					.show();
		}
	}
}
