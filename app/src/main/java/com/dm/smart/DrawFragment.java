package com.dm.smart;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTabHost;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


@SuppressWarnings("deprecation")
public class DrawFragment extends Fragment implements View.OnClickListener {

    FragmentTabHost mTabHost;
    List<Integer> colors;

    static int dampen(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = 0.55f;
        return Color.HSVToColor(hsv);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View mView = inflater.inflate(R.layout.fragment_draw, container, false);
        colors = Arrays.stream(requireActivity().getResources().
                getIntArray(R.array.colors_symptoms)).boxed().collect(Collectors.toList());
        Button button_add_sensation = mView.findViewById(R.id.button_add_sensation);
        button_add_sensation.setOnClickListener(this);
        mTabHost = mView.findViewById(android.R.id.tabhost);
        mTabHost.setup(requireActivity(), getParentFragmentManager(), android.R.id.tabcontent);
        Button button_recording_completed = mView.findViewById(R.id.button_recording_completed);
        button_recording_completed.setOnClickListener(view -> showDrawingDoneDialog());
        TextView textview_no_sensations_recorded = mView.findViewById(R.id.textview_no_sensations_recorded);
        textview_no_sensations_recorded.setOnClickListener(this);
        return mView;
    }

    @Override
    public void onClick(View v) {

        int newTabIndex = mTabHost.getTabWidget().getTabCount();
        int color = colors.get(newTabIndex % colors.size());

        if (newTabIndex == 0) {
            ((FrameLayout) requireView().findViewById(android.R.id.tabcontent)).removeAllViews();
        }

        TextView tab_new_sensation = new TextView(getActivity());
        tab_new_sensation.setTextColor(Color.BLACK);
        tab_new_sensation.setText(String.format("%s %s", getResources().getString(R.string.sensation), newTabIndex + 1));
        tab_new_sensation.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
        tab_new_sensation.setTypeface(Typeface.DEFAULT_BOLD);
        Drawable d = ResourcesCompat.getDrawable(requireActivity().getResources(), R.drawable.tab_indicator, null);

        assert d != null;
        d.setColorFilter(dampen(color), PorterDuff.Mode.SRC_ATOP);
        tab_new_sensation.setBackground(d);

        Bundle b = new Bundle();
        b.putInt("tabIndex", newTabIndex);
        b.putInt("color", color);

        //noinspection deprecation
        mTabHost.addTab(mTabHost.newTabSpec(
                        String.format(Locale.ENGLISH, "tab%d", newTabIndex))
                .setIndicator(tab_new_sensation), CanvasFragment.class, b);

        mTabHost.setCurrentTab(newTabIndex);
    }

    public void showDrawingDoneDialog() {
        String confirm = "Recording completed";
        String positive = "Confirm";
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(confirm).setPositiveButton(positive, (dialog, id) -> {
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}