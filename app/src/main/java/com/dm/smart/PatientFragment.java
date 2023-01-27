package com.dm.smart;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class PatientFragment extends Fragment {

    RecyclerViewAdapterPatients adapter;
    private ArrayList<String> patients;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View mView = inflater.inflate(R.layout.fragment_patient, container, false);
        RecyclerView list_view_patients = mView.findViewById(R.id.list_view_patients);
        EditText edittext_patient_name = mView.findViewById(R.id.edittext_patient_name);
        Button button_add_patients = mView.findViewById(R.id.button_add_patient);

        list_view_patients.setLayoutManager(new LinearLayoutManager(requireActivity()));
        patients = new ArrayList<>();
        adapter = new RecyclerViewAdapterPatients(requireActivity(), patients);
        list_view_patients.setAdapter(adapter);

        button_add_patients.setOnClickListener((View view) -> {

            DBAdapter DBAdapter = new DBAdapter(requireActivity());
            DBAdapter.open();
            DBAdapter.insertPatient(edittext_patient_name.getText().toString());
            DBAdapter.close();
            populateList();
        });
        populateList();
        return mView;
    }


    private void populateList() {
        patients.clear();
        DBAdapter DBAdapter = new DBAdapter(requireActivity());
        DBAdapter.open();
        Cursor cursorPatients = DBAdapter.getAllPatients();
        updateArray(cursorPatients);
        DBAdapter.close();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateArray(Cursor cursorPatients) {

        if (cursorPatients.moveToFirst())
            do {
                @SuppressLint("Range") String name =
                        cursorPatients.getString(cursorPatients.
                                getColumnIndex(com.dm.smart.DBAdapter.PATIENT_NAME));
                patients.add(0, name);
            } while (cursorPatients.moveToNext());
        adapter.notifyDataSetChanged();
        cursorPatients.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        populateList();
    }

}