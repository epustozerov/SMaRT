package com.dm.smart;

import static com.dm.smart.RecyclerViewAdapterRecords.RECORD_DELETE;
import static com.dm.smart.RecyclerViewAdapterRecords.RECORD_SHARE;
import static com.dm.smart.RecyclerViewAdapterRecords.RECORD_SHOW_IMAGE;
import static com.dm.smart.RecyclerViewAdapterSubjects.SUBJECT_DELETE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dm.smart.items.Record;
import com.dm.smart.items.Subject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SubjectFragment extends Fragment {

    RecyclerViewAdapterSubjects adapter_subjects;
    RecyclerViewAdapterRecords adapter_records;
    private ArrayList<Subject> subjects;
    private ArrayList<Record> records;

    boolean current_view_front;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read shared preference to shown subjects' names
        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        boolean show_names = sharedPref.getBoolean(getString(R.string.sp_show_names), false);
        View mView = inflater.inflate(R.layout.fragment_subject, container, false);

        // Spinner for Gender selection
        Spinner spinner = mView.findViewById(R.id.spinner_gender);
        ArrayAdapter<CharSequence> adapter_gender = ArrayAdapter.createFromResource(getContext(),
                R.array.genders, android.R.layout.simple_spinner_item);
        adapter_gender.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter_gender);

        // RecyclerView for Subjects
        RecyclerView list_view_subjects = mView.findViewById(R.id.list_view_subjects);
        list_view_subjects.setLayoutManager(new LinearLayoutManager(requireActivity()));
        subjects = new ArrayList<>();
        adapter_subjects = new RecyclerViewAdapterSubjects(requireActivity(), subjects, show_names);
        list_view_subjects.setAdapter(adapter_subjects);
        adapter_subjects.setClickListener((int position) -> {
            int previousSelectedSubjectId = adapter_subjects.selectedSubjectPosition;
            adapter_subjects.selectedSubjectPosition = position;
            adapter_subjects.notifyItemChanged(position);
            adapter_subjects.notifyItemChanged(previousSelectedSubjectId);
            DBAdapter DBAdapter = new DBAdapter(requireActivity());
            DBAdapter.open();
            Cursor cursorSingleSubject =
                    DBAdapter.getSubjectById(adapter_subjects.getItem(position).getId());
            cursorSingleSubject.moveToFirst();
            MainActivity.currentlySelectedSubject = extractSubjectFromTheDB(cursorSingleSubject);
            cursorSingleSubject.close();
            DBAdapter.close();
            Log.e("SELECTED AFTER CLICK", String.valueOf(MainActivity.currentlySelectedSubject.getId()));
            populateListRecords();
        });

        // RecyclerView for Records
        RecyclerView list_view_records = mView.findViewById(R.id.list_view_records);
        list_view_records.setLayoutManager(new LinearLayoutManager(requireActivity()));
        records = new ArrayList<>();
        adapter_records = new RecyclerViewAdapterRecords(requireActivity(), records);
        list_view_records.setAdapter(adapter_records);

        // EditText and Button for adding new Subjects
        EditText edittext_patient_name = mView.findViewById(R.id.edittext_subject_name);
        Button button_add_patients = mView.findViewById(R.id.button_add_subject);
        button_add_patients.setOnClickListener((View view) -> {
            if (edittext_patient_name.getText().toString().equals("")) {
                Toast toast = Toast.makeText(getContext(), getString(R.string.toast_empty_name), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else {
                DBAdapter DBAdapter = new DBAdapter(requireActivity());
                DBAdapter.open();
                Subject new_subject =
                        new Subject(edittext_patient_name.getText().toString(),
                                (int) spinner.getSelectedItemId());
                new_subject.setId((int) DBAdapter.insertSubject(new_subject));
                DBAdapter.close();
                MainActivity.currentlySelectedSubject = new_subject;
                edittext_patient_name.setText("");
                populateListSubjects();
                populateListRecords();
            }
        });
        populateListSubjects();
        populateListRecords();
        return mView;
    }

    static Subject extractSubjectFromTheDB(Cursor cursor) {
        @SuppressLint("Range") int id = cursor.getInt(cursor.
                getColumnIndex(DBAdapter.SUBJECT_ID));
        @SuppressLint("Range") String name =
                cursor.getString(cursor.
                        getColumnIndex(com.dm.smart.DBAdapter.SUBJECT_NAME));
        @SuppressLint("Range") int gender =
                cursor.getInt(cursor.
                        getColumnIndex(DBAdapter.SUBJECT_GENDER));
        @SuppressLint("Range") long timestamp = cursor.getLong(cursor.
                getColumnIndex(DBAdapter.SUBJECT_TIMESTAMP));
        return new Subject(id, name, gender, timestamp);
    }

    private void populateListSubjects() {
        subjects.clear();
        DBAdapter DBAdapter = new DBAdapter(requireActivity());
        DBAdapter.open();
        Cursor cursorSubjects = DBAdapter.getAllSubjects();
        updateArraySubjects(cursorSubjects);
        DBAdapter.close();
    }

    public void populateListRecords() {
        records.clear();
        DBAdapter DBAdapter = new DBAdapter(requireActivity());
        DBAdapter.open();
        Cursor cursorRecords =
                DBAdapter.getRecordsSingleSubject(MainActivity.currentlySelectedSubject.getId());
        updateArrayRecords(cursorRecords);
        DBAdapter.close();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateArraySubjects(Cursor cursorSubjects) {
        if (cursorSubjects.moveToFirst())
            do {
                Subject newSubject = extractSubjectFromTheDB(cursorSubjects);
                subjects.add(newSubject);
            } while (cursorSubjects.moveToNext());
        Integer id_to_select = MainActivity.currentlySelectedSubject.getId();
        Subject selected = subjects.stream().filter(carnet ->
                id_to_select.equals(carnet.getId())).findFirst().orElse(null);
        adapter_subjects.selectedSubjectPosition = subjects.indexOf(selected);
        adapter_subjects.notifyDataSetChanged();
        cursorSubjects.close();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateArrayRecords(Cursor cursorRecords) {

        if (cursorRecords.moveToFirst())
            do {
                @SuppressLint("Range") int id = cursorRecords.getInt(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_ID));
                @SuppressLint("Range") int subject_id = cursorRecords.getInt(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_SUBJECT_ID));
                @SuppressLint("Range") int n = cursorRecords.getInt(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_N));
                @SuppressLint("Range") String sensations = cursorRecords.getString(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_SENSATIONS));
                @SuppressLint("Range") long timestamp = cursorRecords.getLong(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_TIMESTAMP));
                records.add(0, new Record(id, subject_id, n, sensations, timestamp));
            } while (cursorRecords.moveToNext());
        adapter_records.notifyDataSetChanged();
        cursorRecords.close();
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == SUBJECT_DELETE) {
            showDeleteSubjectDialog();
            return true;
        } else if (item.getItemId() == RECORD_DELETE) {
            showDeleteRecordDialog();
            return true;
        } else if (item.getItemId() == RECORD_SHARE) {
            shareSensations();
            return true;
        } else if (item.getItemId() == RECORD_SHOW_IMAGE) {
            try {
                showMergedImageDialog();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
    }

    private void shareSensations() {

        // Load front and back sensations images
        Record selectedRecord =
                adapter_records.getItem(adapter_records.selectedRecordPosition);
        Subject selectedSubject = subjects.get(adapter_subjects.selectedSubjectPosition);
        File image_sensations_front = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS) + "/SMaRT/" + selectedSubject.getId() + " " +
                selectedSubject.getName() + "/" + selectedRecord.getId() + "/complete_picture_f.png");
        File image_sensations_back = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS) + "/SMaRT/" + selectedSubject.getId() + " " +
                selectedSubject.getName() + "/" + selectedRecord.getId() + "/complete_picture_b.png");
        Uri image_sensations_front_uri = FileProvider.getUriForFile(requireActivity(),
                BuildConfig.APPLICATION_ID + ".provider",
                image_sensations_front);
        Uri image_sensations_back_uri = FileProvider.getUriForFile(requireActivity(),
                BuildConfig.APPLICATION_ID + ".provider",
                image_sensations_back);
        ArrayList<Uri> imageUris = new ArrayList<>();
        imageUris.add(image_sensations_front_uri);
        imageUris.add(image_sensations_back_uri);
        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        // shareIntent.setType("message/rfc822");
        shareIntent.setType("image/png");
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);

        // Make a date
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedRecord.getTimestamp());
        SimpleDateFormat formatter =
                new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String dateString = formatter.format(cal.getTime());

        // Write the colored list of sensations to email text
        List<Integer> colors = Arrays.stream(requireActivity().getResources().
                getIntArray(R.array.colors_symptoms)).boxed().collect(Collectors.toList());
        String text_sensations = selectedRecord.getSensations();
        ArrayList<String> list_sensations = new ArrayList<>(Arrays.asList(text_sensations.split(";")));
        StringBuilder text_sensations_colored = new StringBuilder();
        for (int i = 0; i < list_sensations.size(); i++) {
            list_sensations.set(i, "<font color=" + colors.get(i) + ">" + list_sensations.get(i) + "</font>");
            text_sensations_colored.append(list_sensations.get(i)).append("<br/>");
        }
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                Html.fromHtml(text_sensations_colored.toString(), Html.FROM_HTML_MODE_LEGACY));
        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                getResources().getString(R.string.menu_export_title, selectedSubject.getName(), dateString));
        startActivity(Intent.createChooser(shareIntent, "SEND IMAGE"));
    }


    public void showDeleteSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(getResources().getString(R.string.dialog_delete_subject)).
                setPositiveButton(getResources().getString(R.string.dialog_yes),
                        (dialog, id) -> {
                            Subject selectedSubject =
                                    adapter_subjects.getItem(adapter_subjects.selectedSubjectPosition);
                            DBAdapter DBAdapter = new DBAdapter(requireActivity());
                            DBAdapter.open();
                            DBAdapter.deleteSubject(selectedSubject.getId());
                            DBAdapter.close();
                            populateListSubjects();
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

    @SuppressLint("ResourceType")
    public void showMergedImageDialog() throws NoSuchFieldException, IllegalAccessException {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        @SuppressLint("InflateParams") View alertView =
                getLayoutInflater().inflate(R.layout.alert_image, null);
        ImageView image_view_body = alertView.findViewById(R.id.image_view_body);
        Record selectedRecord =
                adapter_records.getItem(adapter_records.selectedRecordPosition);
        DBAdapter DBAdapter = new DBAdapter(requireActivity());
        DBAdapter.open();
        Cursor cursorSingleSubject =
                DBAdapter.getSubjectById(selectedRecord.getSubjectId());
        cursorSingleSubject.moveToFirst();
        Subject selected_subject = subjects.get(adapter_subjects.selectedSubjectPosition);
        cursorSingleSubject.close();
        File image_sensations_front = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS) + "/SMaRT/" + selected_subject.getId()
                + "/" + selectedRecord.getId() + "/complete_picture_f.png");
        File image_sensations_back = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS) + "/SMaRT/" + selected_subject.getId()
                + "/" + selectedRecord.getId() + "/complete_picture_b.png");
        Bitmap sensations_front = BitmapFactory.decodeFile(image_sensations_front.getAbsolutePath());
        Bitmap sensations_back = BitmapFactory.decodeFile(image_sensations_back.getAbsolutePath());
        current_view_front = true;
        image_view_body.setImageBitmap(sensations_front);
        image_view_body.setOnClickListener(v -> reverse_body_view(image_view_body, sensations_front, sensations_back));
        builder.setView(alertView);
        AlertDialog dialog = builder.create();
        alertView.findViewById(R.id.button_close).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void reverse_body_view(ImageView image_view_body, Bitmap sensations_front, Bitmap sensations_back) {
        if (current_view_front) {
            image_view_body.setImageBitmap(sensations_front);
            current_view_front = false;
        } else {
            image_view_body.setImageBitmap(sensations_back);
            current_view_front = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        boolean show_names = sharedPref.getBoolean(getString(R.string.sp_show_names), false);
        adapter_subjects.setShowNames(show_names);
        populateListSubjects();
        populateListRecords();
    }
}