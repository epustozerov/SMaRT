package com.dm.smart;

import static com.dm.smart.MainActivity.sharedPref;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.dm.smart.items.Record;
import com.dm.smart.items.Step;
import com.dm.smart.ui.elements.CustomAlertDialogs;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DrawFragment extends Fragment {

    private TabLayout tabLayout;
    final Map<String, ArrayList<String>> sensationsList = new HashMap<>();
    final Map<String, Integer> colorsList = new HashMap<>();
    Map<String, List<List<Step>>> stepsList = new HashMap<>();
    public ViewPager2 viewPager;
    public ViewPagerAdapter viewPagerAdapter;
    Configuration configuration;
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the SharedViewModel
        SharedViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Observe the color and active tab index in the SharedViewModel
        sharedViewModel.getColorAndIndex().observe(this,
                colorAndIndex -> updateTabColor(colorAndIndex.second, colorAndIndex.first));
    }

    private void updateTabColor(int index, int color) {
        // Get a reference to the active tab
        TabLayout.Tab activeTab = tabLayout.getTabAt(index);
        if (activeTab != null && activeTab.getCustomView() != null) {
            Drawable drawable = activeTab.getCustomView().getBackground();
            if (drawable != null) {
                drawable.setColorFilter(dampen(color), PorterDuff.Mode.SRC_ATOP);
            }
        }
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
            }
        });

        viewPager.setAdapter(null);

        viewPager.setAdapter(viewPagerAdapter);
        tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setTabGravity(TabLayout.GRAVITY_START);

        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
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
        });
        tabLayoutMediator.attach();
        restoreTempData();
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
            Runnable runnable = () -> viewPager.setCurrentItem(newPageIndex);
            viewPager.post(runnable);
        }

        for (int i = 0; i <= newPageIndex; i++) {
            Integer tabColor = this.colorsList.get("f" + i); // Attempt to read the color from colorsList
            if (tabColor == null) {
                tabColor = colors.get(i % colors.size()); // Use a default color if not found in colorsList
            }
            updateTabColor(i, tabColor);
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
            Bitmap.Config config = Bitmap.Config.ARGB_8888;

            // Create a txt file, put allSteps in it
            StringBuilder allSteps = new StringBuilder();

            // Create a merged bitmap for each BodyDrawingView
            List<Bitmap> mergedBitmaps = new ArrayList<>();
            List<String> sensationsString = new ArrayList<>();
            List<Integer> sensationsColors = new ArrayList<>();

            for (int i = 0; i < createdWindows; i++) {
                CanvasFragment cf =
                        (CanvasFragment) viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i);
                if (cf != null) {
                    for (int j = 0; j < cf.bodyViews.length; j++) {
                        SaveSnapshotTask.doInBackground(
                                cf.bodyViews[j].snapshot, directory, baseName + "_e" + (i + 1) + "_f" + (j + 1) + ".png");

                        // Create a merged bitmap for this BodyDrawingView if it doesn't exist
                        if (mergedBitmaps.size() <= j) {
                            if (cf.bodyViews[j].snapshot != null) {
                                mergedBitmaps.add(Bitmap.createBitmap(cf.bodyViews[j].snapshot.getWidth(),
                                        cf.bodyViews[j].snapshot.getHeight(), config));
                            }
                        }

                        // Add this snapshot to the merged bitmap
                        if (cf.bodyViews[j].snapshot != null) {
                            new Canvas(mergedBitmaps.get(j)).drawBitmap(cf.bodyViews[j].snapshot, 0f, 0f, null);
                        }

                        // Form the sensationsString and sensationsColors
                        if (!cf.selectedSensations.isEmpty()) {
                            StringBuilder sensationsStringSingle = new StringBuilder();
                            for (int k = 0; k < cf.selectedSensations.size(); k++) {
                                // check if the sensation is already in sensationsString, if not, add it
                                if (!sensationsStringSingle.toString().contains(cf.selectedSensations.get(k))) {
                                    sensationsStringSingle.append(cf.selectedSensations.get(k));
                                    if (k < cf.selectedSensations.size() - 1) sensationsStringSingle.append(", ");
                                }
                            }
                            // if sensationsStringSingle is not presented in sensationsString, add it
                            if (!sensationsString.contains(sensationsStringSingle.toString())) {
                                sensationsString.add(sensationsStringSingle.toString());
                                sensationsColors.add(colors.get(i % colors.size()));
                            }
                        }

                        // Add the steps to allSteps
                        if (!cf.bodyViews[j].steps.isEmpty()) { // Only append new lines when there are steps
                            for (int k = 0; k < cf.bodyViews[j].steps.size(); k++) {
                                allSteps.append("Figure ").append(j + 1).append(" "); // Append the body figure index
                                allSteps.append(cf.selectedSensations).append(": ");
                                int intentensity = cf.bodyViews[j].steps.get(k).intensity_mark;
                                intentensity = 100 - intentensity;
                                allSteps.append(intentensity).append(" (color: ");
                                int color = cf.bodyViews[j].steps.get(k).brush.paint.getColor();
                                String color16bit = String.format("#%08X", color);
                                allSteps.append(color16bit).append(")\n");
                            }
                        }
                    }
                }
            }

            // Save the merged bitmaps and full pictures
            for (int j = 0; j < mergedBitmaps.size(); j++) {
                SaveSnapshotTask.doInBackground(mergedBitmaps.get(j), directory, baseName + "_" + (j + 1) + ".png");
                assert cf_base != null;
                Bitmap full = makeFullPicture(mergedBitmaps.get(j),
                        cf_base.bodyViews[j].backgroundImage, sensationsString, sensationsColors);
                SaveSnapshotTask.doInBackground(full, directory, baseName + "_fig_f" + (j + 1) + ".png");
            }

            // create a txt file, overwrite automatically if it exists
            File textFile = new File(directory, baseName + ".txt");
            FileWriter writer;
            try {
                writer = new FileWriter(textFile);
                writer.append(allSteps);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                // do nothing
            }
        }
        clearTempData(requireActivity());
    }


    private Step loadObjectFromFile(String absolutePath) {
        // load serialized object from file
        try {
            return (Step) new ObjectInputStream(Files.newInputStream(Paths.get(absolutePath))).readObject();
        } catch (IOException | ClassNotFoundException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            return null;
        }
    }

    Bitmap makeFullPicture(Bitmap sensations, Bitmap background, List<String> listSensations, List<Integer> colors) {
        Bitmap fullPicture = Bitmap.createBitmap(background.getWidth(),
                background.getHeight() + 100 * listSensations.size(), background.getConfig());
        Canvas canvas = new Canvas(fullPicture);
        canvas.drawBitmap(background, 0f, 0f, null);
        canvas.drawBitmap(sensations, 0f, 0f, null);
        Paint paint = new Paint();
        paint.setTextSize(80);
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

    void saveTempData() {
        File directory = new File(requireActivity().getFilesDir(), "temp");
        if (!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }

        // Save sensations
        File textFile = new File(directory, "sensations.txt");
        try {
            FileWriter writer;
            writer = new FileWriter(textFile);
            for (int i = 0; i < this.sensationsList.size(); i++) {
                writer.append("Sensation ").append(String.valueOf(i + 1)).append(": ");
                if (this.sensationsList.get("f" + i) != null) {
                    for (int j = 0; j < Objects.requireNonNull(this.sensationsList.get("f" + i)).size(); j++) {
                        writer.append(Objects.requireNonNull(this.sensationsList.get("f" + i)).get(j));
                        if (j < Objects.requireNonNull(this.sensationsList.get("f" + i)).size() - 1) {
                            writer.append(", ");
                        }
                    }
                }
                writer.append("\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException ignored) {
        }
        // Save colors
        File colorsFile = new File(directory, "colors.txt");
        try {
            FileWriter writer;
            writer = new FileWriter(colorsFile);
            for (int i = 0; i < this.colorsList.size(); i++) {
                writer.append("Color ").append(String.valueOf(i + 1)).append(": ");
                writer.append(String.valueOf(this.colorsList.get("f" + i)));
                writer.append("\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException ignored) {
        }

        File stepsDirectory = new File(directory, "steps");
        if (!stepsDirectory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            stepsDirectory.mkdirs();
        }
        // Save steps and collect filenames that should exist
        List<String> validFilenames = new ArrayList<>();
        for (Map.Entry<String, List<List<Step>>> entry : this.stepsList.entrySet()) {
            String fragmentTag = entry.getKey();
            List<List<Step>> bodyViewsSteps = entry.getValue();
            for (int j = 0; j < bodyViewsSteps.size(); j++) {
                List<Step> steps = bodyViewsSteps.get(j);
                for (int k = 0; k < steps.size(); k++) {
                    Step step = steps.get(k);
                    String filename = "steps_" + fragmentTag.substring(1) + "_" + j + "_" + k + ".ser";
                    validFilenames.add(filename);
                    saveObjectToFile(step, new File(stepsDirectory, filename).getAbsolutePath());
                }
            }
        }

        // Delete files not present in validFilenames
        File[] existingFiles = stepsDirectory.listFiles();
        if (existingFiles != null) {
            for (File file : existingFiles) {
                if (!validFilenames.contains(file.getName())) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        }
    }


    private void restoreTempData() {
        // Restore sensations from the file
        File sensationsFile = new File(requireActivity().getFilesDir(), "temp/sensations.txt");
        if (sensationsFile.exists() && sensationsFile.length() > 0) {
            // read the file and fill the sensationsList
            try {
                List<String> sensations = new ArrayList<>(Files.readAllLines(Paths.get(sensationsFile.getAbsolutePath())));
                // first open necessary amount of tabs
                for (int i = 0; i < sensations.size(); i++) {
                    createNewTab();
                    // enlarge the sensationsList
                    sensationsList.put("f" + i, new ArrayList<>());
                }

                for (int i = 0; i < sensations.size(); i++) {
                    // check if the line is not empty
                    if (sensations.get(i).split(": ").length > 1) {
                        sensationsList.put("f" + i,
                                new ArrayList<>(Arrays.asList(sensations.get(i).split(": ")[1].split(", "))));
                    }
                }
            } catch (IOException e) {
                // do nothing
            }
        } else {
            createNewTab();
        }

        File colorsFile = new File(requireActivity().getFilesDir(), "temp/colors.txt");
        if (colorsFile.exists() && colorsFile.length() > 0) {
            // read the file and fill the colorsList
            try {
                List<String> colors = new ArrayList<>(Files.readAllLines(Paths.get(colorsFile.getAbsolutePath())));
                for (int i = 0; i < colors.size(); i++) {
                    if (colors.get(i).split(": ").length > 1 && !colors.get(i).split(": ")[1].isEmpty()
                            && !colors.get(i).split(": ")[1].equals("null")) {
                        colorsList.put("f" + i, Integer.parseInt(colors.get(i).split(": ")[1]));
                    }
                }
            } catch (IOException e) {
                // do nothing
            }
        }
        // update tab colors
        for (int i = 0; i < colorsList.size(); i++) {
            if (colorsList.get("f" + i) != null) {
                try {
                    //noinspection DataFlowIssue
                    updateTabColor(i, colorsList.get("f" + i));
                } catch (NullPointerException e) {
                    // do nothing
                }
            }
        }

        File stepsFolder = new File(requireActivity().getFilesDir(), "temp/steps");
        if (!stepsFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            stepsFolder.mkdirs();
        }
        File[] files = stepsFolder.listFiles();
        //noinspection RedundantLengthCheck
        if (files != null && files.length > 0) {
            for (File file : files) {
                int canvasFragmentIndex = Integer.parseInt(file.getName().split("_")[1]);
                int bodyViewIndex = Integer.parseInt(file.getName().split("_")[2]);
                Step step = loadObjectFromFile(file.getAbsolutePath());
                if (step == null) {
                    continue;
                }
                // step.path.scaleCoordinates(1494f / 1023f, 2200f / 1267f);
                if (stepsList.containsKey("f" + canvasFragmentIndex)) {
                    if (Objects.requireNonNull(stepsList.get("f" + canvasFragmentIndex)).size() > bodyViewIndex) {
                        Objects.requireNonNull(stepsList.get("f" + canvasFragmentIndex)).get(bodyViewIndex).add(step);
                    } else {
                        Objects.requireNonNull(stepsList.get("f" + canvasFragmentIndex)).add(
                                new ArrayList<>(Collections.singletonList(step)));
                    }
                } else {
                    stepsList.put("f" + canvasFragmentIndex,
                            new ArrayList<>(Collections.singletonList(new ArrayList<>(Collections.singletonList(step)))));
                }
            }
        }
    }


    public static void clearTempData(Activity context) {
        File stepsFolder = new File(context.getFilesDir(), "temp/steps");
        if (!stepsFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            stepsFolder.mkdirs();
        }
        // check if the folder "steps" is empty, if not show a dialog proposing to proceed from the last step
        File[] files = stepsFolder.listFiles();
        assert files != null;
        for (File file1 : files) {
            // noinspection ResultOfMethodCallIgnored
            file1.delete();
        }
        File file_sensations = new File(context.getFilesDir(), "temp/sensations.txt");
        // noinspection ResultOfMethodCallIgnored
        file_sensations.delete();
        File file_colors = new File(context.getFilesDir(), "temp/colors.txt");
        // noinspection ResultOfMethodCallIgnored
        file_colors.delete();
    }

    public void saveObjectToFile(Serializable object, String filePath) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

            objectOutputStream.writeObject(object);

            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

}
