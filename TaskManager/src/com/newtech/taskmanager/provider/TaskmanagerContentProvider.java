/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * TaskmanagerContentProvider.java
 */
package com.newtech.taskmanager.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class TaskmanagerContentProvider extends ContentProvider {
    public static final String TAG = "TaskmanagerContentProvider";

    public final static String AUTHORITY = "com.newtech.taskmanager.provider";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    private final static int IGNORELIST_TABLE = 0;

    private final static int AUTOLIST_TABLE = 1;

    /** the matching table of provide URI */
    private static UriMatcher sUriMatcher;

    /** the helper of Blacklist Provider **/
    private SQLiteOpenHelper mDbHelper;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, TaskmanagerDatabaseHelper.Tables.IGNORELIST, IGNORELIST_TABLE);
        sUriMatcher.addURI(AUTHORITY, TaskmanagerDatabaseHelper.Tables.AUTOLIST, AUTOLIST_TABLE);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            count = db.delete(getTableName(uri), selection, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (count > 0) {
            notifyChange();
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {

        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id = -1;
        db.beginTransaction();

        try {
            String table = getTableName(uri);
            id = db.insert(table, null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        Uri result = null;
        if (id > 0) {
            result = ContentUris.withAppendedId(uri, id);
            notifyChange();
        }
        return result;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new TaskmanagerDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Cursor cursor = null;
        SQLiteQueryBuilder sqBuilder = new SQLiteQueryBuilder();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        sqBuilder.setTables(getTableName(uri));

        cursor = sqBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            count = db.update(getTableName(uri), values, selection, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (count > 0) {
            notifyChange();
        }

        return count;
    }

    private String getTableName(Uri uri) {
        String tableName = null;
        int match = sUriMatcher.match(uri);

        switch (match) {
            case IGNORELIST_TABLE:
                tableName = TaskmanagerDatabaseHelper.Tables.IGNORELIST;
                break;
            case AUTOLIST_TABLE:
                tableName = TaskmanagerDatabaseHelper.Tables.AUTOLIST;
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);

        }
        return tableName;
    }

    private void notifyChange() {
        getContext().getContentResolver().notifyChange(CONTENT_URI, null, false);
    }
}
