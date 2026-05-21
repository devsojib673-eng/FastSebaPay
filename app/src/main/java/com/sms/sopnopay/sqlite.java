package com.sms.sopnopay;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class sqlite extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "sms_database";
    private static final int DATABASE_VERSION = 1;

    // Table name and columns
    public static final String TABLE_SMS = "sms";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_BODY = "body";



    // Create table SQL query
    private static final String CREATE_TABLE_SMS =
            "CREATE TABLE " + TABLE_SMS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_TITLE + " TEXT,"
                    + COLUMN_BODY + " TEXT"
                    + ")";

    public sqlite(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SMS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed, and create a new one
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SMS);
        onCreate(db);
    }

    public long saveSms(String title, String body) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_BODY, body);

        return db.insert(TABLE_SMS, null, values);
    }

    public ArrayList<HashMap<String, String>> getAllSms() {
        ArrayList<HashMap<String, String>> smsList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {COLUMN_ID, COLUMN_TITLE, COLUMN_BODY};
        Cursor cursor = db.query(TABLE_SMS, projection, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                HashMap<String, String> sms = new HashMap<>();
                sms.put(COLUMN_ID, String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))));
                sms.put(COLUMN_TITLE, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                sms.put(COLUMN_BODY, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BODY)));
                smsList.add(sms);
            } while (cursor.moveToNext());

            cursor.close();
        }

        return smsList;
    }

    public void deleteSms(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SMS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}
