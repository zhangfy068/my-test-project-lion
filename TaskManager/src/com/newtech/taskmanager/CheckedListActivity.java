/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * CheckedListActivity.java
 */
package com.newtech.taskmanager;

import java.util.ArrayList;

import com.newtech.taskmanager.util.Constants;
import com.newtech.taskmanager.util.Utils;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public abstract class CheckedListActivity extends ListActivity implements
		OnClickListener {
	/** all marked flag. */
	private boolean mIsAllUncheckMode = false;
	/** The adapter of Listview */
	private MulttipleChoiceListAdapter mAdapter;

	private SwipeDismissListViewTouchListener mTouchListener;

	private ContentResolver mContentResolver;
	private Button mAllButton;
	private Button mDoneButton;
	private PackageManager mPm;

	private Uri mUri;
	@Override
	protected void onCreate(final Bundle aSavedInstanceState) {
		super.onCreate(aSavedInstanceState);
		setContentView(R.layout.ignorelist);
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mPm = getApplicationContext().getPackageManager();
		/** user null cursor before cursor loader completed */
		mAdapter = new MulttipleChoiceListAdapter(this, null);
		setListAdapter(mAdapter);
		mContentResolver = this.getContentResolver();
		mAllButton = (Button) findViewById(R.id.mark_all_button);
		mDoneButton = (Button) findViewById(R.id.delete_button);
		
		mAllButton.setOnClickListener(this);
		mDoneButton.setOnClickListener(this);
		mUri = getUri();
		mTouchListener = new SwipeDismissListViewTouchListener(getListView(),
				new SwipeDismissListViewTouchListener.OnDismissCallback() {
					@Override
					public void onDismiss(ListView listView,
							int[] reverseSortedPositions) {
						for (int position : reverseSortedPositions) {
							final int pos = position;
							if (mAdapter != null) {
								new Thread(new Runnable() {
									@Override
									public void run() {
										deleteOneItem(mAdapter.getItemId(pos));
										CheckedListActivity.this.runOnUiThread(new Runnable() {
											@Override
											public void run() {
												updateButtonState();
												setEmptyText();
											}
											
										});
									}
									}
								).run();
							}
						}
					}
				});
		
		LoadDataTask task = new LoadDataTask();
		task.execute();
	}

	@Override
	protected void onResume() {
		super.onResume();
//		if(Utils.isSupportSwipe(getApplicationContext())) {
//			getListView().setOnTouchListener(mTouchListener);
//			getListView().setOnScrollListener(mTouchListener.makeScrollListener());
//		} else {
//			getListView().setOnTouchListener(null);
//			getListView().setOnScrollListener(null);
//		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/**
	 * Implements OnItemClickListener.onItemClick().
	 * 
	 * @param aList
	 *            list view
	 * @param aView
	 *            view
	 * @param aPosition
	 *            position
	 * @param aId
	 *            id
	 */
	@Override
	protected final void onListItemClick(final ListView aList,
			final View aView, final int aPosition, final long aId) {
		updateButtonState();
	}

	@Override
	public void onClick(View aView) {
		switch (aView.getId()) {
		case R.id.mark_all_button:
			toggleCheck();
			break;
		case R.id.delete_button:
			DeleteItemsTask task = new DeleteItemsTask();
			task.execute();
			break;
		}
	}

	private void updateButtonState() {
		int checkedCount = 0;
		SparseBooleanArray checks = getListView().getCheckedItemPositions();
		if (checks != null) {
			int count = checks.size();
			for (int i = 0; i < count; i++) {
				int key = checks.keyAt(i);
				long id = getListAdapter().getItemId(key);
				if (checks.get(key) && id > 0) {
					checkedCount++;
				}
			}
		}

		int itemCount = getListAdapter().getCount();

		final boolean allBtnEnable = itemCount > 0 && itemCount > 0;
		mAllButton.setEnabled(allBtnEnable);
		mAllButton.setFocusable(allBtnEnable);

		final boolean doneBtnEnable = checkedCount > 0 && itemCount > 0;
		mDoneButton.setEnabled(doneBtnEnable);
		mDoneButton.setFocusable(doneBtnEnable);

		if (itemCount > 0 && checkedCount == itemCount) {
			mIsAllUncheckMode = true;
			mAllButton.setText(R.string.strings_unmark_all_txt);
		} else {
			mIsAllUncheckMode = false;
			mAllButton.setText(R.string.strings_mark_all_txt);
		}
	}

	/**
	 * Returns the number of checked contacts.
	 */
	protected int getCheckedCount() {
		int checkedCount = 0;
		SparseBooleanArray checks = getListView().getCheckedItemPositions();
		if (checks != null) {
			int count = checks.size();
			for (int i = 0; i < count; i++) {
				int key = checks.keyAt(i);
				long id = getListAdapter().getItemId(key);
				if (checks.get(key) && id > 0) {
					checkedCount++;
				}
			}
		}
		return checkedCount;
	}
	
	protected Uri getUri()	{
		return null;
	}
	private void deleteOneItem(long id) {
		try {
			mContentResolver.delete(mUri, Constants._ID
					+ "=" + id, null);
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
	}

    /**
     * get checked ids.
     *
     * @return array of checked ids.
     */
    private final ArrayList<Long> getCheckedIds() {
        ArrayList<Long> ids = new ArrayList<Long>();
        SparseBooleanArray checks = getListView().getCheckedItemPositions();
        if (checks == null) {
            return ids;
        }
        for (int i = 0, count = checks.size(); i < count; i++) {
            int key = checks.keyAt(i);
            long id = getListAdapter().getItemId(key);
            if (checks.get(key) && id > 0) {
                ids.add(id);
            }
        }
        return ids;
    }

	/**
	 * toggle item check / uncheck.
	 */
	private final void toggleCheck() {
		if (mIsAllUncheckMode) {
			mAllButton.setText(R.string.strings_mark_all_txt);
			allCheck(false);
			mIsAllUncheckMode = false;
		} else {
			mAllButton.setText(R.string.strings_unmark_all_txt);
			allCheck(true);
			mIsAllUncheckMode = true;
		}
	}

	/**
	 * check / uncheck all items on the list.
	 * 
	 * @param aCheck
	 *            check or uncheck
	 */
	private final void allCheck(final boolean aCheck) {
		ListView list = getListView();
		list.clearChoices();
		final int count = list.getCount();
		for (int i = 0; i < count; i++) {
			list.setItemChecked(i, aCheck);
		}
		updateButtonState();
	}

	private void setEmptyText() {
		TextView emptyView = (TextView) findViewById(R.id.emptyText);
		if (getListAdapter().getCount() > 0) {
			emptyView.setVisibility(View.GONE);
		} else {
			emptyView.setVisibility(View.VISIBLE);
			emptyView
					.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
		}
	}

	protected static final class ViewHolder {
		private TextView mNameView;

		/** The widget displays icon. */
		private ImageView mIconView;

		/** Constructor. */
		protected ViewHolder() {
		}
	}

	class MulttipleChoiceListAdapter extends CursorAdapter {
		private Context mContext;

		@SuppressWarnings("deprecation")
		public MulttipleChoiceListAdapter(Context context, Cursor c) {
			super(context, c);
			mContext = context;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			String name = cursor.getString(cursor
					.getColumnIndex(Constants.PACKAGE_NAME));
			ViewHolder holder = (ViewHolder) view.getTag();
			try {
				ApplicationInfo info = mPm.getApplicationInfo(name,
						PackageManager.GET_ACTIVITIES);
				holder.mIconView.setImageDrawable(info.loadIcon(mPm));
				holder.mNameView.setText(info.loadLabel(mPm).toString());
			} catch (NameNotFoundException e) {
			}
			holder.mNameView.setVisibility(View.GONE);
			holder.mNameView.setVisibility(View.VISIBLE);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			ChecklistItem itemView = ChecklistItem.create(mContext, inflater,
					R.layout.checklist_double_item, parent);
			CheckedTextView check = (CheckedTextView) itemView
					.findViewById(R.id.checklist_check);
			itemView.setCheckable(check);
			ViewHolder cache = new ViewHolder();
			cache.mNameView = (TextView) itemView
					.findViewById(R.id.process_name);
			cache.mIconView = (ImageView) itemView
					.findViewById(R.id.head_image);
			itemView.setTag(cache);
			return itemView;
		}

		@Override
		public void changeCursor(final Cursor aCursor) {
		}
	}

	class LoadDataTask extends AsyncTask<Void, Void, Cursor> {
		@Override
		protected Cursor doInBackground(Void... params) {
			try {
				return mContentResolver.query(mUri, Utils.getIgnoreProject(),
						null, null, null);
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			return null;
		}

        @Override
        protected void onPostExecute(Cursor aCursor) {
            if (aCursor != null) {
                if (Build.VERSION.SDK_INT >= 11) {
                    mAdapter.swapCursor(aCursor);
                } else {
                    mAdapter = new MulttipleChoiceListAdapter(
                            CheckedListActivity.this, aCursor);
                    setListAdapter(mAdapter);
                }
                updateButtonState();
                setEmptyText();
            }
        }
    }

	class DeleteItemsTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			ArrayList<Long> ids = getCheckedIds();
			for (Long id : ids) {
				deleteOneItem(id);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			updateButtonState();
			setEmptyText();
		}
	}
}
