package com.dm.smart;

import static com.dm.smart.MainActivity.sharedPref;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import android.view.WindowManager;
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
import com.dm.smart.ui.elements.CustomAlertDialogs;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DrawFragment extends Fragment {

    Configuration configuration;
    final Map<String, ArrayList<String>> persSensations = new HashMap<>();
    final Map<String, ArrayList<BodyDrawingView.Step>> persStepsFront = new HashMap<>();
    final Map<String, ArrayList<BodyDrawingView.Step>> persStepsBack = new HashMap<>();
    public ViewPager2 viewPager;
    public ViewPagerAdapter viewPagerAdapter;
    List<Integer> colors;
    Lifecycle lifecycle;

    public DrawFragment() {
    }

    static int dampen(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = 0.5f;
        return Color.HSVToColor(hsv);
    }

    static int[] defineMinMaxColors(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = 1.0f;
        int color_max = Color.HSVToColor(hsv);
        hsv[1] = 0.1f;
        hsv[2] = 1f;
        int color_min = Color.HSVToColor(hsv);
        return new int[]{color_max, color_min};
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

        String selectedSubjectBodyScheme = MainActivity.currentlySelectedSubject.getBodyScheme();
        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        boolean customConfig = sharedPref.getBoolean(getString(R.string.sp_custom_config), false);
        if (customConfig) {
            String configPath = sharedPref.getString(getString(R.string.sp_custom_config_path), "");
            String configName = sharedPref.getString(getString(R.string.sp_selected_config), "Built-in");
            configuration = new Configuration(configPath, configName);
            try {
                configuration.formConfig(selectedSubjectBodyScheme);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (customConfig) {
            colors = Arrays.stream(configuration.getColorSymptoms()).map(Color::parseColor).collect(Collectors.toList());
        } else {
            colors = Arrays.stream(requireActivity().getResources().
                    getIntArray(R.array.colors_symptoms)).boxed().collect(Collectors.toList());
        }
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
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateGeneralViewGlobal();
            }
        });

        viewPager.setAdapter(null);

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

        // if this is not the first tab, update the general view
        if (newPageIndex > 0) {
            Runnable runnable = () -> {
                updateGeneralViewGlobal();
                viewPager.setCurrentItem(newPageIndex);
            };
            viewPager.post(runnable);
        }

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
                            if (sharedPref.getBoolean(getString(R.string.sp_request_password), false)) {
                                android.app.AlertDialog alertDialog =
                                        CustomAlertDialogs.requestPassword(getActivity(), null, null, null);
                                alertDialog.show();
                                Objects.requireNonNull(alertDialog.getWindow()).setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                                        WindowManager.LayoutParams.MATCH_PARENT);
                            }
                        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @SuppressLint("NewApi")
    void storeData() {
        // Create a new record in the database
        long startTime = SystemClock.elapsedRealtime();
        DBAdapter DBAdapter = new DBAdapter(requireActivity());
        DBAdapter.open();
        int patient_id = MainActivity.currentlySelectedSubject.getId();
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
        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        boolean customConfig = sharedPref.getBoolean(getString(R.string.sp_custom_config), false);
        Record record;
        if (customConfig) {
            record = new Record(patient_id, configuration.getConfigName(), sensations.toString());
        } else {
            record = new Record(patient_id, "Built-in", sensations.toString());
        }

        // get the amount of records by patient specified with patient_id
        Cursor cursorRecords =
                DBAdapter.getRecordsSingleSubject(MainActivity.currentlySelectedSubject.getId());
        // get the largest n
        int recordCount = 0;
        if (cursorRecords.moveToFirst()) {
            do {
                @SuppressLint("Range") int n = cursorRecords.getInt(cursorRecords.getColumnIndex(com.dm.smart.DBAdapter.RECORD_N));
                if (n > recordCount) recordCount = n;
            } while (cursorRecords.moveToNext());
        }
        record.setN(recordCount + 1);
        DBAdapter.insertRecord(record);
        DBAdapter.close();
        long endTime = SystemClock.elapsedRealtime();
        long elapsedMilliSeconds = endTime - startTime;
        double elapsedSeconds = elapsedMilliSeconds / 1000.0;
        Log.e("STORAGE", "DB storage elapsed time: " + elapsedSeconds);

        // Create a new folder for the record
        File directory = new File(
                String.valueOf(Paths.get(String.valueOf(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOCUMENTS)), "SMaRT",
                        String.valueOf(patient_id), String.valueOf(recordCount + 1))));
        if (!directory.exists()) //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        String baseName = patient_id + "_" + (recordCount + 1);

        // Save all the images
        if (createdWindows > 0) {
            CanvasFragment cf_base =
                    (CanvasFragment) viewPagerAdapter.fragmentManager.findFragmentByTag("f" + 0);
            int height = 1494;
            int width = 2200;
            Bitmap.Config config = Bitmap.Config.ARGB_8888;

            for (int i = 0; i < createdWindows; i++) {
                CanvasFragment cf =
                        (CanvasFragment) viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i);
                if (cf != null) {
                    if (cf.bodyViewFront.snapshot != null) {
                        height = cf.bodyViewFront.snapshot.getHeight();
                        width = cf.bodyViewFront.snapshot.getWidth();
                        config = cf.bodyViewFront.snapshot.getConfig();
                        break;
                    } else if (cf.bodyViewBack.snapshot != null) {
                        height = cf.bodyViewBack.snapshot.getHeight();
                        width = cf.bodyViewBack.snapshot.getWidth();
                        config = cf.bodyViewBack.snapshot.getConfig();
                        break;
                    }
                }
            }

            // Create a txt file, put allStepsFront and allStepsBack in it
            StringBuilder allStepsFront = new StringBuilder();
            StringBuilder allStepsBack = new StringBuilder();
            for (int i = 0; i < createdWindows; i++) {
                CanvasFragment cf =
                        (CanvasFragment) viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i);
                if (cf != null) {
                    for (int j = 0; j < cf.bodyViewFront.steps.size(); j++) {
                        allStepsFront.append(cf.selectedSensations).append(": ");
                        int intentensity = cf.bodyViewFront.steps.get(j).intensity_mark;
                        intentensity = 100 - intentensity;
                        allStepsFront.append(intentensity).append(" (color: ");
                        int color = cf.bodyViewFront.steps.get(j).brush.paint.getColor();
                        String color16bit = String.format("#%08X", color);
                        allStepsFront.append(color16bit).append(")\n");
                    }
                    allStepsFront.append("\n");
                    for (int j = 0; j < cf.bodyViewBack.steps.size(); j++) {
                        allStepsBack.append(cf.selectedSensations).append(": ");
                        int intentensity = cf.bodyViewBack.steps.get(j).intensity_mark;
                        intentensity = 100 - intentensity;
                        allStepsBack.append(intentensity).append(" (color: ");
                        int color = cf.bodyViewBack.steps.get(j).brush.paint.getColor();
                        String color16bit = String.format("#%08X", color);
                        allStepsBack.append(color16bit).append(")\n");
                    }
                    allStepsBack.append("\n");
                }
            }
            // create a txt file, overwrite automatically if it exists
            File textFile = new File(directory, baseName + ".txt");
            FileWriter writer;
            try {
                writer = new FileWriter(textFile);
                writer.append("Front\n");
                writer.append(allStepsFront);
                writer.append("Back\n");
                writer.append(allStepsBack);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                // do nothing
            }

            Bitmap merged_f = Bitmap.createBitmap(width, height, config);
            Bitmap merged_b = Bitmap.createBitmap(width, height, config);
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
                SaveSnapshotTask.doInBackground(
                        cf.bodyViewFront.snapshot, directory, baseName + "_" + (i + 1) + "_f.png");
                SaveSnapshotTask.doInBackground(
                        cf.bodyViewBack.snapshot, directory, baseName + "_" + (i + 1) + "_b.png");
                if (cf.bodyViewFront.snapshot != null)
                    canvasMergedFront.drawBitmap(cf.bodyViewFront.snapshot, 0f, 0f, null);
                if (cf.bodyViewBack.snapshot != null)
                    canvasMergedBack.drawBitmap(cf.bodyViewBack.snapshot, 0f, 0f, null);
            }
            SaveSnapshotTask.doInBackground(merged_f, directory, baseName + "_f.png");
            SaveSnapshotTask.doInBackground(merged_b, directory, baseName + "_b.png");
            assert cf_base != null;
            Bitmap full_f = makeFullPicture(merged_f, cf_base.bodyViewFront.backgroundImage, sensations.toString());
            SaveSnapshotTask.doInBackground(full_f, directory, baseName + "_fig_f.png");
            Bitmap full_b = makeFullPicture(merged_b, cf_base.bodyViewBack.backgroundImage, sensations.toString());
            SaveSnapshotTask.doInBackground(full_b, directory, baseName + "_fig_b.png");
            endTime = SystemClock.elapsedRealtime();
            elapsedMilliSeconds = endTime - startTime;
            elapsedSeconds = elapsedMilliSeconds / 1000.0;
            Log.e("STORAGE", "Finalization elapsed time: " + elapsedSeconds);

            // Config
            try {
                Configuration.initDefaultConfig(requireActivity());
            } catch (Exception e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("NewApi")
    void updateGeneralViewGlobal() {
        int createdWindows = viewPagerAdapter.getItemCount();

        if (createdWindows > 0) {
            int height = 1494;
            int width = 2200;
            Bitmap.Config config = Bitmap.Config.ARGB_8888;

            for (int i = 0; i < createdWindows; i++) {
                CanvasFragment cf =
                        (CanvasFragment) viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i);
                if (cf != null) {
                    if (cf.bodyViewFront.snapshot != null) {
                        height = cf.bodyViewFront.snapshot.getHeight();
                        width = cf.bodyViewFront.snapshot.getWidth();
                        config = cf.bodyViewFront.snapshot.getConfig();
                        break;
                    } else if (cf.bodyViewBack.snapshot != null) {
                        height = cf.bodyViewBack.snapshot.getHeight();
                        width = cf.bodyViewBack.snapshot.getWidth();
                        config = cf.bodyViewBack.snapshot.getConfig();
                        break;
                    }
                }
            }

            // Create the merged image with all sensations
            Bitmap merged_f = Bitmap.createBitmap(width, height, config);
            Bitmap merged_b = Bitmap.createBitmap(width, height, config);
            Canvas canvasMergedFront = new Canvas(merged_f);
            Canvas canvasMergedBack = new Canvas(merged_b);
            for (int i = 0; i < createdWindows; i++) {
                if (viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i) == null) continue;
                CanvasFragment cf =
                        (CanvasFragment) viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i);
                assert cf != null;
                if (cf.bodyViewFront.snapshot != null)
                    canvasMergedFront.drawBitmap(cf.bodyViewFront.snapshot, 0f, 0f, null);
                if (cf.bodyViewBack.snapshot != null)
                    canvasMergedBack.drawBitmap(cf.bodyViewBack.snapshot, 0f, 0f, null);
            }

            // Update the general view of each fragment
            for (int i = 0; i < createdWindows; i++) {
                if (viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i) == null) continue;
                CanvasFragment cf =
                        (CanvasFragment) viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i);
                assert cf != null;
                cf.generalViewFront = merged_f;
                cf.generalViewBack = merged_b;
                // set front or back image
                if (cf.currentStateIsFront) {
                    Bitmap background = cf.bodyViewFront.backgroundImage;
                    Bitmap fullPicture = Bitmap.createBitmap(background.getWidth(),
                            background.getHeight() + 100, background.getConfig());
                    Canvas canvas = new Canvas(fullPicture);
                    canvas.drawBitmap(background, 0f, 0f, null);
                    canvas.drawBitmap(merged_f, 0f, 0f, null);
                    cf.buttonCompleteView.setImageBitmap(Bitmap.createScaledBitmap(fullPicture, 149, 220, true));
                } else {
                    Bitmap background = cf.bodyViewBack.backgroundImage;
                    Bitmap fullPicture = Bitmap.createBitmap(background.getWidth(),
                            background.getHeight() + 100, background.getConfig());
                    Canvas canvas = new Canvas(fullPicture);
                    canvas.drawBitmap(background, 0f, 0f, null);
                    canvas.drawBitmap(merged_b, 0f, 0f, null);
                    cf.buttonCompleteView.setImageBitmap(Bitmap.createScaledBitmap(fullPicture, 149, 220, true));
                }
            }
        }
    }

    Bitmap makeFullPicture(Bitmap sensations, Bitmap background, String text_sensations) {
        ArrayList<String> listSensations = new ArrayList<>(Arrays.asList(text_sensations.split(";")));
        Bitmap fullPicture = Bitmap.createBitmap(background.getWidth(),
                background.getHeight() + 100 * listSensations.size(), background.getConfig());
        Canvas canvas = new Canvas(fullPicture);
        canvas.drawBitmap(background, 0f, 0f, null);
        canvas.drawBitmap(sensations, 0f, 0f, null);
        Paint paint = new Paint();
        paint.setTextSize(80);
        List<Integer> colors = Arrays.stream(requireActivity().getResources().
                getIntArray(R.array.colors_symptoms)).boxed().collect(Collectors.toList());
        for (int i = 0; i < listSensations.size(); i++) {
            paint.setColor(colors.get(i));
            canvas.drawText(listSensations.get(i).trim(), 0, background.getHeight() + 100 * i, paint);
        }
        return fullPicture;
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
        super.onPause();
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
                } else {
                    // Save the empty image
                    Bitmap empty = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                    empty.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                    Log.i("SAVE_FILE", "NO");
                }
            } catch (java.io.IOException e) {
                Log.e("ERROR_SAVING", "Exception in SaveSnapshotTask", e);
            }
        }
    }

}
