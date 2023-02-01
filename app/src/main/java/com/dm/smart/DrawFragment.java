package com.dm.smart;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DrawFragment extends Fragment {

    ViewPagerAdapter viewPagerAdapter;
    ViewPager2 viewPager;
    List<Integer> colors;
    Lifecycle lifecycle;

    static int dampen(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = 0.55f;
        return Color.HSVToColor(hsv);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {


        lifecycle = new Lifecycle() {
            @Override
            public void addObserver(@NonNull LifecycleObserver observer) {

            }

            @Override
            public void removeObserver(@NonNull LifecycleObserver observer) {

            }

            @NonNull
            @Override
            public State getCurrentState() {
                return null;
            }
        };

        View mView = inflater.inflate(R.layout.fragment_draw, container, false);
        colors = Arrays.stream(requireActivity().getResources().
                getIntArray(R.array.colors_symptoms)).boxed().collect(Collectors.toList());
        Button button_add_sensation = mView.findViewById(R.id.button_add_sensation);
        button_add_sensation.setOnClickListener(view -> createNewTab());
        Button button_recording_completed = mView.findViewById(R.id.button_recording_completed);
        button_recording_completed.setOnClickListener(view -> showDrawingDoneDialog());
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewPagerAdapter = new ViewPagerAdapter(getParentFragmentManager(), lifecycle);
        // viewPagerAdapter.setHasStableIds(true);
        viewPager = view.findViewById(R.id.view_pager);
        viewPager.setUserInputEnabled(false);
        viewPager.setOffscreenPageLimit(10);
        viewPager.setAdapter(viewPagerAdapter);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setTabGravity(TabLayout.GRAVITY_START);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            TextView tab_new_sensation = new TextView(getActivity());
            tab_new_sensation.setTextColor(Color.BLACK);
            tab_new_sensation.setText(String.format("%s %s", getResources().getString(R.string.sensation), position + 1));
            tab_new_sensation.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
            tab_new_sensation.setTypeface(Typeface.DEFAULT_BOLD);
            Drawable d = ResourcesCompat.getDrawable(requireActivity().getResources(), R.drawable.tab_indicator, null);
            int color = colors.get(position % colors.size());
            assert d != null;
            d.setColorFilter(dampen(color), PorterDuff.Mode.SRC_ATOP);
            tab_new_sensation.setBackground(d);
            tab.setCustomView(tab_new_sensation);
        }).attach();
        createNewTab();
    }

    void createNewTab() {
        int newPageIndex = viewPagerAdapter.getItemCount();
        int color = colors.get(newPageIndex % colors.size());
        Bundle b = new Bundle();
        b.putInt("tabIndex", newPageIndex);
        b.putInt("color", color);
        viewPagerAdapter.add(b);
        viewPagerAdapter.notifyItemChanged(newPageIndex);
        viewPager.setCurrentItem(newPageIndex);
    }


    public void showDrawingDoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(getResources().getString(R.string.dialog_drawing_completed)).
                setPositiveButton(getResources().getString(R.string.dialog_drawing_confirmed),
                        (dialog, id) -> {
                            // safe all the stuff
                            // erase all the images
                            storeData();
                            Navigation.findNavController(requireActivity(),
                                            R.id.nav_host_fragment_activity_main).
                                    navigate(R.id.navigation_patient);
                        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    void storeData() {
        int createdWindows = viewPagerAdapter.getItemCount();
        for (int i = 0; i < createdWindows; i++) {
            CanvasFragment cf = (CanvasFragment) viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i);
            assert cf != null;
            DrawFragment.SaveSnapshotTask.doInBackground(cf.bodyViewFront.snapshot, i + "_f.png");
            DrawFragment.SaveSnapshotTask.doInBackground(cf.bodyViewBack.snapshot, i + "_b.png");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    abstract static class SaveSnapshotTask extends AsyncTask<Bitmap, String, Void> {
        protected static void doInBackground(Bitmap figure, String name) {
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "SMaRT");
            if (!directory.exists()) {
                boolean mkdirs = directory.mkdirs();
                Log.i("DIRECTORY", String.valueOf(mkdirs));
            }
            File photo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/SMaRT", name);
            try {
                FileOutputStream fos = new FileOutputStream(photo.getPath());
                if (figure != null) {
                    figure.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                    Log.i("SAVE_FILE", "YES");
                }
            } catch (java.io.IOException e) {
                Log.e("ERROR_SAVING", "Exception in SaveSnapshotTask", e);
            }
        }
    }
}
