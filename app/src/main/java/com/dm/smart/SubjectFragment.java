package com.dm.smart;

import static com.dm.smart.RecyclerViewAdapterPatients.PATIENT_DELETE;
import static com.dm.smart.RecyclerViewAdapterRecords.RECORD_DELETE;
import static com.dm.smart.RecyclerViewAdapterRecords.RECORD_SHOW_FOLDER;
import static com.dm.smart.RecyclerViewAdapterRecords.RECORD_SHOW_IMAGE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dm.smart.items.Record;
import com.dm.smart.items.Subject;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;

public class SubjectFragment extends Fragment {

    RecyclerViewAdapterPatients adapter_patients;
    RecyclerViewAdapterRecords adapter_records;
    private ArrayList<Subject> subjects;
    private ArrayList<Record> records;

    static Subject extractPatientFromTheDB(Cursor cursor) {
        @SuppressLint("Range") int id = cursor.getInt(cursor.
                getColumnIndex(DBAdapter.SUBJECT_ID));
        @SuppressLint("Range") String name =
                cursor.getString(cursor.
                        getColumnIndex(com.dm.smart.DBAdapter.SUBJECT_NAME));
        @SuppressLint("Range") String gender =
                cursor.getString(cursor.
                        getColumnIndex(DBAdapter.SUBJECT_GENDER));
        @SuppressLint("Range") long timestamp = cursor.getLong(cursor.
                getColumnIndex(DBAdapter.SUBJECT_TIMESTAMP));
        return new Subject(id, name, gender, timestamp);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View mView = inflater.inflate(R.layout.fragment_patient, container, false);
        Log.e("SELECTED PATIENT", String.valueOf(MainActivity.currentlySelectedSubject));

        // Spinner for Gender selection
        Spinner spinner = mView.findViewById(R.id.spinner_gender);
        ArrayAdapter<CharSequence> adapter_gender = ArrayAdapter.createFromResource(getContext(),
                R.array.genders, android.R.layout.simple_spinner_item);
        adapter_gender.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter_gender);

        // RecyclerView for Patients
        RecyclerView list_view_patients = mView.findViewById(R.id.list_view_patients);
        list_view_patients.setLayoutManager(new LinearLayoutManager(requireActivity()));
        subjects = new ArrayList<>();
        adapter_patients = new RecyclerViewAdapterPatients(requireActivity(), subjects);
        list_view_patients.setAdapter(adapter_patients);
        adapter_patients.setClickListener((int position) -> {
            int previousSelectedPatientId = adapter_patients.selectedPatientPosition;
            adapter_patients.selectedPatientPosition = position;
            adapter_patients.notifyItemChanged(position);
            adapter_patients.notifyItemChanged(previousSelectedPatientId);
            DBAdapter DBAdapter = new DBAdapter(requireActivity());
            DBAdapter.open();
            Cursor cursorSinglePatient =
                    DBAdapter.getPatientById(adapter_patients.getItem(position).getId());
            cursorSinglePatient.moveToFirst();
            MainActivity.currentlySelectedSubject = extractPatientFromTheDB(cursorSinglePatient);
            cursorSinglePatient.close();
            DBAdapter.close();
            Log.e("SELECTED AFTER CLICK", String.valueOf(MainActivity.currentlySelectedSubject));
            populateListRecords();
        });

        // RecyclerView for Records
        RecyclerView list_view_records = mView.findViewById(R.id.list_view_records);
        list_view_records.setLayoutManager(new LinearLayoutManager(requireActivity()));
        records = new ArrayList<>();
        adapter_records = new RecyclerViewAdapterRecords(requireActivity(), records);
        list_view_records.setAdapter(adapter_records);

