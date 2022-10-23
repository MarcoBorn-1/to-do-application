package com.example.todoapplication;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class TaskDatabaseHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "TaskDatabase.db";

    /*public Cursor query (boolean distinct, String table,
                         String[] columns, String selection,
                         String[] selectionArgs, String groupBy,
                         String having, String orderBy, String limit)*/

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TaskDatabase.FeedEntry.TABLE_NAME + " (" +
                    TaskDatabase.FeedEntry._ID + " INTEGER PRIMARY KEY," +
                    TaskDatabase.FeedEntry.COLUMN_NAME_TITLE + " TEXT," +
                    TaskDatabase.FeedEntry.COLUMN_NAME_DESC + " TEXT," +
                    TaskDatabase.FeedEntry.COLUMN_NAME_CATEGORY + " INTEGER," +
                    TaskDatabase.FeedEntry.COLUMN_NAME_DATE_OF_CREATION + " TEXT," +
                    TaskDatabase.FeedEntry.COLUMN_NAME_DATE_OF_DEADLINE + " TEXT," +
                    TaskDatabase.FeedEntry.COLUMN_NAME_DATE_OF_FINISH + " TEXT," +
                    TaskDatabase.FeedEntry.COLUMN_NAME_IS_FINISHED + " INT," +
                    TaskDatabase.FeedEntry.COLUMN_NAME_ATTACHMENT + " TEXT," +
                    TaskDatabase.FeedEntry.COLUMN_NAME_HAS_NOTIFICATIONS + " INT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TaskDatabase.FeedEntry.TABLE_NAME;

    public TaskDatabaseHelper (Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}