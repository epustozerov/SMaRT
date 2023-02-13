package com.dm.smart;

import android.graphics.Bitmap;
import android.graphics.Canvas;
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

import com.dm.smart.items.Record;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DrawFragment extends Fragment {

    ViewPager2 viewPager;

    ViewPagerAdapter viewPagerAdapter;
    List<Integer> colors;
    Lifecycle lifecycle;

    static int dampen(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = 0.55f;
        return Color.HSVToColor(hsv);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                //noinspection ConstantConditions
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
        super.onViewCreated(view, savedInstanceState);

        viewPager = view.findViewById(R.id.view_pager);
        viewPager.setUserInputEnabled(false);
        viewPager.setOffscreenPageLimit(10);
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(viewPagerAdapter);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setTabGravity(TabLayout.GRAVITY_START);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            TextView tab_new_sensation = new TextView(getActivity());
            tab_new_sensation.setTextColor(Color.BLACK);
            tab_new_sensation.setText(String.format("%s %s",
                    getResources().getString(R.string.sensation), position + 1));
            tab_new_sensation.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
            tab_new_sensation.setTypeface(Typeface.DEFAULT_BOLD);
            Drawable d = ResourcesCompat.getDrawable(requireActivity().getResources(),
                    R.drawable.tab_indicator, null);
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void showDrawingDoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(getResources().getString(R.string.dialog_drawing_completed)).
                setPositiveButton(getResources().getString(R.string.dialog_drawing_confirmed),
                        (dialog, id) -> {
                            storeData();
                            Navigation.findNavController(requireActivity(),
                                            R.id.nav_host_fragment_activity_main).
                                    navigate(R.id.navigation_patient);
                        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void storeData() {

        // Create a new record in the database
        DBAdapter DBAdapter = new DBAdapter(requireActivity());
        DBAdapter.open();
        int patient_id = MainActivity.currentlySelectedSubject.getId();
        String patient_name = MainActivity.currentlySelectedSubject.getName();
        int createdWindows = viewPagerAdapter.getItemCount();
        StringBuilder sensations = new StringBuilder();
        for (int i = 0; i < createdWindows; i++) {
            CanvasFragment cf =
                    (CanvasFragment) viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i);
            assert cf != null;
            for (int j = 0; j < cf.selectedSensations.size(); j++) {
                sensations.append(cf.selectedSensations.get(j));
                if (j < cf.selectedSensations.size() - 1)
                    sensations.append(", ");
            }
            sensations.append("; ");
        }

        Record record = new Record(patient_id, sensations.toString());
        long record_id = DBAdapter.insertRecord(record);
        DBAdapter.close();

        // Create a new folder for the record
        File directory = new File(
                String.valueOf(Paths.get(String.valueOf(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOCUMENTS)), "SMaRT",
                        patient_id + " " + patient_name, String.valueOf(record_id))));
        if (!directory.exists()) //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();

        // Save all the images
        if (createdWindows > 0) {
            // Merge images into one
            CanvasFragment cf_base =
                    (CanvasFragment) viewPagerAdapter.fragmentManager.findFragmentByTag("f" + 0);
            assert cf_base != null;
            Bitmap merged = Bitmap.createBitmap(cf_base.bodyViewFront.snapshot.getWidth(),
                    cf_base.bodyViewFront.snapshot.getHeight(), cf_base.bodyViewFront.snapshot.getConfig());
            Canvas canvasMergedFront = new Canvas(merged);
            Canvas canvasMergedBack = new Canvas(merged);
            for (int i = 0; i < createdWindows; i++) {
                CanvasFragment cf =
                        (CanvasFragment) viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i);
                assert cf != null;
                StringBuilder file_name_sensations = new StringBuilder();
                for (int j = 0; j < cf.selectedSensations.size(); j++) {
                    file_name_sensations.append(cf.selectedSensations.get(j)).append("_");
                }
                SaveSnapshotTask.doInBackground(
                        cf.bodyViewFront.snapshot, directory, i + "_" + file_name_sensations + "f.png");
                SaveSnapshotTask.doInBackground(
                        cf.bodyViewBack.snapshot, directory, i + "_" + file_name_sensations + "b.png");
                if (cf.bodyViewFront.snapshot != null)
                    canvasMergedFront.drawBitmap(cf.bodyViewFront.snapshot, 0f, 0f, null);
                if (cf.bodyViewBack.snapshot != null)
                    canvasMergedBack.drawBitmap(cf.bodyViewBack.snapshot, 0f, 0f, null);
            }
            SaveSnapshotTask.doInBackground(merged, directory, "merged_f.png");
            SaveSnapshotTask.doInBackground(merged, directory, "merged_b.png");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        Log.e("DEBUG", "OnPause of HomeFragment");
        super.onPause();
        viewPager.setAdapter(null);
    }

    @SuppressWarnings("deprecation")
    abstract static class SaveSnapshotTask extends AsyncTask<Bitmap, String, Void> {
        protected static void doInBackground(Bitmap figure, File directory, String name) {
            File photo = new File(directory, name);
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
