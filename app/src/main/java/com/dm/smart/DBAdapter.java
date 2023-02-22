package com.dm.smart;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.dm.smart.items.Record;
import com.dm.smart.items.Subject;

import java.util.Date;

public class DBAdapter {
    public static final String SUBJECT_ID = "id";
    public static final String SUBJECT_NAME = "name";
    public static final String SUBJECT_GENDER = "gender";
    public static final String SUBJECT_DELETED = "deleted";
    public static final String SUBJECT_TIMESTAMP = "timestamp";
    public static final String RECORD_ID = "id";
    public static final String RECORD_PATIENT_ID = "patient_id";
    public static final String RECORD_SENSATIONS = "sensations";
    public static final String RECORD_DELETED = "deleted";
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

    void deletePatient(long patient_id) {
        ContentValues updatePatient = new ContentValues();
        updatePatient.put(SUBJECT_DELETED, 1);
        db.update(DATABASE_TABLE_PATIENTS, updatePatient, SUBJECT_ID + "="
                + patient_id, null);
        ContentValues updatedRecord = new ContentValues();
        updatedRecord.put(RECORD_DELETED, 1);
        db.update(DATABASE_TABLE_RECORDS, updatedRecord, SUBJECT_ID + "="
                + patient_id, null);
    }

    void deleteRecord(long record_id) {
        ContentValues updatedRecord = new ContentValues();
        updatedRecord.put(RECORD_DELETED, 1);
        db.update(DATABASE_TABLE_RECORDS, updatedRecord, RECORD_ID + "="
                + record_id, null);
    }

    Cursor getPatientById(long patient_id) {
        return db.query(DATABASE_TABLE_PATIENTS,
                new String[]{SUBJECT_ID, SUBJECT_NAME, SUBJECT_GENDER, SUBJECT_TIMESTAMP},
                SUBJECT_ID + "='" + patient_id + "'",
                null, null, null, RECORD_ID);
    }

    Cursor getAllPatients() {
        return db.query(DATABASE_TABLE_PATIENTS,
                new String[]{SUBJECT_ID, SUBJECT_NAME, SUBJECT_GENDER, SUBJECT_TIMESTAMP},
                SUBJECT_DELETED + "='" + 0 + "'", null, null, null, SUBJECT_ID + " DESC");
    }

    Cursor getRecordsSinglePatient(long patient_id) {
        return db.query(DATABASE_TABLE_RECORDS,
                new String[]{RECORD_ID, RECORD_PATIENT_ID, RECORD_SENSATIONS, RECORD_TIMESTAMP},
                (RECORD_PATIENT_ID + "='" + patient_id + "'") +
                        " AND " + (RECORD_DELETED + "='" + 0 + "'"),
                null, null, null, RECORD_ID);
    }

    public long insertPatient(Subject new_subject) {
        ContentValues cv_new_patient = new ContentValues();
        cv_new_patient.put(SUBJECT_NAME, new_subject.getName());
        cv_new_patient.put(SUBJECT_GENDER, new_subject.getGender());
        cv_new_patient.put(SUBJECT_DELETED, 0);
        long timestamp = new Date().getTime();
        cv_new_patient.put(SUBJECT_TIMESTAMP, timestamp);
        return db.insert(DATABASE_TABLE_PATIENTS, null, cv_new_patient);
    }

    public long insertRecord(Record new_record) {
        ContentValues cv_new_record = new ContentValues();
        cv_new_record.put(RECORD_PATIENT_ID, new_record.getPatientId());
        cv_new_record.put(RECORD_SENSATIONS, new_record.getSensations());
        cv_new_record.put(RECORD_DELETED, 0);
        long timestamp = new Date().getTime();
        cv_new_record.put(SUBJECT_TIMESTAMP, timestamp);
        return db.insert(DATABASE_TABLE_RECORDS, null, cv_new_record);
    }

    private static class DBOpenHelper extends SQLiteOpenHelper {

        private static final String DATABASE_CREATE_1 = "create table "
                + DATABASE_TABLE_PATIENTS + " (" + SUBJECT_ID
                + " integer primary key autoincrement, " + SUBJECT_NAME
                + " string, " + SUBJECT_GENDER + " integer, " + SUBJECT_DELETED + " integer, "
                + SUBJECT_TIMESTAMP + " long);";

        private static final String DATABASE_CREATE_2 = "create table " + DATABASE_TABLE_RECORDS +
                " (" + RECORD_ID + " integer primary key autoincrement, " + RECORD_PATIENT_ID +
                " integer, " + RECORD_SENSATIONS + " string, " + RECORD_DELETED + " integer, " +
                RECORD_TIMESTAMP + " long);";

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