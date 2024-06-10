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
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.dm.smart.R;
import com.dm.smart.SharedViewModel;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import java.io.File;
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
        View alertView = inflater.inflate(R.layout.alert_add_sensation, null);
        EditText editText = alertView.findViewById(R.id.edit_text_sensation);

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

    public static void showColorPickerDialog(Context context, int initialColor, OnColorSelectedListener onColorSelectedListener,
                                             ColorPickerClickListener onPositiveButtonClickListener) {
        ColorPickerDialogBuilder
                .with(context)
                .setTitle(R.string.dialog_choose_color)
                .initialColor(initialColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setOnColorSelectedListener(onColorSelectedListener)
                .setPositiveButton(R.string.dialog_ok, onPositiveButtonClickListener)
                .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {
                })
                .build()
                .show();
    }

    public static AlertDialog showLineWidthDialog(Context context, SharedViewModel sharedViewModel, int currentBrushThickness, OnLineWidthSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View alertView = inflater.inflate(R.layout.alert_line_width, null);

        SeekBar sbLineWidth = alertView.findViewById(R.id.sb_line_width);
        TextView tvLineWidth = alertView.findViewById(R.id.tv_line_width);

        // Set the SeekBar progress to the current brush thickness
        sbLineWidth.setProgress(currentBrushThickness - 1);
        tvLineWidth.setText(context.getString(R.string.dialog_line_width, currentBrushThickness));

        sbLineWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvLineWidth.setText(context.getString(R.string.dialog_line_width, (progress + 1)));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Set the lineWidth in the SharedViewModel
                sharedViewModel.setLineWidth(seekBar.getProgress() + 1);
            }
        });

        builder.setView(alertView);
        AlertDialog dialog = builder.create();

        alertView.findViewById(R.id.button_ok).setOnClickListener(v -> {
            dialog.dismiss();
            // Call the listener with the selected line width
            listener.onLineWidthSelected(sbLineWidth.getProgress() + 1);
        });

        dialog.show();

        return dialog;
    }


    public interface OnLineWidthSelectedListener {
        void onLineWidthSelected(int lineWidth);
    }

}
