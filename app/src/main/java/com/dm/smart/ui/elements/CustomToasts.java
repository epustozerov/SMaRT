package com.dm.smart.ui.elements;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dm.smart.R;

public class CustomToasts {


    public static Toast showToast(Context context, String text_to_show) {
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        LayoutInflater inflater_toast = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View toast_view = inflater_toast.inflate(R.layout.toast_bordered, null);
        TextView text = toast_view.findViewById(R.id.toast_text);
        text.setText(text_to_show);
        toast.setView(toast_view);
        return toast;
    }
}
