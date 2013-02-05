/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * TaskmanagerDatabaseHelper.java
 */

package com.newtech.taskmanager.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class TaskmanagerDatabaseHelper extends SQLiteOpenHelper {
    public static final String TAG = "TaskmanagerDatabaseHelper";

    /** The name of blacklist **/
    private static final String DATABASE_NAME = "taskmanager.db";

    /** The version of blacklist database */
    private static final int DATABASE_VERSION = 1;

    /** The tables of blacklist database */
    public interface Tables {
        public static final String IGNORELIST = "ignorelist";
        public static final String AUTOLIST = "autolist";
    }

    /** The column of blacklist/viplist table in blacklist database */
    public interface TableColumns {
        public static final String _ID = BaseColumns._ID;
        public static final String PACKAGE_NAME = "package_name";
    }

    public TaskmanagerDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_IGNORE_TABLE);
        db.execSQL(CREATE_AUTOLIST_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.AUTOLIST + ";");
        db.execSQL("DROP TABLE IF EXISTS " + Tables.IGNORELIST + ";");
        onCreate(db);
    }

    /** the sql statement of creating ignore table */
    private static final String CREATE_IGNORE_TABLE =
            "CREATE TABLE " + Tables.AUTOLIST + " ("
            + TableColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + TableColumns.PACKAGE_NAME + " TEXT NOT NULL UNIQUE "
            + ");";

    /** the sql statement of creating autolist table */
    private static final String CREATE_AUTOLIST_TABLE =
            "CREATE TABLE " + Tables.IGNORELIST + " ("
            + TableColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + TableColumns.PACKAGE_NAME + " TEXT NOT NULL UNIQUE "
            + ");";
}