        // EditText and Button for adding new Patients
        EditText edittext_patient_name = mView.findViewById(R.id.edittext_patient_name);
        Button button_add_patients = mView.findViewById(R.id.button_add_patient);
        button_add_patients.setOnClickListener((View view) -> {
            DBAdapter DBAdapter = new DBAdapter(requireActivity());
            DBAdapter.open();
            Subject new_subject =
                    new Subject(edittext_patient_name.getText().toString(),
                            spinner.getSelectedItem().toString());
            DBAdapter.insertPatient(new_subject);
            DBAdapter.close();
            edittext_patient_name.setText("");
            populateListPatients();
        });
        populateListPatients();
        populateListRecords();
        return mView;
    }

    private void populateListPatients() {
        subjects.clear();
        DBAdapter DBAdapter = new DBAdapter(requireActivity());
        DBAdapter.open();
        Cursor cursorPatients = DBAdapter.getAllPatients();
        updateArrayPatients(cursorPatients);
        DBAdapter.close();
    }

    public void populateListRecords() {
        records.clear();
        DBAdapter DBAdapter = new DBAdapter(requireActivity());
        DBAdapter.open();
        Cursor cursorRecords =
                DBAdapter.getRecordsSinglePatient(MainActivity.currentlySelectedSubject.getId());
        updateArrayRecords(cursorRecords);
        DBAdapter.close();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateArrayPatients(Cursor cursorPatients) {
        if (cursorPatients.moveToFirst())
            do {
                Subject newSubject = extractPatientFromTheDB(cursorPatients);
                subjects.add(newSubject);
            } while (cursorPatients.moveToNext());
        adapter_patients.notifyDataSetChanged();
        cursorPatients.close();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateArrayRecords(Cursor cursorRecords) {

        if (cursorRecords.moveToFirst())
            do {
                @SuppressLint("Range") int id = cursorRecords.getInt(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_ID));
                @SuppressLint("Range") int patient_id = cursorRecords.getInt(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_PATIENT_ID));
                @SuppressLint("Range") long timestamp = cursorRecords.getLong(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_TIMESTAMP));
                records.add(0, new Record(id, patient_id, timestamp));
            } while (cursorRecords.moveToNext());
        adapter_records.notifyDataSetChanged();
        cursorRecords.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == PATIENT_DELETE) {
            showDeletePatientDialog();
            return true;
        } else if (item.getItemId() == RECORD_DELETE) {
            showDeleteRecordDialog();
            return true;
        } else if (item.getItemId() == RECORD_SHOW_FOLDER) {
            openFolder();
            return true;
        } else if (item.getItemId() == RECORD_SHOW_IMAGE) {
            showMergedImageDialog();
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        populateListPatients();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void openFolder() {
        Record selectedRecord =
                adapter_records.getItem(adapter_records.selectedRecordPosition);
        DBAdapter DBAdapter = new DBAdapter(requireActivity());
        DBAdapter.open();
        Cursor cursorSinglePatient =
                DBAdapter.getPatientById(selectedRecord.getPatientId());
        cursorSinglePatient.moveToFirst();
        Subject selected_subject = extractPatientFromTheDB(cursorSinglePatient);
        cursorSinglePatient.close();
        DBAdapter.close();
        Uri uri = Uri.parse(String.valueOf(Paths.get(String.valueOf(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOCUMENTS)), "SMaRT",
                selectedRecord.getPatientId() + " " + selected_subject.getName(),
                String.valueOf(selectedRecord.getId()))));
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR);
        startActivity(Intent.createChooser(intent, "Open folder"));
    }

    public void showDeletePatientDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(getResources().getString(R.string.dialog_delete_subject)).
                setPositiveButton(getResources().getString(R.string.dialog_yes),
                        (dialog, id) -> {
                            Subject selectedSubject =
                                    adapter_patients.getItem(adapter_patients.selectedPatientPosition);
                            DBAdapter DBAdapter = new DBAdapter(requireActivity());
                            DBAdapter.open();
                            DBAdapter.deletePatient(selectedSubject.getId());
                            DBAdapter.close();
                            populateListPatients();
                        })
                .setNegativeButton(getResources().getString(R.string.dialog_no),
                        (dialog, id) -> {
                        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showDeleteRecordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(getResources().getString(R.string.dialog_delete_record)).
                setPositiveButton(getResources().getString(R.string.dialog_yes),
                        (dialog, id) -> {
                            Record selectedRecord =
                                    adapter_records.getItem(adapter_records.selectedRecordPosition);
                            DBAdapter DBAdapter = new DBAdapter(requireActivity());
                            DBAdapter.open();
                            DBAdapter.deleteRecord(selectedRecord.getId());
                            DBAdapter.close();
                            populateListRecords();
                        })
                .setNegativeButton(getResources().getString(R.string.dialog_no),
                        (dialog, id) -> {
                        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("UseCompatLoadingForDrawables")
    public void showMergedImageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        @SuppressLint("InflateParams") View alertView =
                getLayoutInflater().inflate(R.layout.alert_image, null);
        ImageView imageMerged = alertView.findViewById(R.id.merged_image);
        imageMerged.setImageDrawable(requireContext().getDrawable(R.drawable.body_neutral_front));

        Record selectedRecord =
                adapter_records.getItem(adapter_records.selectedRecordPosition);
        DBAdapter DBAdapter = new DBAdapter(requireActivity());
        DBAdapter.open();
        Cursor cursorSinglePatient =
                DBAdapter.getPatientById(selectedRecord.getPatientId());
        cursorSinglePatient.moveToFirst();
        Subject selected_subject = extractPatientFromTheDB(cursorSinglePatient);
        cursorSinglePatient.close();
        DBAdapter.close();
        Uri uri = Uri.parse(String.valueOf(Paths.get(String.valueOf(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOCUMENTS)), "SMaRT",
                selectedRecord.getPatientId() + " " + selected_subject.getName(),
                String.valueOf(selectedRecord.getId()), "f_merged.png")));
        File imgFile = new File(String.valueOf(uri));
        if (imgFile.exists()) {
            Bitmap background = BitmapFactory.decodeResource(getResources(),
                    R.drawable.body_neutral_front);
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            Bitmap mutableBackground = background.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBackground);
            canvas.drawBitmap(mutableBackground, 0f, 0f, null);
            canvas.drawBitmap(myBitmap, 0f, 0f, null);
            imageMerged.setImageBitmap(mutableBackground);
        }

        builder.setView(alertView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}