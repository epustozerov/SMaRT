package com.dm.smart;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import androidx.fragment.app.FragmentActivity;

import org.ini4j.Ini;
import org.ini4j.IniPreferences;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class Configuration {

    private final String configPath;
    private final String configName;

    String[] bodySchemes;
    String[] selectedBodySchemes;
    String[] sensationTypes;
    private String[] colorsSymptoms;
    private String instructionsPath;
    String textScaleMax;
    String textScaleMin;


    public String getConfigName() {
        return configName;
    }

    public Configuration(String configPath, String configName) {
        this.configPath = configPath;
        this.configName = configName;
    }

    public void formConfig(FragmentActivity fragmentActivity, String selectedSubjectBodyScheme) throws IOException {
        IniPreferences iniPreference;
        ContentResolver contentResolver = fragmentActivity.getContentResolver();
        iniPreference = new IniPreferences(new Ini(contentResolver.openInputStream(Uri.parse(this.configPath))));
        this.sensationTypes = iniPreference.node(this.configName).get("sensation_types", "").split(", ");
        this.colorsSymptoms = iniPreference.node(this.configName).get("colors_symptoms", "").split(", ");
        this.bodySchemes = iniPreference.node(this.configName).get("body_schemes", "").split(", ");
        this.instructionsPath = iniPreference.node(this.configName).get("instructions", "instructions.png");
        this.textScaleMax = iniPreference.node(this.configName).get("text_scale_max", "");
        this.textScaleMin = iniPreference.node(this.configName).get("text_scale_min", "");
        this.formBodySchemes(selectedSubjectBodyScheme);
    }


    public void formBodySchemes(String bodyScheme) {
        this.selectedBodySchemes = new String[4];
        this.selectedBodySchemes[0] = "body_" + bodyScheme + "_front.png";
        this.selectedBodySchemes[1] = "body_" + bodyScheme + "_front_mask.png";
        this.selectedBodySchemes[2] = "body_" + bodyScheme + "_back.png";
        this.selectedBodySchemes[3] = "body_" + bodyScheme + "_back_mask.png";
    }

    public static void checkConfigFolder() {
        File configFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "SMaRT/config");
        if (!configFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            configFolder.mkdirs();
        }
    }

    public static void initConfig(Context context) throws IOException {
        File configFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "SMaRT/config/config.ini");
        File folderImages = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "SMaRT/config/body_figures");
        if (!configFile.exists() || !folderImages.exists() || Objects.requireNonNull(folderImages.listFiles()).length == 0) {
            // remove old config file
            if (configFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                configFile.delete();
            }
            FileWriter writer = new FileWriter(configFile);
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

            // Create folder for default body figures
            File body_figures_folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "SMaRT/config/body_figures");
            if (!body_figures_folder.exists()) {
                //noinspection ResultOfMethodCallIgnored
                body_figures_folder.mkdirs();
            }
            writer.append(config_body.toString());


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
                DrawFragment.SaveSnapshotTask.doInBackground(resourceImage, body_figures_folder, completeListOfBodyFigure + ".png");
            }

            // Add default instructions image
            writer.append("\n" +
                    "instructions = instructions.png\n");
            File instructions = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "SMaRT/config/instructions.png");
            @SuppressLint("DiscouragedApi") final int resourceId = context.getResources().getIdentifier("instructions",
                    "drawable", context.getPackageName());
            Bitmap resourceImage = BitmapFactory.decodeResource(context.getResources(), resourceId);
            DrawFragment.SaveSnapshotTask.doInBackground(resourceImage, instructions.getParentFile(), instructions.getName());

            // Add default text labels for the scale
            writer.append("text_scale_max = max\n" + "text_scale_min = min");

            writer.flush();
            writer.close();
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
