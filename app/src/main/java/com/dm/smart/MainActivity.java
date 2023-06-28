package com.dm.smart;

import static com.dm.smart.SubjectFragment.extractSubjectFromTheDB;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.dm.smart.databinding.ActivityMainBinding;
import com.dm.smart.items.Subject;
import com.dm.smart.ui.elements.CustomAlertDialogs;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    static Subject currentlySelectedSubject;

    static SharedPreferences sharedPref;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = this.getPreferences(Context.MODE_PRIVATE);

        // Add a dafault patient if the database is empty
        DBAdapter db = new DBAdapter(this);
        db.open();
        Cursor cursor = db.getAllSubjects();
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            currentlySelectedSubject = extractSubjectFromTheDB(cursor);
        } else {
            currentlySelectedSubject = new Subject("Default Subject", 0);
        }
        db.close();

        com.dm.smart.databinding.ActivityMainBinding binding =
                ActivityMainBinding.inflate(getLayoutInflater());
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
                    if (sharedPref.getBoolean(getString(R.string.sp_show_instructions), false)) {
                        android.app.AlertDialog alertDialog = CustomAlertDialogs.showInstructions(this);
                        alertDialog.show();
                    }
                    return NavigationUI.onNavDestinationSelected(item, navController);
                case R.id.navigation_subject:
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.dialog_save_images);
                    builder.setPositiveButton(R.string.dialog_continue, (dialog, id) -> {
                        NavigationUI.onNavDestinationSelected(item, navController);
                        if (sharedPref.getBoolean(getString(R.string.sp_request_password), false)) {
                            AlertDialog alertDialog = CustomAlertDialogs.requestPassword(
                                    MainActivity.this, null, null, null);
                            alertDialog.show();
                        }
                    });
                    builder.setNegativeButton(R.string.dialog_no, (dialog, id) -> {
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return false;
            }
            return false;
        });

        if (sharedPref.getBoolean(getString(R.string.sp_request_password), false)) {
            android.app.AlertDialog alertDialog = CustomAlertDialogs.requestPassword(this, null, null, null);
            alertDialog.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        boolean show_instructions = sharedPref.getBoolean(getString(R.string.sp_show_instructions), false);
        boolean request_password = sharedPref.getBoolean(getString(R.string.sp_request_password), false);
        boolean show_names = sharedPref.getBoolean(getString(R.string.sp_show_names), false);
        menu.findItem(R.id.menu_show_instructions).setChecked(show_instructions);
        menu.findItem(R.id.menu_request_password).setChecked(request_password);
        menu.findItem(R.id.menu_show_names).setChecked(show_names);
        return true;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == R.id.menu_instructions) {
            android.app.AlertDialog alertDialog = CustomAlertDialogs.showInstructions(this);
            alertDialog.show();
            return true;
        } else if (item.getItemId() == R.id.menu_show_instructions) {
            item.setChecked(!item.isChecked());
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(getString(R.string.sp_show_instructions), item.isChecked());
            editor.apply();
        } else if (item.getItemId() == R.id.menu_request_password) {
            String pref = getString(R.string.sp_request_password);
            if (item.isChecked()) {
                android.app.AlertDialog alertDialog = CustomAlertDialogs.requestPassword(this, sharedPref, pref, item);
                alertDialog.show();
            } else {
                item.setChecked(!item.isChecked());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(pref, item.isChecked());
                editor.apply();
            }
        } else if (item.getItemId() == R.id.menu_show_names) {
            String pref = getString(R.string.sp_show_names);
            if (!item.isChecked()) {
                android.app.AlertDialog alertDialog = CustomAlertDialogs.requestPassword(this, sharedPref, pref, item);
                alertDialog.show();
            } else {
                item.setChecked(!item.isChecked());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(pref, item.isChecked());
                editor.apply();
            }
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
            if (Objects.requireNonNull(navController.getCurrentDestination()).getId() == R.id.navigation_subject)
                navController.navigate(R.id.navigation_subject);
        }
        return false;
    }
}