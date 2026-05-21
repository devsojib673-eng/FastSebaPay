package com.sms.sopnopay;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class sqlite extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "sms_database";
    private static final int DATABASE_VERSION = 2;

    // SMS Table
    public static final String TABLE_SMS = "sms";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_BODY = "body";

    // Transaction Table
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COL_TXN_ID = "id";
    public static final String COL_TXN_ADDRESS = "address";
    public static final String COL_TXN_MESSAGE = "message";
    public static final String COL_TXN_STATUS = "status";
    public static final String COL_TXN_DATE = "created_at";

    private static final String CREATE_TABLE_SMS =
            "CREATE TABLE " + TABLE_SMS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_TITLE + " TEXT,"
                    + COLUMN_BODY + " TEXT"
                    + ")";

    private static final String CREATE_TABLE_TRANSACTIONS =
            "CREATE TABLE " + TABLE_TRANSACTIONS + "("
                    + COL_TXN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COL_TXN_ADDRESS + " TEXT,"
                    + COL_TXN_MESSAGE + " TEXT,"
                    + COL_TXN_STATUS + " TEXT DEFAULT 'pending',"
                    + COL_TXN_DATE + " TEXT"
                    + ")";

    public sqlite(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SMS);
        db.execSQL(CREATE_TABLE_TRANSACTIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_TABLE_TRANSACTIONS);
        }
    }

    // SMS methods
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

    // Transaction methods
    public long saveTransaction(String address, String message, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TXN_ADDRESS, address);
        values.put(COL_TXN_MESSAGE, message);
        values.put(COL_TXN_STATUS, status);
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        values.put(COL_TXN_DATE, timestamp);
        return db.insert(TABLE_TRANSACTIONS, null, values);
    }

    public void updateTransactionStatus(long id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TXN_STATUS, status);
        db.update(TABLE_TRANSACTIONS, values, COL_TXN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public ArrayList<HashMap<String, String>> getAllTransactions() {
        return getTransactionsByStatus(null);
    }

    public ArrayList<HashMap<String, String>> getTransactionsByStatus(String status) {
        ArrayList<HashMap<String, String>> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = status != null ? COL_TXN_STATUS + " = ?" : null;
        String[] selectionArgs = status != null ? new String[]{status} : null;
        Cursor cursor = db.query(TABLE_TRANSACTIONS, null, selection, selectionArgs, null, null, COL_TXN_ID + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                HashMap<String, String> txn = new HashMap<>();
                txn.put("id", String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(COL_TXN_ID))));
                txn.put("address", cursor.getString(cursor.getColumnIndexOrThrow(COL_TXN_ADDRESS)));
                txn.put("message", cursor.getString(cursor.getColumnIndexOrThrow(COL_TXN_MESSAGE)));
                txn.put("status", cursor.getString(cursor.getColumnIndexOrThrow(COL_TXN_STATUS)));
                txn.put("date", cursor.getString(cursor.getColumnIndexOrThrow(COL_TXN_DATE)));
                list.add(txn);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public int getTransactionCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TRANSACTIONS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }
}
