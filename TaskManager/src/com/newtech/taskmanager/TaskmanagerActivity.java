/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * TaskmanagerActivity.java
 */

package com.newtech.taskmanager;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.accessibility.AccessibilityEvent;
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
	private static final String SCHEME = "package";
	private static final int CONTEXT_MENU_KILL = 0;

	private static final int CONTEXT_MENU_SWICHTO = 1;

	private static final int CONTEXT_MENU_IGNORE = 2;

	private static final int CONTEXT_MENU_AUTOKILL = 3;

    private static final int CONTEXT_MENU_DETAIL = 4;

	private AppItemAdapter mAdapter;

	private int mMaxMemory = 0;

	private View mHeadView;

	private ListView mListView;

	private ArrayAdapter<?> mSpinnerAdapter;

	private List<ProcessInfo> mAppList;

	//two list for withProcess and all process. These two list share object
	private ArrayList<ProcessInfo> mNormalProcess;
//	private List<ProcessInfo> mAppListAll;
	//ServiceProcess includes all services and foregrould/visible process
	private ArrayList<ProcessInfo> mServiceProcess;
    //All System Process
	private ArrayList<ProcessInfo> mSystemProcess;
	private Spinner mSpinner;

	private Button mRefreshButton;

	private Button mKillAllButton;

	private ActivityManager mAm;

	private PackageManager mPm;

	private SwipeDismissListViewTouchListener mTouchListener;

	private LinearColorBar mColorBar;

	private View mProcessView;
	private TextView mEmptyView;
	private float mTotalMemory;
	private float mAvailMemory;

	private TextView mUsedMemoryTextView;
	private TextView mAvailMemTextView;

	private boolean mOnRefresh;

	private RunningProcessStatus mRunningStatus;

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

        if (Constants.API_LEVEL > 12) {
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
        }
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

        mKillAllButton = (Button) this.findViewById(R.id.kill_all);
        mKillAllButton.setOnClickListener(this);

		mSpinnerAdapter = ArrayAdapter.createFromResource(this,
				R.array.snipper_filter_arrays,
				android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(mSpinnerAdapter);
	    mEmptyView= (TextView)findViewById(R.id.emptyText);
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

        if (Utils.isSupportSwipe(this.getApplicationContext())
                && Constants.API_LEVEL > 12) {
            mListView.setOnTouchListener(mTouchListener);
            mListView.setOnScrollListener(mTouchListener.makeScrollListener());
        } else {
            mListView.setOnTouchListener(null);
            mListView.setOnScrollListener(null);
        }

        if (mAdapter != null) {
            // && mAppListAll != null) {
            // if (Utils.isShowSystemProcess(this.getApplicationContext())) {
            // mAppList = mAppListAll;
            // } else {
            // mAppList = mNormalProcess;
            // }
            // if (mAppList.size() > 0) {
            // mMaxMemory = mAppList.get(0).getMemory();
            // }
            filterAppList();
            mAdapter.notifyDataSetChanged();
            updateEmtpyView();
        }
        // }
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
		if( aView.hasFocus() ) {
		    TMLog.d(TAG, "aView hasFocus.");
		}
		if(mAppList.size() == 0) {
		    return;
		}

        ProcessInfo processInfo = mAppList.get(info.position);
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View menuHeader = inflater.inflate(R.layout.contextmenu_header, null);
        ImageView icon = (ImageView) menuHeader.findViewById(R.id.contextmenu_icon);
        TextView title = (TextView) menuHeader.findViewById(R.id.contextmenu_title);
        icon.setImageDrawable(processInfo.getIcon(mPm));
        title.setText(processInfo.getName(mPm));
        aMenu.setHeaderView(menuHeader);
        //aMenu.setHeaderIcon(processInfo.getIcon(mPm));
        //aMenu.setHeaderTitle(processInfo.getName(mPm));
        aMenu.add(Menu.NONE, CONTEXT_MENU_KILL, 0,
                R.string.string_context_menu_kill_txt);
        if (processInfo.getIntent() != null) {
            aMenu.add(Menu.NONE, CONTEXT_MENU_SWICHTO, 0,
                    R.string.string_context_menu_switchto);
        }
        aMenu.add(Menu.NONE, CONTEXT_MENU_IGNORE, 0,
                R.string.string_context_menu_ingore);
        aMenu.add(Menu.NONE, CONTEXT_MENU_AUTOKILL, 0,
                R.string.string_context_menu_autokill);
        aMenu.add(Menu.NONE, CONTEXT_MENU_DETAIL, 0,
                R.string.string_context_menu_detail);
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
			break;
		case CONTEXT_MENU_AUTOKILL:
			addAutoProcess(pos);
			break;
		case CONTEXT_MENU_DETAIL:
		    showDetail(pos);
		    break;
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
		case R.id.show_ignorelist:
			Intent intentIgnore = new Intent(Intent.ACTION_VIEW);
			intentIgnore.setClass(this, IgnoreListActivity.class);
			startActivity(intentIgnore);
			break;
		case R.id.show_autolist:
			Intent intentAuto = new Intent(Intent.ACTION_VIEW);
			intentAuto.setClass(this, AutolistActivity.class);
			startActivity(intentAuto);
			break;
		default:
			break;
		}
		return true;
	}

    private void updateEmtpyView() {
        if (mAppList != null ) {
            if(mAppList.size() == 0) {
                mEmptyView.setVisibility(View.VISIBLE);
            }else {
                mEmptyView.setVisibility(View.GONE);
                mEmptyView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
            }
        }
    }

	private void killProcess(int position) {
		if (mAppList != null) {
			ProcessInfo info = mAppList.get(position);
			mAppList.remove(position);
			removeProcessFromList(info);
			TMLog.d(TAG, info.getProcessName());
			info.killSelf(this);
            if (mAppList.size() == 0) {
                updateEmtpyView();
                return;
            }
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
			String name = mAppList.get(pos).getProcessName();
			ContentValues values = new ContentValues();
			values.put(Constants.PACKAGE_NAME, name);
			try {
				mContentResolver.insert(Constants.IGNORE_LIST_URI, values);
				removeProcessFromList(mAppList.get(pos));
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
		}
	}

	private void addAutoProcess(int pos) {
		if (mAppList != null) {
			String name = mAppList.get(pos).getProcessName();
			ContentValues values = new ContentValues();
			values.put(Constants.PACKAGE_NAME, name);
			try {
				mContentResolver.insert(Constants.AUTO_LIST_URI, values);
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
		}
	}

	private void showDetail(int pos) {
	    if (mAppList != null) {
            ProcessInfo info = mAppList.get(pos);
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts(SCHEME, info.getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
	    }
	}

	private void killAllUserProcess() {
	    TMLog.begin(TAG);
	    if (mAppList != null) {
	        int count = mAppList.size();
	        for(int i = 0;i < count;i++) {
                if(count == 0) {
                    break;
                }
	            ProcessInfo info = mAppList.get(i);
	            if(!info.isSystemProcess()) {
	                killProcess(i);
	                i--;
	                count--;
	            }
	        }
	    }
	    TMLog.end(TAG);
	}

    // Could not find the better way to do this.
    private void removeProcessFromList(ProcessInfo aInfo) {
        String name = aInfo.getPackageName();
        if (aInfo.isSystemProcess()) {
            removeListWithName(name, mSystemProcess);
        } else if(aInfo.isService()) {
            removeListWithName(name, mServiceProcess);
        } else {
            removeListWithName(name, mNormalProcess);
        }
//		for(ProcessInfo info : mAppListAll) {
//			if(TextUtils.equals(info.getProcessName(), processName)){
//				mAppListAll.remove(info);
//				break;
//			}
//		}
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

    private void removeListWithName(String name, ArrayList<ProcessInfo> list) {
        for (ProcessInfo info : list) {
            if (TextUtils.equals(info.getProcessName(), name)) {
                list.remove(info);
                break;
            }
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
            boolean isSystem = appInfo.isSystemProcess();
            if (isSystem) {
                holder.mTitle.setTextColor(Color.BLUE);
            } else if (appInfo.getImportance()
                    <= RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                holder.mTitle.setTextColor(Color.GREEN);
            } else {
                holder.mTitle.setTextColor(Color.BLACK);
            }
			convertView.setTag(R.id.app_summary,
					Boolean.valueOf(isSystem));
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
		case R.id.kill_all:
		    if(!mOnRefresh) {
		        killAllUserProcess();
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

    private void filterAppList() {
        if (mNormalProcess == null || mSystemProcess == null
                || mServiceProcess == null) {
            return;
        }
        mAppList = new ArrayList<ProcessInfo>();
        if (Utils.isShowSystemProcess(TaskmanagerActivity.this)
                && Utils.isShowService(TaskmanagerActivity.this)) {
            mAppList.addAll(mNormalProcess);
            mAppList.addAll(mSystemProcess);
            mAppList.addAll(mServiceProcess);
        } else if (Utils.isShowSystemProcess(TaskmanagerActivity.this)) {
            mAppList.addAll(mNormalProcess);
            mAppList.addAll(mSystemProcess);
        } else if (Utils.isShowService(TaskmanagerActivity.this)) {
            mAppList.addAll(mNormalProcess);
            mAppList.addAll(mServiceProcess);
        } else {
            mAppList.addAll(mNormalProcess);
        }
        if (mAppList.size() != 0) {
            sortList(mAppList);
            mMaxMemory = mAppList.get(0).getMemory();
        }
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
            mNormalProcess = new ArrayList<ProcessInfo>();
            mServiceProcess = new ArrayList<ProcessInfo>();
            mSystemProcess = new ArrayList<ProcessInfo>();
            for (ProcessInfo info : list) {
                if (info.isSystemProcess()) {
                    mSystemProcess.add(info);
                } else if (info.isService()) {
                    mServiceProcess.add(info);
                } else {
                    mNormalProcess.add(info);
                }
            }
            filterAppList();
            return list;
        }

		@Override
		protected void onPostExecute(List<ProcessInfo> list) {
//			mAppListAll = list;
//		    mAppList = new ArrayList<ProcessInfo>();
//            if (Utils.isShowSystemProcess(TaskmanagerActivity.this)
//                    && Utils.isShowService(TaskmanagerActivity.this)) {
//                mAppList.addAll(mNormalProcess);
//                mAppList.addAll(mSystemProcess);
//                mAppList.addAll(mServiceProcess);
//            } else if (Utils.isShowSystemProcess(TaskmanagerActivity.this)) {
//                mAppList.addAll(mNormalProcess);
//                mAppList.addAll(mSystemProcess);
//            } else if (Utils.isShowService(TaskmanagerActivity.this)) {
//                mAppList.addAll(mNormalProcess);
//                mAppList.addAll(mServiceProcess);
//            } else {
//                mAppList.addAll(mNormalProcess);
//            }
//            sortList(mAppList);
//			mMaxMemory = mAppList.get(0).getMemory();
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
			mKillAllButton.setEnabled(true);
			Toast.makeText(TaskmanagerActivity.this,
					R.string.string_refresh_complete_txt, Toast.LENGTH_SHORT)
					.show();
            updateEmtpyView();
		}

		protected void onPreExecute() {
			super.onPreExecute();
			mRefreshButton.setEnabled(false);
			mKillAllButton.setEnabled(false);
			Toast.makeText(TaskmanagerActivity.this,
					R.string.string_start_refresh_txt, Toast.LENGTH_SHORT)
					.show();
		}
	}
}
