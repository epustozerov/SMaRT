package com.dm.smart;

import static com.dm.smart.SubjectFragment.extractPatientFromTheDB;

import android.database.Cursor;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.dm.smart.databinding.ActivityMainBinding;
import com.dm.smart.items.Subject;

public class MainActivity extends AppCompatActivity {

    static Subject currentlySelectedSubject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add a dafault patient if the database is empty
        DBAdapter db = new DBAdapter(this);
        db.open();
        // check if table patients is empty
        Cursor cursor = db.getAllPatients();
        if (cursor.getCount() == 0) {
            Subject defaultSubject = new Subject("Default Patient", 0);
            db.insertPatient(defaultSubject);
            currentlySelectedSubject = defaultSubject;
        } else {
            cursor.moveToFirst();
            currentlySelectedSubject = extractPatientFromTheDB(cursor);
        }
        db.close();

        com.dm.smart.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_patient, R.id.navigation_add_sense, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }
}