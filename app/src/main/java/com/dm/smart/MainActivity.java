package com.dm.smart;

import static com.dm.smart.CanvasFragment.verifyStoragePermissions;
import static com.dm.smart.SubjectFragment.extractSubjectFromTheDB;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.view.WindowManager;

import androidx.annotation.NonNull;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.prefs.BackingStoreException;

public class MainActivity extends AppCompatActivity {

    static Subject currentlySelectedSubject;
    static SharedPreferences sharedPref;

    @SuppressLint({"NonConstantResourceId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        formConfigAndSchemes();

        // Check if we have config.ini file in the app files folder
        File file = new File(getFilesDir(), "config.ini");
        boolean justCreated = !file.exists();
        if (justCreated) {
            try {
                Configuration.initDefaultConfig(this);
            } catch (Exception e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        }

        // Check if we have config.ini file in the Documents/SMaRT folder
        File file2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "SMaRT/config/config.ini");
        boolean justCreated2 = !file2.exists();
        if (justCreated2) {
            try {
                Configuration.initDefaultConfig(this);
            } catch (Exception e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        }

        // Add a dafault patient if the database is empty
        DBAdapter db = new DBAdapter(this);
        db.open();
        Cursor cursor = db.getAllSubjects();
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            currentlySelectedSubject = extractSubjectFromTheDB(cursor);
        } else {
            currentlySelectedSubject = new Subject("Default Subject", "Built-in", "neutral");
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
                    String selectedConfig = sharedPref.getString(getString(R.string.sp_selected_config), "Built-in");
                    String patientConfig = currentlySelectedSubject.getConfig();
                    // create a configuration object
                    String configPath = sharedPref.getString(getString(R.string.sp_custom_config_path),
                            getFilesDir() + "/config.ini");
                    Configuration patientConfiguration = new Configuration(configPath, patientConfig);
                    try {
                        patientConfiguration.formConfig(currentlySelectedSubject.getBodyScheme());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    if (!selectedConfig.isEmpty() && !selectedConfig.equals(patientConfig)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage(R.string.dialog_config_mismatch);
                        builder.setPositiveButton(R.string.dialog_ok, (dialog, id) -> {
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        return false;
                    } else {
                        if (sharedPref.getBoolean(getString(R.string.sp_show_instructions), false)) {
                            if (!sharedPref.getBoolean(getString(R.string.sp_custom_config), false)) {
                                AlertDialog alertDialog =
                                        CustomAlertDialogs.showInstructions(this, false, null);
                                alertDialog.show();
                            } else {
                                // create a configuration object
                                AlertDialog alertDialog =
                                        CustomAlertDialogs.showInstructions(this, true,
                                                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                                                        "SMaRT/config/" + patientConfiguration.getInstructionsPath()));
                                alertDialog.show();
                            }
                        }
                        return NavigationUI.onNavDestinationSelected(item, navController);
                    }
                case R.id.navigation_subject:
                    // if we are not at th subject fragment, we can go there
                    if (Objects.requireNonNull(navController.getCurrentDestination()).getId() != R.id.navigation_subject) {
                        AlertDialog.Builder builder = getBuilderSaveRecord(item, navController);
                        builder.setNegativeButton(R.string.dialog_no, (dialog, id) -> {
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        return false;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + item.getItemId());
            }
            return false;
        });

        if (sharedPref.getBoolean(getString(R.string.sp_request_password), false)) {
            android.app.AlertDialog alertDialog = CustomAlertDialogs.requestPassword(this, null, null, null);
            alertDialog.show();
            Objects.requireNonNull(alertDialog.getWindow()).setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private void formConfigAndSchemes() {
        boolean customConfig = sharedPref.getBoolean(getString(R.string.sp_custom_config), false);
        Configuration configuration;
        if (customConfig) {
            String configPath = sharedPref.getString(getString(R.string.sp_custom_config_path), getFilesDir() + "/config.ini");
            String configName = sharedPref.getString(getString(R.string.sp_selected_config), "Built-in");
            configuration = new Configuration(configPath, configName);
            try {
                configuration.formConfig("neutral");
                String[] bodySchemes = configuration.bodySchemes;
                for (String bodyScheme : bodySchemes) {
                    String[] bodySchemeParts = new String[4];
                    bodySchemeParts[0] = "body_" + bodyScheme + "_front.png";
                    bodySchemeParts[1] = "body_" + bodyScheme + "_front_mask.png";
                    bodySchemeParts[2] = "body_" + bodyScheme + "_back.png";
                    bodySchemeParts[3] = "body_" + bodyScheme + "_back_mask.png";
                    for (String bodySchemePart : bodySchemeParts) {
                        // define the "body_figures" folder in the app folder
                        File configFolder = new File(getFilesDir(), "body_figures");
                        if (!configFolder.exists()) {
                            //noinspection ResultOfMethodCallIgnored
                            configFolder.mkdirs();
                        }
                        File file = new File(configFolder, bodySchemePart);
                        if (!file.exists()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage(R.string.dialog_config_no_scheme);
                            builder.setPositiveButton(R.string.dialog_ok, (dialog, id) -> {
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putBoolean(getString(R.string.sp_custom_config), false);
                            editor.putString(getString(R.string.sp_selected_config), "Built-in");
                            editor.apply();
                        }
                    }
                }
            } catch (IOException e) {
                // Switch to built-in config
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.sp_custom_config), false);
                editor.putString(getString(R.string.sp_selected_config), "Built-in");
                editor.apply();
            }
        }
    }

    @NonNull
    private AlertDialog.Builder getBuilderSaveRecord(MenuItem item, NavController navController) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.dialog_save_images);
        builder.setPositiveButton(R.string.dialog_continue, (dialog, id) -> {
            NavigationUI.onNavDestinationSelected(item, navController);
            if (sharedPref.getBoolean(getString(R.string.sp_request_password), false)) {
                AlertDialog alertDialog = CustomAlertDialogs.requestPassword(
                        MainActivity.this, null, null, null);
                alertDialog.show();
                Objects.requireNonNull(alertDialog.getWindow()).setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
            }
        });
        return builder;
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
        if (selectedConfig.isEmpty() || !customConfig) {
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
            // if custom config is selected, show the instructions from the custom config
            if (!sharedPref.getBoolean(getString(R.string.sp_custom_config), false)) {
                android.app.AlertDialog alertDialog = CustomAlertDialogs.showInstructions(this, false, null);
                alertDialog.show();
            } else {
                String configPath = sharedPref.getString(getString(R.string.sp_custom_config_path), getFilesDir() + "/config.ini");
                String configName = sharedPref.getString(getString(R.string.sp_selected_config), "Built-in");
                Configuration configuration = new Configuration(configPath, configName);
                try {
                    configuration.formConfig("neutral");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String instructionsPath = configuration.getInstructionsPath();
                File instructionsFile = new File(getFilesDir(), instructionsPath);
                android.app.AlertDialog alertDialog = CustomAlertDialogs.showInstructions(this, true, instructionsFile);
                alertDialog.show();
            }
        }
        if (item.getItemId() == R.id.menu_show_instructions) {
            item.setChecked(!item.isChecked());
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(getString(R.string.sp_show_instructions), item.isChecked());
            editor.apply();
        } else if (item.getItemId() == R.id.menu_request_password) {
            String pref = getString(R.string.sp_request_password);
            if (item.isChecked()) {
                android.app.AlertDialog alertDialog = CustomAlertDialogs.requestPassword(this, sharedPref, pref, item);
                alertDialog.show();
                Objects.requireNonNull(alertDialog.getWindow()).setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
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
                Objects.requireNonNull(alertDialog.getWindow()).setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
            } else {
                item.setChecked(!item.isChecked());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(pref, item.isChecked());
                editor.apply();
                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
                navController.navigate(R.id.navigation_subject);
            }
        } else if (item.getItemId() == R.id.menu_custom_config) {
            if (!item.isChecked()) {
                // set the uri to Documents folder
                Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath());
                pickConfig(uri);
                item.setChecked(true);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.sp_custom_config), true);
                editor.apply();
                invalidateOptionsMenu();
                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
                navController.navigate(R.id.navigation_subject);
            } else {
                item.setChecked(false);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.sp_selected_config), "Built-in");
                editor.putBoolean(getString(R.string.sp_custom_config), false);
                editor.apply();
                invalidateOptionsMenu();
            }

            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_subject);

        } else if (item.getItemId() == R.id.menu_selected_config) {
            if (sharedPref.getBoolean(getString(R.string.sp_custom_config), false)) {
                String configPath = sharedPref.getString(getString(R.string.sp_custom_config_path), "");
                try {
                    AlertDialog dialog = getAlertDialogSelectConfigType(configPath);
                    dialog.show();
                } catch (IOException | BackingStoreException e) {
                    throw new RuntimeException(e);
                }
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

    private AlertDialog getAlertDialogSelectConfigType(String configPath) throws IOException, BackingStoreException {
        IniPreferences iniPreference = new IniPreferences(new Ini(new File(configPath)));
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

        // if the user clicks outside the dialog, select the first item in configNames
        builder.setOnCancelListener(dialog -> {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.sp_selected_config), configNames[0]);
            editor.apply();
            invalidateOptionsMenu();
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_subject);
        });
        return builder.create();
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
        // When we selected the new config file
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == 1) {
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                assert uri != null;

                // grant permissions to read the file
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                InputStream in;
                OutputStream out;
                try {
                    in = getContentResolver().openInputStream(uri);
                    File fileConfigAppFolder = new File(getFilesDir(), "config.ini");
                    out = Files.newOutputStream(fileConfigAppFolder.toPath());
                    assert in != null;
                    copyFile(in, out);
                    in.close();
                    out.flush();
                    out.close();

                    // safe the uri to shared preferences
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.sp_custom_config_path), fileConfigAppFolder.getAbsolutePath());
                    // set the option that custom config is selected
                    editor.putBoolean(getString(R.string.sp_custom_config), true);
                    editor.apply();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                // copy the body schemes from body_figures folder in the external storage next to the config file to the app folder
                File configFolder = new File(getFilesDir(), "body_figures");
                if (!configFolder.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    configFolder.mkdirs();
                }
                // take the path from the uri, extract only part of the path after the Documents folder
                String config_path_from_uri =
                        Objects.requireNonNull(uri.getPath()).substring(uri.getPath().indexOf("Documents") + 9);
                // remove the file name from the path
                config_path_from_uri = config_path_from_uri.substring(0, config_path_from_uri.lastIndexOf("/"));
                File configFolderOut = new File(
                        String.valueOf(Paths.get(String.valueOf(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOCUMENTS)), config_path_from_uri, "body_figures")));
                File[] files = configFolderOut.listFiles();
                for (File file : Objects.requireNonNull(files)) {
                    File outFile = new File(configFolder, file.getName());
                    try {
                        in = Files.newInputStream(file.toPath());
                        out = Files.newOutputStream(outFile.toPath());
                        copyFile(in, out);
                        in.close();
                        out.flush();
                        out.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                try {
                    File outFile = new File(getFilesDir(), "config.ini");
                    AlertDialog dialog = getAlertDialogSelectConfigType(String.valueOf(outFile));
                    dialog.show();
                } catch (IOException | BackingStoreException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    // run code before activity is created
    @Override
    public void onAttachedToWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            verifyStoragePermissions(this);
        }
    }
}