package com.dm.smart;

import static com.dm.smart.SubjectFragment.extractPatientFromTheDB;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.dm.smart.databinding.ActivityMainBinding;
import com.dm.smart.items.Subject;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    static Subject currentlySelectedSubject;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add a dafault patient if the database is empty
        DBAdapter db = new DBAdapter(this);
        db.open();
        Cursor cursor = db.getAllPatients();
        if (cursor.getCount() == 0) {
            Subject defaultSubject = new Subject("Default Patient", 0);
            db.insertPatient(defaultSubject);
        }
        cursor.moveToFirst();
        currentlySelectedSubject = extractPatientFromTheDB(cursor);
        db.close();

        com.dm.smart.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_subject, R.id.navigation_add_sense)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        BottomNavigationView navigationView = findViewById(R.id.nav_view);

        navigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_add_sense:
                    NavigationUI.onNavDestinationSelected(item, navController);
                    break;
                case R.id.navigation_subject:
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.dialog_save_images)
                            .setPositiveButton(R.string.dialog_yes, (dialog, id) ->
                                    NavigationUI.onNavDestinationSelected(item, navController))
                            .setNegativeButton(R.string.dialog_no, (dialog, id) -> {
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    break;
            }
            return true;
        });
    }
}