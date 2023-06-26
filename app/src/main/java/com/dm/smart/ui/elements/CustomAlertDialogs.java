package com.dm.smart.ui.elements;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.dm.smart.R;

public class CustomAlertDialogs {

    public static AlertDialog requestPassword(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View alertView = inflater.inflate(R.layout.alert_password, null);
        EditText password = alertView.findViewById(R.id.edit_text_password);
        builder.setView(alertView);
        AlertDialog dialog = builder.create();
        alertView.findViewById(R.id.button_close).setOnClickListener(v -> {
            if (password.getText().toString().equals(context.getString(R.string.password))) {
                dialog.dismiss();
            } else {
                password.setError(context.getString(R.string.wrong_password));
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    public static AlertDialog showInstructions(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View alertView =
                inflater.inflate(R.layout.alert_image, null);
        ImageView image_view_body = alertView.findViewById(R.id.image_view_body);
        image_view_body.setImageResource(R.drawable.instructions);
        builder.setView(alertView);
        AlertDialog dialog = builder.create();
        alertView.findViewById(R.id.button_close).setOnClickListener(v -> dialog.dismiss());
        return dialog;
    }

}
