package com.dm.smart.ui.elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.dm.smart.R;
import com.dm.smart.SharedViewModel;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;


public class CustomAlertDialogs {

    public static AlertDialog requestPassword(Activity context, SharedPreferences sharedPref, String pref, android.view.MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View alertView = inflater.inflate(R.layout.alert_password, null);
        EditText password = alertView.findViewById(R.id.edit_text_password);
        builder.setView(alertView);
        AlertDialog dialog = builder.create();
        alertView.findViewById(R.id.button_close).setOnClickListener(v -> {
            if (password.getText().toString().equals(context.getString(R.string.password))) {
                dialog.dismiss();
                if (item != null) {
                    item.setChecked(!item.isChecked());
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean(pref, item.isChecked());
                    editor.apply();
                    NavController navController = Navigation.findNavController(context, R.id.nav_host_fragment_activity_main);
                    if (Objects.requireNonNull(navController.getCurrentDestination()).getId() == R.id.navigation_subject)
                        navController.navigate(R.id.navigation_subject);
                }
            } else {
                password.setError(context.getString(R.string.wrong_password));
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    public static AlertDialog showInstructions(Context context, boolean customConfig, File instructionsPath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View alertView =
                inflater.inflate(R.layout.alert_image, null);
        if (!customConfig) {
            ImageView imageInstructions = alertView.findViewById(R.id.image_view_body);
            imageInstructions.setImageResource(R.drawable.instructions);
        } else {
            Bitmap bitmap = BitmapFactory.decodeFile(String.valueOf(instructionsPath));
            ImageView imageInstructions = alertView.findViewById(R.id.image_view_body);
            imageInstructions.setImageBitmap(bitmap);
        }
        builder.setView(alertView);
        AlertDialog dialog = builder.create();
        alertView.findViewById(R.id.button_close).setOnClickListener(v -> dialog.dismiss());
        return dialog;
    }

    public static AlertDialog showGeneralView(Context context, Bitmap bitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View alertView =
                inflater.inflate(R.layout.alert_image, null);
        ImageView image_view_body = alertView.findViewById(R.id.image_view_body);
        Drawable d = new BitmapDrawable(context.getResources(), bitmap);
        image_view_body.setBackground(d);
        builder.setView(alertView);
        AlertDialog dialog = builder.create();
        alertView.findViewById(R.id.button_close).setOnClickListener(v -> dialog.dismiss());
        return dialog;
    }

    public static String showAddSensationDialog(Context context, SharedViewModel sharedViewModel) {
        final String[] inputText = new String[1];
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View alertView = inflater.inflate(R.layout.alert_add_sensation, null); // Assuming you have a layout file named alert_add_sensation
        EditText editText = alertView.findViewById(R.id.edit_text_sensation); // Assuming you have an EditText with id edit_text_sensation in your layout

        builder.setView(alertView);
        AlertDialog dialog = builder.create();

        alertView.findViewById(R.id.button_ok).setOnClickListener(v -> {
            inputText[0] = editText.getText().toString();
            sharedViewModel.setSensationAddedFromDialog(true);
            sharedViewModel.selectSensation(inputText[0]);
            dialog.dismiss();
        });

        alertView.findViewById(R.id.button_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        return inputText[0];
    }
}
