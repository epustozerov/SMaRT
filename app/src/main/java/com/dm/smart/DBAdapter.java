package com.dm.smart;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Date;

public class DBAdapter {
    public static final String PATIENT_ID = "id";
    public static final String PATIENT_NAME = "name";
    public static final String PATIENT_TIMESTAMP = "timestamp";
    public static final String RECORD_ID = "id";
    public static final String RECORD_PATIENT_ID = "patient_id";
    public static final String RECORD_TIMESTAMP = "timestamp";
    private static final String DATABASE_NAME = "smart.db";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_TABLE_PATIENTS = "patients";
    private static final String DATABASE_TABLE_RECORDS = "records";
    private final DBOpenHelper dbHelper;
    private SQLiteDatabase db;

    public DBAdapter(Context _context) {
        dbHelper = new DBOpenHelper(_context
        );
    }

    public void close() {
        db.close();
    }

    public void open() throws SQLiteException {
        try {
            db = dbHelper.getWritableDatabase();
        } catch (SQLiteException ex) {
            db = dbHelper.getReadableDatabase();
        }
    }

    // ///////////////////////////////////
    // Insert new records to the DataBase
    // ///////////////////////////////////

    void insertPatient(String patient_name) {
        ContentValues new_patient = new ContentValues();
        new_patient.put(PATIENT_NAME, patient_name);
        long timestamp = new Date().getTime();
        new_patient.put(PATIENT_TIMESTAMP, timestamp);
        db.insert(DATABASE_TABLE_PATIENTS, null, new_patient);
    }

    Cursor getAllPatients() {
        return db.query(DATABASE_TABLE_PATIENTS,
                new String[]{PATIENT_ID, PATIENT_NAME, PATIENT_TIMESTAMP},
                null, null, null, null, PATIENT_ID);
    }

    Cursor getAllRecords() {
        return db.query(DATABASE_TABLE_RECORDS,
                new String[]{RECORD_ID, RECORD_PATIENT_ID, RECORD_TIMESTAMP},
                null, null, null, null, RECORD_ID);
    }

    private static class DBOpenHelper extends SQLiteOpenHelper {

        private static final String DATABASE_CREATE_1 = "create table "
                + DATABASE_TABLE_PATIENTS + " (" + PATIENT_ID
                + " integer primary key autoincrement, " + PATIENT_NAME
                + " string, " + PATIENT_TIMESTAMP + " long);";

        private static final String DATABASE_CREATE_2 = "create table "
                + DATABASE_TABLE_RECORDS + " (" + RECORD_ID
                + " integer primary key autoincrement, " + RECORD_PATIENT_ID
                + " integer, " + RECORD_TIMESTAMP + " long);";


        DBOpenHelper(Context context) {
            super(context, DBAdapter.DATABASE_NAME, null, DBAdapter.DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase _db) {
            _db.execSQL(DATABASE_CREATE_1);
            _db.execSQL(DATABASE_CREATE_2);
        }

        @Override
        public void onUpgrade(SQLiteDatabase _db, int _oldVersion,
                              int _newVersion) {
            Log.w("diaDBAdapter", "Upgrading from version " + _oldVersion
                    + " to " + _newVersion
                    + ", which will destroy some old data");

            // Drop the old table
            _db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_PATIENTS);
            _db.execSQL(DATABASE_CREATE_1);
            _db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_RECORDS);
            _db.execSQL(DATABASE_CREATE_2);
        }
    }
}