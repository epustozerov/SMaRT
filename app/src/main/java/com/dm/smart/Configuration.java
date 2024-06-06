package com.dm.smart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import org.ini4j.Ini;
import org.ini4j.IniPreferences;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class Configuration {

    private final String configPath;
    private final String configName;

    String[] bodySchemes;

    String[] bodyViews;
    String[] selectedBodyViews;
    String[] selectedBodyMasks;
    String[] sensationTypes;
    String textScaleMax;
    String textScaleMin;
    private String[] colorsSymptoms;
    private String instructionsPath;


    public Configuration(String configPath, String configName) {
        this.configPath = configPath;
        this.configName = configName;
    }

    public static void initDefaultConfig(Context context) {

        File configFolder = context.getFilesDir();
        File configFile = new File(configFolder, "config.ini");
        if (!configFile.exists()) {
            createConfigFile(context, configFolder, configFile);
        }
        // Create the copy of the default config file in the external storage
        File configFolderOut = new File(
                String.valueOf(Paths.get(String.valueOf(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS)), "SMaRT", "config_sample")));
        if (!configFolderOut.exists()) {
            //noinspection ResultOfMethodCallIgnored
            configFolderOut.mkdirs();
        }
        File configFileOut = new File(configFolderOut, "config.ini");
        if (!configFileOut.exists()) {
            createConfigFile(context, configFolderOut, configFileOut);
        }
    }

    private static void createConfigFile(Context context, File configFolder, File configFile) {

        // Create folder for default body figures
        File bodyFiguresFolder = new File(configFolder,
                "body_figures");
        if (!bodyFiguresFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            bodyFiguresFolder.mkdirs();
        }

        StringBuilder config_body = new StringBuilder("[Default]\n" +
                "sensation_types = ");

        // Add default sensations from the sting list stored in string.xml file
        String[] sensations = context.getResources().getStringArray(R.array.sensation_types);
        for (String sensation : sensations) {
            config_body.append(sensation).append(", ");
        }
        config_body.deleteCharAt(config_body.length() - 2);
        config_body.deleteCharAt(config_body.length() - 1);

        // Add default colors to the config file
        config_body.append("\n" +
                "colors_symptoms = ");
        List<Integer> colors = Arrays.stream(context.getResources().
                getIntArray(R.array.colors_symptoms)).boxed().collect(Collectors.toList());
        for (Integer color : colors) {
            String hex = String.format("#%06X", (0xFFFFFF & color));
            config_body.append(hex).append(", ");
        }
        config_body.deleteCharAt(config_body.length() - 2);
        config_body.deleteCharAt(config_body.length() - 1);

        // Iterate through schemes from body_figures.xml
        config_body.append("\n" +
                "body_schemes = ");
        List<String> body_figures = Arrays.stream(context.getResources().
                getStringArray(R.array.body_figures)).collect(Collectors.toList());
        for (String body_figure : body_figures) {
            body_figure = body_figure.substring(body_figure.lastIndexOf("_") + 1);
            config_body.append(body_figure).append(", ");
        }
        config_body.deleteCharAt(config_body.length() - 2);
        config_body.deleteCharAt(config_body.length() - 1);

        config_body.append("\n" +
                "body_views = ");
        List<String> body_views = Arrays.stream(context.getResources().
                getStringArray(R.array.body_views)).collect(Collectors.toList());
        for (String body_view : body_views) {
            config_body.append(body_view).append(", ");
        }
        config_body.deleteCharAt(config_body.length() - 2);
        config_body.deleteCharAt(config_body.length() - 1);

        config_body.append("\n" +
                "instructions = instructions.png\n");
        config_body.append("text_scale_max = max\n" + "text_scale_min = min");

        String[] completeListOfBodyFigures = new String[12];
        completeListOfBodyFigures[0] = "body_female_front";
        completeListOfBodyFigures[1] = "body_female_front_mask";
        completeListOfBodyFigures[2] = "body_female_back";
        completeListOfBodyFigures[3] = "body_female_back_mask";
        completeListOfBodyFigures[4] = "body_male_front";
        completeListOfBodyFigures[5] = "body_male_front_mask";
        completeListOfBodyFigures[6] = "body_male_back";
        completeListOfBodyFigures[7] = "body_male_back_mask";
        completeListOfBodyFigures[8] = "body_neutral_front";
        completeListOfBodyFigures[9] = "body_neutral_front_mask";
        completeListOfBodyFigures[10] = "body_neutral_back";
        completeListOfBodyFigures[11] = "body_neutral_back_mask";

        for (String completeListOfBodyFigure : completeListOfBodyFigures) {
            Resources resources = context.getResources();
            @SuppressLint("DiscouragedApi") final int resourceId = resources.getIdentifier(completeListOfBodyFigure, "drawable",
                    context.getPackageName());
            Bitmap resourceImage = BitmapFactory.decodeResource(context.getResources(), resourceId);
            DrawFragment.SaveSnapshotTask.doInBackground(resourceImage, bodyFiguresFolder, completeListOfBodyFigure + ".png");
        }


        File instructions = new File(configFolder,
                "instructions.png");
        @SuppressLint("DiscouragedApi") final int resourceId = context.getResources().getIdentifier("instructions",
                "drawable", context.getPackageName());
        Bitmap resourceImage = BitmapFactory.decodeResource(context.getResources(), resourceId);
        DrawFragment.SaveSnapshotTask.doInBackground(resourceImage, instructions.getParentFile(), instructions.getName());

        FileWriter writer;
        try {
            // check if config file exists
            if (!configFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                configFile.createNewFile();
            }
            writer = new FileWriter(configFile);
            writer.append(config_body.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    public String getConfigName() {
        return configName;
    }

    public void formConfig(String selectedSubjectBodyScheme) throws IOException {
        IniPreferences iniPreference;
        iniPreference = new IniPreferences(new Ini(new File(this.configPath)));
        this.sensationTypes = iniPreference.node(this.configName).get("sensation_types", "").split(", ");
        this.colorsSymptoms = iniPreference.node(this.configName).get("colors_symptoms", "").split(", ");
        this.bodySchemes = iniPreference.node(this.configName).get("body_schemes", "").split(", ");
        this.bodyViews = iniPreference.node(this.configName).get("body_views", "").split(", ");
        this.instructionsPath = iniPreference.node(this.configName).get("instructions", "instructions.png");
        this.textScaleMax = iniPreference.node(this.configName).get("text_scale_max", "");
        this.textScaleMin = iniPreference.node(this.configName).get("text_scale_min", "");
        this.formBodySchemes(selectedSubjectBodyScheme);
    }

    public void formBodySchemes(String bodyScheme) {
        // Initialize the selectedBodyViews and selectedBodyMasks arrays
        selectedBodyViews = new String[bodyViews.length];
        selectedBodyMasks = new String[bodyViews.length];

        // Iterate over the bodyViews array
        for (int i = 0; i < bodyViews.length; i++) {
            // For each body view, create the corresponding body figure and mask names
            selectedBodyViews[i] = "body_" + bodyScheme + "_" + bodyViews[i];
            selectedBodyMasks[i] = selectedBodyViews[i] + "_mask";
        }
    }

    public String[] getColorSymptoms() {
        return colorsSymptoms;
    }

    public String[] getBodySchemes() {
        return bodySchemes;
    }

    public String getTextMin() {
        return textScaleMin;
    }

    public String getTextMax() {
        return textScaleMax;
    }

    public String getInstructionsPath() {
        return instructionsPath;

    }
}
