package com.dm.smart;

import static com.dm.smart.SubjectFragment.extractSubjectFromTheDB;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
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

import org.ini4j.Ini;
import org.ini4j.IniPreferences;

import java.io.IOException;
import java.util.Objects;
import java.util.prefs.BackingStoreException;

public class MainActivity extends AppCompatActivity {

    static Subject currentlySelectedSubject;
    static SharedPreferences sharedPref;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = this.getPreferences(Context.MODE_PRIVATE);

        // Config
        Configuration.checkConfigFolder();
        try {
            Configuration.initConfig(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Add a dafault patient if the database is empty
        DBAdapter db = new DBAdapter(this);
        db.open();
        Cursor cursor = db.getAllSubjects();
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            currentlySelectedSubject = extractSubjectFromTheDB(cursor);
        } else {
            currentlySelectedSubject = new Subject("Default Subject", "Default", "neutral");
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
                    // check if the config of the selected patient matches the selected config in system preferences
                    String selectedConfig = sharedPref.getString(getString(R.string.sp_selected_config), "");
                    String patientConfig = currentlySelectedSubject.getConfig();
                    if (!selectedConfig.equals("") && !selectedConfig.equals(patientConfig)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage(R.string.dialog_config_mismatch);
                        builder.setPositiveButton(R.string.dialog_ok, (dialog, id) -> {
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        return false;
                    } else {
                        if (sharedPref.getBoolean(getString(R.string.sp_show_instructions), false)) {
                            android.app.AlertDialog alertDialog = CustomAlertDialogs.showInstructions(this);
                            alertDialog.show();
                        }
                        return NavigationUI.onNavDestinationSelected(item, navController);
                    }
                case R.id.navigation_subject:
                    // if we are not at th subject fragment, we can go there
                    if (Objects.requireNonNull(navController.getCurrentDestination()).getId() != R.id.navigation_subject) {
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
        boolean showInstructions = sharedPref.getBoolean(getString(R.string.sp_show_instructions), false);
        boolean requestPassword = sharedPref.getBoolean(getString(R.string.sp_request_password), false);
        boolean showNames = sharedPref.getBoolean(getString(R.string.sp_show_names), false);
        boolean customConfig = sharedPref.getBoolean(getString(R.string.sp_custom_config), false);
        menu.findItem(R.id.menu_show_instructions).setChecked(showInstructions);
        menu.findItem(R.id.menu_request_password).setChecked(requestPassword);
        menu.findItem(R.id.menu_show_names).setChecked(showNames);
        menu.findItem(R.id.menu_custom_config).setChecked(customConfig);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            menu.setGroupDividerEnabled(true);
        }
        // update the text on the selected config menu item
        String selectedConfig = sharedPref.getString(getString(R.string.sp_selected_config), "");
        if (selectedConfig.equals("")) {
            menu.findItem(R.id.menu_selected_config).setTitle(getString(R.string.menu_selected_config_default));
        } else {
            menu.findItem(R.id.menu_selected_config).setTitle(getString(R.string.menu_selected_config) + " " + selectedConfig);
        }
        return true;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
        } else if (item.getItemId() == R.id.menu_custom_config) {
            String pref = getString(R.string.sp_custom_config);
            item.setChecked(!item.isChecked());
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(pref, item.isChecked());
            editor.apply();
            if (item.isChecked()) {
                // set the uri to Documents folder
                Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath());
                pickConfig(uri);
            }
        } else if (item.getItemId() == R.id.menu_selected_config) {
            // open the selected config file
            String configPath = sharedPref.getString(getString(R.string.sp_custom_config_path), "");
            try {
                ContentResolver contentResolver = getContentResolver();
                IniPreferences iniPreference = new IniPreferences(new Ini(contentResolver.openInputStream(Uri.parse(configPath))));
                String[] configNames = iniPreference.childrenNames();
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.dialog_select_config);
                builder.setItems(configNames, (dialog, which) -> {
                    String configName = configNames[which];
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.sp_selected_config), configName);
                    editor.apply();
                    invalidateOptionsMenu();
                    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
                    navController.navigate(R.id.navigation_subject);
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            } catch (IOException | BackingStoreException e) {
                throw new RuntimeException(e);
            }
        } else if (item.getItemId() == R.id.menu_imprint) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.imprint);
            builder.setPositiveButton(R.string.dialog_ok, (dialog, id) -> {
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else if (item.getItemId() == R.id.menu_privacy) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.privacy);
            builder.setPositiveButton(R.string.dialog_ok, (dialog, id) -> {
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else if (item.getItemId() == R.id.menu_info) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.info);
            builder.setPositiveButton(R.string.dialog_ok, (dialog, id) -> {
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else if (item.getItemId() == R.id.menu_copyright) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.copyright);
            builder.setPositiveButton(R.string.dialog_ok, (dialog, id) -> {
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return false;
    }

    private void pickConfig(Uri pickerInitialUri) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        //noinspection deprecation
        startActivityForResult(intent, 1, null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == 1) {
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                assert uri != null;
                // safe the uri to shared preferences
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.sp_custom_config_path), uri.toString());
                editor.apply();

                // grant permissions to read the file
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }
    }
}