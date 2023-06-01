package com.dm.smart;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.ArrayList;
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
        if (viewPagerAdapter == null) {
            viewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager(), lifecycle);
        }

        viewPager = view.findViewById(R.id.view_pager);
        viewPager.setUserInputEnabled(false);
        viewPager.setOffscreenPageLimit(10);
        for (int i = 0; i < viewPagerAdapter.fragmentManager.getFragments().size(); i++) {
            Fragment f = viewPagerAdapter.fragmentManager.getFragments().get(i);
            if (f != null) {
                Log.e("FRAGMENTS", "Current tag:" + f.getTag() + ", iteration: " + i);
            }
        }
        Log.e("VIEPAGERADAPTER", "tabs" + viewPagerAdapter.getItemCount());
        viewPager.setAdapter(null);
        for (int i = 0; i < viewPagerAdapter.fragmentManager.getFragments().size(); i++) {
            Fragment f = viewPagerAdapter.fragmentManager.getFragments().get(i);
            if (f != null) {
                Log.e("FRAGMENTS", "Current tag:" + f.getTag() + ", iteration: " + i);
            }
        }
        Log.e("VIEPAGERADAPTER", "tabs" + viewPagerAdapter.getItemCount());

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
        long startTime = SystemClock.elapsedRealtime();
        int newPageIndex = viewPagerAdapter.getItemCount();
        int color = colors.get(newPageIndex % colors.size());
        Bundle b = new Bundle();
        b.putInt("tabIndex", newPageIndex);
        b.putInt("color", color);
        viewPagerAdapter.add(b);
        viewPagerAdapter.notifyItemChanged(newPageIndex);
        viewPager.setCurrentItem(newPageIndex);
        long endTime = SystemClock.elapsedRealtime();
        long elapsedMilliSeconds = endTime - startTime;
        double elapsedSeconds = elapsedMilliSeconds / 1000.0;
        Log.e("NEW TAB", "New tab creation elapsed time: " + elapsedSeconds);
    }

    public void showDrawingDoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(getResources().getString(R.string.dialog_drawing_completed)).
                setPositiveButton(getResources().getString(R.string.dialog_drawing_confirmed),
                        (dialog, id) -> {
                            Navigation.findNavController(requireActivity(),
                                            R.id.nav_host_fragment_activity_main).
                                    navigate(R.id.navigation_subject);
                            Runnable runnable = this::storeData;
                            new Thread(runnable).start();
                        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    void storeData() {

        // Create a new record in the database
        long startTime = SystemClock.elapsedRealtime();
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
        Log.e("THESE ARE THE SENSATIONS WE HAVE:", sensations.toString());
        Record record = new Record(patient_id, sensations.toString());
        long record_id = DBAdapter.insertRecord(record);
        DBAdapter.close();
        long endTime = SystemClock.elapsedRealtime();
        long elapsedMilliSeconds = endTime - startTime;
        double elapsedSeconds = elapsedMilliSeconds / 1000.0;
        Log.e("STORAGE", "DB storage elapsed time: " + elapsedSeconds);

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
            Bitmap merged_f;
            Bitmap merged_b;

            assert cf_base != null;
            if (cf_base.bodyViewFront.snapshot != null) {
                merged_f = Bitmap.createBitmap(cf_base.bodyViewFront.snapshot.getWidth(),
                        cf_base.bodyViewFront.snapshot.getHeight(), cf_base.bodyViewFront.snapshot.getConfig());
            } else {
                merged_f = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            }
            if (cf_base.bodyViewBack.snapshot != null) {
                merged_b = Bitmap.createBitmap(cf_base.bodyViewBack.snapshot.getWidth(),
                        cf_base.bodyViewBack.snapshot.getHeight(), cf_base.bodyViewBack.snapshot.getConfig());
            } else {
                merged_b = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            }

            Canvas canvasMergedFront = new Canvas(merged_f);
            Canvas canvasMergedBack = new Canvas(merged_b);

            endTime = SystemClock.elapsedRealtime();
            elapsedMilliSeconds = endTime - startTime;
            elapsedSeconds = elapsedMilliSeconds / 1000.0;
            Log.e("STORAGE", "Storage init elapsed time: " + elapsedSeconds);

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
                endTime = SystemClock.elapsedRealtime();
                elapsedMilliSeconds = endTime - startTime;
                elapsedSeconds = elapsedMilliSeconds / 1000.0;
                Log.e("STORAGE", "Particular canvas storage elapsed time: " + elapsedSeconds);
            }
            SaveSnapshotTask.doInBackground(merged_f, directory, "merged_sensations_f.png");
            SaveSnapshotTask.doInBackground(merged_b, directory, "merged_sensations_b.png");
            Bitmap full_f = make_full_picture(merged_f, cf_base.bodyViewFront.backgroundImage, sensations.toString());
            SaveSnapshotTask.doInBackground(full_f, directory, "complete_picture_f.png");
            Bitmap full_b = make_full_picture(merged_b, cf_base.bodyViewBack.backgroundImage, sensations.toString());
            SaveSnapshotTask.doInBackground(full_b, directory, "complete_picture_b.png");
            endTime = SystemClock.elapsedRealtime();
            elapsedMilliSeconds = endTime - startTime;
            elapsedSeconds = elapsedMilliSeconds / 1000.0;
            Log.e("STORAGE", "Finalization elapsed time: " + elapsedSeconds);
        }
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

    Bitmap make_full_picture(Bitmap sensations, Bitmap background, String text_sensations) {
        ArrayList<String> list_sensations = new ArrayList<>(Arrays.asList(text_sensations.split(";")));
        Bitmap full_picture = Bitmap.createBitmap(background.getWidth(),
                background.getHeight() + 100 * list_sensations.size(), background.getConfig());
        Canvas canvas = new Canvas(full_picture);
        canvas.drawBitmap(background, 0f, 0f, null);
        canvas.drawBitmap(sensations, 0f, 0f, null);
        Paint paint = new Paint();
        paint.setTextSize(80);
        List<Integer> colors = Arrays.stream(requireActivity().getResources().
                getIntArray(R.array.colors_symptoms)).boxed().collect(Collectors.toList());
        for (int i = 0; i < list_sensations.size(); i++) {
            paint.setColor(colors.get(i));
            canvas.drawText(list_sensations.get(i).trim(), 0, background.getHeight() + 100 * i, paint);
        }
        return full_picture;
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        Log.e("DEBUG", "OnPause of DrawFragment");

        for (int i = 0; i < viewPagerAdapter.fragmentManager.getFragments().size(); i++) {
            Fragment f = viewPagerAdapter.fragmentManager.getFragments().get(i);
            if (f != null) {
                Log.e("FRAGMENTS", "Current tag:" + f.getTag() + ", iteration: " + i);
            }
        }
        Log.e("DEBUG", "OnPause of DrawFragment before I detach all CanvasFragments");
        if (viewPagerAdapter.getItemCount() > 0) {
            for (int i = 0; i < viewPagerAdapter.getItemCount(); i++) {
                CanvasFragment cf =
                        (CanvasFragment) viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i);
                assert cf != null;
                viewPagerAdapter.fragmentManager.beginTransaction().detach(cf).commit();
            }
        }
        // getChildFragmentManager().beginTransaction().detach(this).commit();
        Log.e("DEBUG", "OnPause of DrawFragment before super");
        super.onPause();
        Log.e("DEBUG", "OnPause of DrawFragment after super");
    }

    @Override
    public void onDestroy() {
        Log.e("DEBUG", "OnDestroy of DrawFragment");
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        Log.e("DEBUG", "OnDestroyView of DrawFragment");
        for (int i = 0; i < viewPagerAdapter.fragmentManager.getFragments().size(); i++) {
            Fragment f = viewPagerAdapter.fragmentManager.getFragments().get(i);
            if (f != null) {
                Log.e("FRAGMENTS", "Current tag:" + f.getTag() + ", iteration: " + i);
            }
        }
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        Log.e("DEBUG", "OnDetach of DrawFragment");
        super.onDetach();
    }

    @Override
    public void onStop() {

        Log.e("DEBUG", "OnStop of DrawFragment");
        for (int i = 0; i < viewPagerAdapter.fragmentManager.getFragments().size(); i++) {
            Fragment f = viewPagerAdapter.fragmentManager.getFragments().get(i);
            if (f != null) {
                Log.e("FRAGMENTS", "Current tag:" + f.getTag() + ", iteration: " + i);
            }
        }
        super.onStop();
        for (int i = 0; i < viewPagerAdapter.fragmentManager.getFragments().size(); i++) {
            Fragment f = viewPagerAdapter.fragmentManager.getFragments().get(i);
            if (f != null) {
                Log.e("FRAGMENTS", "Current tag:" + f.getTag() + ", iteration: " + i);
            }
        }
    }

    @Override
    public void onResume() {
        Log.e("DEBUG", "OnResume of DrawFragment");
        super.onResume();
        viewPager.setAdapter(viewPagerAdapter);
    }

    @Override
    public void onStart() {
        Log.e("DEBUG", "OnStart of DrawFragment");
        super.onStart();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        Log.e("DEBUG", "OnAttach of DrawFragment");
        super.onAttach(context);
    }


}
