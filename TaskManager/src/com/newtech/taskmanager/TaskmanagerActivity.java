/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * TaskmanagerActivity.java
 */

package com.newtech.taskmanager;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug.MemoryInfo;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.util.Log;
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
import java.util.Comparator;
import java.util.List;

public class TaskmanagerActivity extends ListActivity implements
		View.OnClickListener, OnItemClickListener,
		View.OnCreateContextMenuListener {
	private static final String TAG = "TaskmanagerActivity";
	
	private static final int CONTEXT_MENU_KILL = 0;

	private static final int CONTEXT_MENU_SWICHTO = 1;

	private static final int USER_PROCESS_ID = 10000;

	private AppItemAdapter mAdapter;

	private int mMaxMemory = 0;

	private View mHeadView;

	private ProgressDialog mProgressDlg;

	private ListView mListView;

	private ArrayAdapter mSpinnerAdapter;

	private List<ProcessInfo> mAppList;

	private Spinner mSpinner;

	private Button mRefreshButton;

	private ActivityManager mAm;

	private PackageManager mPm;
	
	private SwipeDismissListViewTouchListener mTouchListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_manager);
		mListView = getListView();
		mListView.setOnItemClickListener(this);

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
		mTouchListener.setAllowTag(R.id.app_summary);

		mListView.setOnTouchListener(mTouchListener);
		mListView.setOnScrollListener(mTouchListener.makeScrollListener());
		// LayoutInflater inflater = (LayoutInflater) this
		// .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		mAm = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

		mPm = getApplicationContext().getPackageManager();
		mHeadView = this.findViewById(R.id.header_view);
		// mHeadView = inflater.inflate(R.layout.header_view, null);
		// mListView.addHeaderView(mHeadView);
		mSpinner = (Spinner) this.findViewById(R.id.spinner_filter);
		mRefreshButton = (Button) this.findViewById(R.id.refresh_btn);

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
		if (mProgressDlg != null) {
			mProgressDlg.dismiss();
			mProgressDlg = null;
		}
		if (mAdapter != null) {
			mAdapter = null;
		}
		super.onDestroy();
	}

	private void initProcess() {
		LoadProcossTask task = new LoadProcossTask();
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
		aMenu.setHeaderIcon(processInfo.icon);
		aMenu.setHeaderTitle(processInfo.name);
		aMenu.add(Menu.NONE, CONTEXT_MENU_KILL, 0,
				R.string.string_context_menu_kill_txt);
		MenuItem switchMenu = aMenu.add(Menu.NONE, CONTEXT_MENU_SWICHTO, 0,
				R.string.string_context_menu_switchto);

		Intent intent = mPm.getLaunchIntentForPackage(processInfo.packagename);
		if (intent == null) {
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
			return true;
		case CONTEXT_MENU_SWICHTO:
			switchToProcess(pos);
			return true;
		default:
			break;
		}
		return true;
	}
	
	private void killProcess(int position) {
		if (mAppList != null) {
			ProcessInfo processInfo = mAppList.get(position);
			mAm.killBackgroundProcesses(processInfo.packagename);
			mAppList.remove(position);
			sortList(mAppList);
			if(mAdapter != null) {
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	private void switchToProcess(int position) {
		if (mAppList != null) {
			Intent intent = mPm.getLaunchIntentForPackage(mAppList
					.get(position).packagename);
			if (intent != null) {
				startActivity(intent);
			}
		}
	}

	public List<ProcessInfo> getRunningProcess() {
		List<RunningAppProcessInfo> run = mAm.getRunningAppProcesses();
		PackageManager pm = this.getPackageManager();
		List<ProcessInfo> list = new ArrayList<ProcessInfo>();
		for (RunningAppProcessInfo runningInfo : run) {
			if (runningInfo.processName.equals("system")
					|| runningInfo.processName.equals("com.android.phone")) {
				continue;
			}
			try {
				String packageName = runningInfo.processName;
				ProcessInfo processInfo = new ProcessInfo();
				PackageInfo packInfo = mPm.getPackageInfo(packageName,
						PackageManager.GET_ACTIVITIES);
				ApplicationInfo appInfo = mPm.getApplicationInfo(packageName,
						PackageManager.GET_ACTIVITIES);

				if (packInfo != null) {
					int pid = runningInfo.pid;
					Drawable draw = mPm.getApplicationIcon(packageName);
					if (draw != null) {
						processInfo.icon = draw;
						processInfo.name = appInfo.loadLabel(pm).toString();
						processInfo.Uid = runningInfo.uid;
						processInfo.importance = runningInfo.importance;
						processInfo.pid = pid;

						//Poor performance here.
						processInfo.packagename = runningInfo.processName;
						MemoryInfo[] meminfo = mAm
								.getProcessMemoryInfo(new int[] { pid });
						MemoryInfo pInfo = meminfo[0];
						processInfo.memory = pInfo.getTotalPss();
						Log.i(TAG, appInfo.loadLabel(pm).toString() + " UID: "
								+ processInfo.Uid + "  Importance: "
								+ processInfo.importance + " Process name: "
								+ runningInfo.processName);
						// if (processInfo.Uid > USER_PROCESS_ID
						// && processInfo.importance
						// > RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE) {
						list.add(processInfo);
					}
				}
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	class AppItemAdapter extends BaseAdapter {

		private Context mContext;

		public AppItemAdapter(List<ProcessInfo> list, Context context) {
			mAppList = list;
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
			holder.mIcon.setImageDrawable(appInfo.icon);
			holder.mTitle.setText(appInfo.name);
			holder.mSummary.setText(String.format("%.2f MB",
					(float) appInfo.memory / 1024));
			int mem = appInfo.memory;
			int pos = mem * 100 / mMaxMemory;
			holder.mProgressBar.setProgress(pos);
			Boolean isSystem = false;
			if(appInfo.importance < RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE
					|| appInfo.Uid < USER_PROCESS_ID) {
				isSystem = true;
			}
			convertView.setTag(R.id.app_summary, isSystem);
			return convertView;
		}

	}

	@SuppressWarnings("unchecked")
	private void sortList(List<ProcessInfo> list) {
		Collections.sort(list, new ProcessInfo());
		mMaxMemory = list.get(0).memory;
	}

	static private class ViewHolder {
		ImageView mIcon;

		TextView mTitle;

		TextView mSummary;

		ProgressBar mProgressBar;
	}

//	public class PackagesInfo {
//		private List<ApplicationInfo> appList;
//
//		public PackagesInfo(Context context) {
//			appList = mPm
//					.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
//		}
//
//		public ApplicationInfo getInfo(String name) {
//			if (name == null) {
//				return null;
//			}
//			for (ApplicationInfo appinfo : appList) {
//				if (name.equals(appinfo.processName)) {
//					return appinfo;
//				}
//			}
//			return null;
//		}
//
//	}

	public static class ProcessInfo implements Comparator {
		private int Uid;

		private int pid;

		private Drawable icon;

		private String name;

		private String packagename;

		private int memory;

		private int importance;

		@Override
		public int compare(Object arg0, Object arg1) {
			if (arg0 instanceof ProcessInfo && arg1 instanceof ProcessInfo) {
				ProcessInfo info0 = (ProcessInfo) arg0;
				ProcessInfo info1 = (ProcessInfo) arg1;
				if (info0.memory > info1.memory) {
					return -1;
				} else if (info0.memory < info1.memory) {
					return 1;
				}
			}
			return 0;
		}
	}

	public class LoadProcossTask extends
			AsyncTask<Void, Void, List<ProcessInfo>> {

		@SuppressWarnings("unchecked")
		@Override
		protected List<ProcessInfo> doInBackground(Void... params) {
			List<ProcessInfo> list = getRunningProcess();
			sortList(list);
			return list;
		}

		@Override
		protected void onPostExecute(List<ProcessInfo> list) {
			mAdapter = new AppItemAdapter(list, TaskmanagerActivity.this);
			setListAdapter(mAdapter);
			mHeadView.setVisibility(View.VISIBLE);
			if (mProgressDlg != null) {
				mProgressDlg.dismiss();
			}
		}

		protected void onPreExecute() {
			mProgressDlg = new ProgressDialog(TaskmanagerActivity.this);
			mProgressDlg.setMessage(getResources().getString(
					R.string.refreshing_dialog_message));
			mProgressDlg.setTitle(R.string.refreshing_dialog_title);
			mProgressDlg.show();
		}
	}

	public class RefreshTask extends AsyncTask<Void, Void, Void> {

		@SuppressWarnings("unchecked")
		@Override
		protected Void doInBackground(Void... params) {
			mAppList = getRunningProcess();
			sortList(mAppList);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (mAdapter != null) {
				mAdapter.notifyDataSetChanged();
			}
			mRefreshButton.setEnabled(true);
			Toast.makeText(TaskmanagerActivity.this,
					R.string.string_refresh_complete_txt, Toast.LENGTH_SHORT)
					.show();
		}

		protected void onPreExecute() {
			mRefreshButton.setEnabled(false);
			Toast.makeText(TaskmanagerActivity.this,
					R.string.string_start_refresh_txt, Toast.LENGTH_SHORT)
					.show();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.refresh_btn:
			RefreshTask task = new RefreshTask();
			task.execute();
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
}
