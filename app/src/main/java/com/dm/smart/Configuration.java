package com.dm.smart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Configuration {

    public static void checkConfigFolder() {
        File configFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "SMaRT/config");
        if (!configFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            configFolder.mkdirs();
        }
    }

    public static void checkConfigFile(Context context) throws IOException {
        File configFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "SMaRT/config/config.ini");
        if (!configFile.exists()) {
            FileWriter writer = new FileWriter(configFile);
            StringBuilder config_body = new StringBuilder("[General]\n" +
                    "sensation_types = ");

            // Add default sensations from the sting list stored in string.xml file
            String[] sensations = context.getResources().getStringArray(R.array.sensation_types);
            for (String sensation : sensations) {
                config_body.append(sensation).append(",");
            }

            // Add default colors to the config file
            config_body.append("\n" +
                    "colors_symptoms = ");
            List<Integer> colors = Arrays.stream(context.getResources().
                    getIntArray(R.array.colors_symptoms)).boxed().collect(Collectors.toList());
            for (Integer color : colors) {
                String hex = String.format("#%06X", (0xFFFFFF & color));
                config_body.append(hex).append(",");
            }

            // Iterate through schemes from body_figures.xml
            config_body.append("\n" +
                    "body_figures = ");
            Log.e("Config 1", config_body.toString());
            List<String> body_figures = Arrays.stream(context.getResources().
                    getStringArray(R.array.schemes)).collect(Collectors.toList());
            File body_figures_folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "SMaRT/config/body_figures");
            if (!body_figures_folder.exists()) {
                //noinspection ResultOfMethodCallIgnored
                body_figures_folder.mkdirs();
            }
            for (String body_figure : body_figures) {
                @SuppressLint("DiscouragedApi") List<String> body_figure_parts = Arrays.stream(context.getResources().
                        getStringArray(context.getResources().getIdentifier(body_figure,
                                "array", context.getPackageName()))).collect(Collectors.toList());
                @SuppressLint({"DiscouragedApi", "Recycle"}) TypedArray body_figure_parts2 =
                        context.getResources().obtainTypedArray(context.getResources().
                                getIdentifier(body_figure, "array", context.getPackageName()));
                for (int i = 0; i < body_figure_parts.size(); i++) {
                    String body_figure_part = body_figure_parts.get(i).substring(body_figure_parts.get(i).lastIndexOf("/") + 1);
                    config_body.append(body_figure_part).append(",");
                    int id = body_figure_parts2.getResourceId(i, 0);
                    Log.e("IDS from resourses", String.valueOf(id));
                    Bitmap resourceImage = BitmapFactory.decodeResource(context.getResources(), id);
                    DrawFragment.SaveSnapshotTask.doInBackground(resourceImage, body_figures_folder, body_figure_part);
                }
            }
            writer.append(config_body.toString());
            writer.flush();
            writer.close();
        }
    }
}
