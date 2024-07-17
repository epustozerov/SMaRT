package com.dm.smart;

import static com.dm.smart.DrawFragment.dampen;
import static com.dm.smart.DrawFragment.defineMinMaxColors;
import static com.dm.smart.ui.elements.CustomAlertDialogs.showColorPickerDialog;
import static com.dm.smart.ui.elements.CustomAlertDialogs.showGeneralView;
import static com.dm.smart.ui.elements.CustomAlertDialogs.showAddSensationDialog;
import static com.dm.smart.ui.elements.CustomAlertDialogs.showLineWidthDialog;
import static com.dm.smart.ui.elements.CustomToasts.showToast;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.dm.smart.items.Brush;
import com.dm.smart.items.SerializablePaint;
import com.dm.smart.items.Step;
import com.dm.smart.ui.elements.CustomThumbDrawer;
import com.rtugeek.android.colorseekbar.ColorSeekBar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class CanvasFragment extends Fragment implements BodyDrawingView.OnDrawingChangeListener {

    // Storage Permissions
    private static final int READ_MEDIA_IMAGES = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_MEDIA_IMAGES
    };
    private final List<String> sortedChoices = new ArrayList<>();
    public ArrayList<String> selectedSensations;
    public int color;
    public LinearLayout tagContainerSensations;
    public int activeBodyViewIndex;
    public BodyDrawingView[] bodyViews;
    public Bitmap[] generalView;
    ImageView buttonCompleteView;
    ImageView buttonBackView;
    TypedArray bodyImages;
    TypedArray bodyImagesMasks;
    Toast showedToast = null;
    private Brush currentBrush;
    private List<Brush> brushes;
    private int currentIntensity;
    private boolean tagsVisible = true;
    private int dampenedColor;
    private View mCanvas = null;
    private int currentBrushId, eraserId, lastBrushId;
    private int mShortAnimationDuration;
    private boolean allowOutsideDrawing = false;
    Button buttonSensationsTool;

    private SharedViewModel sharedViewModel;
    private ColorSeekBar intensityScale;

    public CanvasFragment() {
    }

    public int getTabIndex() {
        DrawFragment parentFragment = (DrawFragment) getParentFragment();
        if (parentFragment != null) {
            for (int i = 0; i < parentFragment.viewPagerAdapter.getItemCount(); i++) {
                Fragment fragment = parentFragment.viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i);
                if (fragment != null) {
                    assert fragment.getTag() != null;
                    if (fragment.getTag().equals(this.getTag())) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void onSensationAdded(String sensation) {
        if (!sharedViewModel.isSensationAddedFromDialog() ||
                !Objects.equals(getTag(), sharedViewModel.getActiveCanvasFragmentTag())) {
            return;
        }

        sharedViewModel.setSensationAddedFromDialog(false);
        if (selectedSensations.isEmpty()) {
            ((LinearLayout) mCanvas.findViewById(R.id.drawn_sensations)).removeAllViews();
        }
        if (selectedSensations.contains(sensation)) {
            selectedSensations.remove(sensation);
        } else {
            selectedSensations.add(sensation);
        }
        int index = sortedChoices.indexOf(sensation);
        if (index < 0) {
            ToggleButton b = new ToggleButton(getContext());
            b.setBackground(requireContext().getDrawable(R.drawable.custom_radio));
            b.setTextColor(Color.BLACK);
            b.setTextOn(sensation);
            b.setTextOff(sensation);
            b.setText(sensation);
            b.setPadding(Math.round(dp2px(8)), Math.round(dp2px(8)),
                    Math.round(dp2px(8)), Math.round(dp2px(8)));
            b.setOnClickListener(v -> {
                String selectedSensation1 = ((ToggleButton) v).getText().toString();
                if (selectedSensations.contains(selectedSensation1)) {
                    selectedSensations.remove(selectedSensation1);
                    b.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                } else {
                    selectedSensations.add(selectedSensation1);
                    b.setBackgroundTintList(ColorStateList.valueOf(dampenedColor));
                }
                updateTagContainerSensations();
            });
            b.setTag(sensation);
            b.setBackgroundTintList(ColorStateList.valueOf(dampenedColor));
            sortedChoices.add(sensation);
            LinearLayout sensationsContainer = mCanvas.findViewById(R.id.sensations_container);
            LinearLayout lastColumn = (LinearLayout) sensationsContainer.getChildAt(sensationsContainer.getChildCount() - 1);

            // If the last column has less than 20 buttons, add the new button to it.
            // Otherwise, create a new column and add the button to it.
            if (lastColumn != null && lastColumn.getChildCount() < 20) {
                lastColumn.addView(b, lastColumn.getChildCount() - 1); // Add the button above the "add sensation" button
            } else {
                LinearLayout newColumn = new LinearLayout(getContext());
                newColumn.setOrientation(LinearLayout.VERTICAL);
                newColumn.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                newColumn.addView(b);
                sensationsContainer.addView(newColumn);
            }
            updateTagContainerSensations();
        }
    }

    private void updateTagContainerSensations() {
        tagContainerSensations.removeAllViews();
        for (String selectedSensation : selectedSensations) {
            TextView txt = new TextView(getContext());
            txt.setText(selectedSensation);
            txt.setPadding(Math.round(dp2px(6)), Math.round(dp2px(6)),
                    Math.round(dp2px(6)), Math.round(dp2px(6)));
            tagContainerSensations.addView(txt);
        }
    }

    public CanvasFragment(Bundle b) {
        color = b.getInt("color");
        dampenedColor = dampen(color);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    READ_MEDIA_IMAGES
            );
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBrushes();
    }

    @SuppressLint({"ClickableViewAccessibility", "UseCompatLoadingForDrawables", "ResourceType", "MissingInflatedId"})
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e("DEBUG", "onCreateView of CanvasFragment " + getTag());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            verifyStoragePermissions(requireActivity());
        }

        // Get the SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Observe the LiveData
        sharedViewModel.getSensation().observe(getViewLifecycleOwner(), this::onSensationAdded);
        sharedViewModel.getLineWidth().observe(getViewLifecycleOwner(), this::onLineWidthChanged);

        String selectedSubjectBodyScheme = MainActivity.currentlySelectedSubject.getBodyScheme();
        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        boolean customConfig = sharedPref.getBoolean(getString(R.string.sp_custom_config), false);
        Configuration configuration;
        if (customConfig) {
            String configPath = sharedPref.getString(getString(R.string.sp_custom_config_path), "");
            String configName = sharedPref.getString(getString(R.string.sp_selected_config), "Built-in");
            configuration = new Configuration(configPath, configName);
            try {
                configuration.formConfig(selectedSubjectBodyScheme);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            configuration = null;
            String currentlySelectedSubjectBodyScheme = MainActivity.currentlySelectedSubject.getBodyScheme();
            switch (currentlySelectedSubjectBodyScheme) {
                case "männlich":
                    bodyImages = getResources().obtainTypedArray(R.array.body_figures_male_main);
                    bodyImagesMasks = getResources().obtainTypedArray(R.array.body_figures_male_mask);
                    break;
                case "weiblich":
                    bodyImages = getResources().obtainTypedArray(R.array.body_figures_female_main);
                    bodyImagesMasks = getResources().obtainTypedArray(R.array.body_figures_female_mask);
                    break;
                default:
                    bodyImages = getResources().obtainTypedArray(R.array.body_figures_neutral_main);
                    bodyImagesMasks = getResources().obtainTypedArray(R.array.body_figures_neutral_mask);
                    break;
            }
        }

        if (mCanvas != null) {
            return mCanvas;
        }
        mCanvas = inflater.inflate(R.layout.fragment_canvas, container, false);


        // Show current fragment tag
        Log.e("RECREATION", "CanvasFragment");
        Log.e("SELECTED SENSATIONS", String.valueOf(selectedSensations));
        // Init container with drawn sensations
        // Don't show the default text if there are more than 0 sensations

        // Determine the number of body views based on the configuration
        int numBodyViews = customConfig ? configuration.selectedBodyViews.length : 2;

        // Init body figures for drawing
        bodyViews = new BodyDrawingView[numBodyViews];

        // Array of all possible view IDs
        int[] viewIds = {
                R.id.drawing_view_front,
                R.id.drawing_view_back1,
                R.id.drawing_view_back2,
                R.id.drawing_view_back3,
                R.id.drawing_view_back4,
                R.id.drawing_view_back5
        };

        for (int i = 0; i < numBodyViews; i++) {
            bodyViews[i] = mCanvas.findViewById(viewIds[i]);
            if (i != 0) {
                bodyViews[i].setVisibility(View.GONE);
            }
        }

        // If there is only one body view, hide the switch button
        if (numBodyViews == 1) {
            mCanvas.findViewById(R.id.textview_switch_bodyview).setVisibility(View.GONE);
        }

        // Set the remaining views to GONE
        for (int i = numBodyViews; i < viewIds.length; i++) {
            mCanvas.findViewById(viewIds[i]).setVisibility(View.GONE);
        }

        if (customConfig) {
            for (int i = 0; i < configuration.selectedBodyViews.length; i++) {
                bodyViews[i].setBGImage(setBodyImage(configuration.selectedBodyViews[i], false));
                bodyViews[i].setMaskImage(setBodyImage(configuration.selectedBodyViews[i] + "_mask", false));
                // set the visibility to GONE for all body views except the first one
                if (i != 0) {
                    bodyViews[i].setVisibility(View.GONE);
                }
            }
        } else {
            bodyViews[0].setBGImage(setBodyImage(bodyImages.getResourceId(0, 0), false));
            bodyViews[0].setMaskImage(setBodyImage(bodyImagesMasks.getResourceId(0, 0), false));
            bodyViews[1].setBGImage(setBodyImage(bodyImages.getResourceId(1, 0), false));
            bodyViews[1].setMaskImage(setBodyImage(bodyImagesMasks.getResourceId(1, 0), false));
        }

        final LinearLayout viewA = mCanvas.findViewById(R.id.viewA);
        final View grayOverlay = mCanvas.findViewById(R.id.gray_overlay_opened_tab);
        grayOverlay.setClickable(true);
        grayOverlay.setAlpha(.618f);

        // Open or close the sensation tab (on the left)
        buttonSensationsTool = mCanvas.findViewById(R.id.button_sensations_tool);
        buttonSensationsTool.setOnClickListener(v -> {
            AnimatorSet animSet = new AnimatorSet();
            if (tagsVisible) {
                if (sortedChoices.isEmpty()) {
                    if (showedToast != null) {
                        showedToast.cancel();
                    }
                    showedToast = showToast(this.getContext(), getResources().getString(R.string.toast_select_tag));
                    showedToast.show();
                    return;
                }
                buttonSensationsTool.setText(getResources().getString(R.string.to_tags));
                ObjectAnimator animX =
                        ObjectAnimator.ofFloat(viewA, "x",
                                buttonSensationsTool.getWidth() - viewA.getWidth());
                ObjectAnimator animAlpha =
                        ObjectAnimator.ofFloat(grayOverlay, "alpha", 0f);
                animSet.playTogether(animX, animAlpha);
                grayOverlay.setClickable(false);
            } else {
                buttonSensationsTool.setText(getResources().getString(R.string.to_drawing));
                ObjectAnimator animX = ObjectAnimator.ofFloat(viewA, "x", 0);
                ObjectAnimator animAlpha = ObjectAnimator.ofFloat(grayOverlay, "alpha", .618f);
                animSet.playTogether(animX, animAlpha);
                grayOverlay.setClickable(true);
            }
            tagsVisible = !tagsVisible;
            animSet.setDuration(240);
            animSet.start();
        });

        // Close the sensation tab if the sensation is selected
        grayOverlay.setOnClickListener(v -> {
            AnimatorSet animSet = new AnimatorSet();
            if (tagsVisible) {
                if (sortedChoices.isEmpty()) {
                    if (showedToast != null) {
                        showedToast.cancel();
                    }
                    showedToast = showToast(this.getContext(), getResources().getString(R.string.toast_select_tag));
                    showedToast.show();
                    return;
                }
                buttonSensationsTool.setText(getResources().getString(R.string.to_tags));
                ObjectAnimator animX = ObjectAnimator.ofFloat(viewA, "x",
                        buttonSensationsTool.getWidth() - viewA.getWidth());
                ObjectAnimator animAlpha = ObjectAnimator.ofFloat(grayOverlay, "alpha", 0f);
                animSet.playTogether(animX, animAlpha);
                grayOverlay.setClickable(false);
            }
            tagsVisible = !tagsVisible;
            animSet.setDuration(240);
            animSet.start();
        });

        // Init the complete image view
        buttonCompleteView = mCanvas.findViewById(R.id.button_general_view);
        if (customConfig) {
            buttonCompleteView.setImageBitmap(setBodyImage(configuration.selectedBodyViews[0], true));
        } else {
            buttonCompleteView.setImageBitmap(setBodyImage(bodyImages.getResourceId(0, 0), true));
        }
        buttonCompleteView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        generalView = new Bitmap[bodyViews.length];
        buttonCompleteView.setOnClickListener(v -> {
            updateGeneralView(activeBodyViewIndex);
            showGeneralView(requireContext(), generalView[activeBodyViewIndex]).show();
        });

        // Init switch of front and back body images
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        buttonBackView = mCanvas.findViewById(R.id.button_switch_bodyview);

        if (customConfig) {
            if (configuration.selectedBodyViews.length < 2) {
                buttonBackView.setVisibility(View.GONE);
            } else {
                buttonBackView.setImageBitmap(setBodyImage(configuration.selectedBodyViews[1], true));
            }
        } else {
            buttonBackView.setImageBitmap(setBodyImage(bodyImages.getResourceId(1, 0), true));
        }

        buttonBackView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        buttonBackView.setOnClickListener(v -> {
            int nextBodyViewIndex = (activeBodyViewIndex + 1) % bodyViews.length;
            int prevBodyViewIndex = (activeBodyViewIndex + bodyViews.length - 2) % bodyViews.length;

            updateBackView(bodyViews[prevBodyViewIndex].snapshot, bodyViews[prevBodyViewIndex].backgroundImage);
            updateGeneralView(nextBodyViewIndex);
            v.setEnabled(false);
            bodyViews[(activeBodyViewIndex + 1) % bodyViews.length].setAlpha(1f); // Set alpha to 1
            bodyViews[(activeBodyViewIndex + 1) % bodyViews.length].setVisibility(View.VISIBLE);

            // Animate the content view to 100% opacity, and clear any animation
            // listener set on the view
            bodyViews[(activeBodyViewIndex + 1) % bodyViews.length].animate()
                    .alpha(1f)
                    .setDuration(mShortAnimationDuration)
                    .setListener(null);

            // Animate the loading view to 0% opacity. After the animation ends,
            // set its visibility to GONE as an optimization step
            // (it won't participate in layout passes, etc.)
            bodyViews[activeBodyViewIndex].animate()
                    .alpha(0f)
                    .setDuration(mShortAnimationDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            bodyViews[activeBodyViewIndex].setVisibility(View.GONE);
                            bodyViews[(activeBodyViewIndex + 1) % bodyViews.length].setBrush(currentBrush);
                            bodyViews[(activeBodyViewIndex + 1) % bodyViews.length].setIntensity(currentIntensity);
                            activeBodyViewIndex = (activeBodyViewIndex + 1) % bodyViews.length;
                            v.setEnabled(true);
                        }
                    });
        });

        assert getParentFragment() != null;
        ArrayList<String> savedSendations = ((DrawFragment) getParentFragment()).sensationsList.get(getTag());
        if (savedSendations != null) {
            selectedSensations = savedSendations;
        } else {
            selectedSensations = new ArrayList<>();
        }
        tagContainerSensations = mCanvas.findViewById(R.id.drawn_sensations);

        Integer savedColor = ((DrawFragment) getParentFragment()).colorsList.get(getTag());
        if (savedColor != null) {
            color = savedColor;
            dampenedColor = dampen(color);
        }

        // Init the list of sensations to select in the top panel
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Math.round(dp2px(4)), Math.round(dp2px(4)),
                Math.round(dp2px(4)), Math.round(dp2px(4)));
        LinearLayout sensationsContainer = mCanvas.findViewById(R.id.sensations_container);

        // Create buttons with sensations in the side panel
        View.OnClickListener choiceClickListener = v -> {
            if (selectedSensations.isEmpty()) {
                ((LinearLayout) mCanvas.findViewById(R.id.drawn_sensations)).removeAllViews();
            }
            String selectedSensation = ((ToggleButton) v).getText().toString();
            if (selectedSensations.contains(selectedSensation)) {
                selectedSensations.remove(selectedSensation);
            } else {
                selectedSensations.add(selectedSensation);
            }
            int index = sortedChoices.indexOf(selectedSensation);
            if (index < 0) {
                v.setBackgroundTintList(ColorStateList.valueOf(dampenedColor));
                sortedChoices.add(selectedSensation);
                TextView txt = new TextView(getContext());
                txt.setText(selectedSensation);
                txt.setPadding(Math.round(dp2px(6)), Math.round(dp2px(6)),
                        Math.round(dp2px(6)), Math.round(dp2px(6)));
                tagContainerSensations.addView(txt);
            } else {
                v.setBackgroundTintList(null);
                sortedChoices.remove(selectedSensation);
                if (tagContainerSensations.getChildCount() > index) {
                    tagContainerSensations.removeViewAt(index);
                }
            }
        };

        String[] sensationTypes;
        if (customConfig) {
            sensationTypes = configuration.sensationTypes;
        } else {
            sensationTypes = getResources().getStringArray(R.array.sensation_types);
        }
        for (String selectedSensation : selectedSensations) {
            if (!Arrays.asList(sensationTypes).contains(selectedSensation)) {
                sensationTypes = Arrays.copyOf(sensationTypes, sensationTypes.length + 1);
                sensationTypes[sensationTypes.length - 1] = selectedSensation;
            }
        }

        int numRows = (int) Math.floor((double) (getResources().getDisplayMetrics().heightPixels - 120) / dp2px(60));
        int numColumns = (int) Math.ceil((double) sensationTypes.length / numRows);
        for (int i = 0; i < numColumns; i++) {
            LinearLayout column = new LinearLayout(getContext());
            column.setOrientation(LinearLayout.VERTICAL);
            column.setLayoutParams(lp);
            sensationsContainer.addView(column);
            for (int j = 0; j < numRows; j++) {
                int index = i * numRows + j;
                if (index >= sensationTypes.length) {
                    break;
                }
                ToggleButton b = new ToggleButton(getContext());
                b.setBackground(requireContext().getDrawable(R.drawable.custom_radio));
                b.setTextColor(Color.BLACK);
                b.setTextOn(sensationTypes[index]);
                b.setTextOff(sensationTypes[index]);
                b.setText(sensationTypes[index]);
                b.setPadding(Math.round(dp2px(8)), Math.round(dp2px(8)),
                        Math.round(dp2px(8)), Math.round(dp2px(8)));
                b.setOnClickListener(choiceClickListener);
                b.setTag(sensationTypes[index]);
                column.addView(b);
            }
            // Add here one more button with the text "anderer Begriff" on it
            if (i == numColumns - 1) {

                // Create new layout parameters for the button
                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, // Width
                        LinearLayout.LayoutParams.WRAP_CONTENT  // Height
                );
                int topMarginInDp = 20;
                int topMarginInPixels = Math.round(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        topMarginInDp,
                        getResources().getDisplayMetrics()
                ));
                buttonParams.setMargins(0, topMarginInPixels, 0, 0);

                Button b_add_sens = new Button(getContext());
                b_add_sens.setBackground(requireContext().getDrawable(R.drawable.custom_radio));
                b_add_sens.setTextColor(Color.BLACK);
                b_add_sens.setText(getResources().getString(R.string.other_sensation));
                b_add_sens.setPadding(Math.round(dp2px(8)), Math.round(dp2px(8)),
                        Math.round(dp2px(8)), Math.round(dp2px(8)));
                b_add_sens.setLayoutParams(buttonParams);
                b_add_sens.setOnClickListener(v -> {
                    // create a new dialog with an edit text field
                    String newSensation = showAddSensationDialog(getContext(), sharedViewModel);
                    if (newSensation != null) {
                        if (selectedSensations.isEmpty()) {
                            ((LinearLayout) mCanvas.findViewById(R.id.drawn_sensations)).removeAllViews();
                        }
                        if (selectedSensations.contains(newSensation)) {
                            selectedSensations.remove(newSensation);
                        } else {
                            selectedSensations.add(newSensation);
                        }
                        int index1 = sortedChoices.indexOf(newSensation);
                        if (index1 < 0) {
                            ToggleButton b = new ToggleButton(getContext());
                            b.setBackground(requireContext().getDrawable(R.drawable.custom_radio));
                            b.setTextColor(Color.BLACK);
                            b.setTextOn(newSensation);
                            b.setTextOff(newSensation);
                            b.setText(newSensation);
                            b.setPadding(Math.round(dp2px(8)), Math.round(dp2px(8)),
                                    Math.round(dp2px(8)), Math.round(dp2px(8)));
                            b.setOnClickListener(choiceClickListener);
                            b.setTag(newSensation);
                            b.setBackgroundTintList(ColorStateList.valueOf(dampenedColor));
                            sortedChoices.add(newSensation);
                            TextView txt = new TextView(getContext());
                            txt.setText(newSensation);
                            txt.setPadding(Math.round(dp2px(6)), Math.round(dp2px(6)),
                                    Math.round(dp2px(6)), Math.round(dp2px(6)));
                            tagContainerSensations.addView(txt);
                        } else {
                            sortedChoices.remove(newSensation);
                            tagContainerSensations.removeViewAt(index1);
                        }
                    }

                });
                column.addView(b_add_sens);
            }
        }

        for (String selectedSensation : selectedSensations) {
            int index = Arrays.asList(sensationTypes).indexOf(selectedSensation);
            ToggleButton b = sensationsContainer.findViewWithTag(sensationTypes[index]);
            b.setChecked(true);
            b.setBackgroundTintList(ColorStateList.valueOf(dampenedColor));
            sortedChoices.add(selectedSensation);
            TextView txt = new TextView(getContext());
            txt.setText(selectedSensation);
            txt.setPadding(Math.round(dp2px(6)), Math.round(dp2px(6)),
                    Math.round(dp2px(6)), Math.round(dp2px(6)));
            tagContainerSensations.addView(txt);
        }

        // Init the bottom tool bar
        LinearLayout toolContainer = mCanvas.findViewById(R.id.tools_container);
        LinearLayout.LayoutParams lp1 =
                new LinearLayout.LayoutParams(Math.round(dp2px(80)), Math.round(dp2px(102)));
        lp1.setMargins(Math.round(dp2px(4)), 0, Math.round(dp2px(4)), 0);
        LinearLayout.LayoutParams lp2 =
                new LinearLayout.LayoutParams(Math.round(dp2px(80)), Math.round(dp2px(102)));
        lp2.setMargins(Math.round(dp2px(24)), 0, Math.round(dp2px(4)), 0);

        // Tool buttons
        final List<ImageButton> toolsBtns = new ArrayList<>();
        @SuppressLint("ClickableViewAccessibility") View.OnTouchListener keepSelectedListener = (v, event) -> {
            int brushId = ((ViewGroup) v.getParent()).indexOfChild(v);
            for (ImageButton imageButton : toolsBtns) {
                imageButton.setPressed(brushId ==
                        ((ViewGroup) imageButton.getParent()).indexOfChild(imageButton));
                if (brushId != eraserId) {
                    lastBrushId = brushId;
                }
                currentBrushId = brushId;
                currentBrush = brushes.get(brushId);
                bodyViews[activeBodyViewIndex].setBrush(currentBrush);
            }
            return true;
        };

        for (Brush brush : brushes) {
            ImageButton btn = new ImageButton(getContext());
            btn.setBackground(requireContext().getDrawable(R.drawable.listitem_selector));
            btn.setImageDrawable(brush.icon);
            btn.setCropToPadding(false);
            btn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
            btn.setOnTouchListener(keepSelectedListener);
            toolContainer.addView(btn, lp1);
            toolsBtns.add(btn);
            if (brush.title.equals(getString(R.string.brush_erase))) {
                eraserId = brushes.indexOf(brush);
            }
        }

        // Select the initial body view
        activeBodyViewIndex = 0;

        // Select the initially selected tool
        currentBrushId = 0;
        lastBrushId = 0;
        currentBrush = brushes.get(0);
        currentIntensity = -1;
        toolsBtns.get(0).setPressed(true);
        bodyViews[activeBodyViewIndex].setBrush(currentBrush);

        // Undo button
        ImageButton btnUndo = new ImageButton(getContext());
        btnUndo.setBackground(requireContext().getDrawable(R.drawable.listitem_selector));
        btnUndo.setImageDrawable(requireContext().getDrawable(R.drawable.icon_undo));
        btnUndo.setCropToPadding(false);
        btnUndo.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        btnUndo.setOnClickListener(view -> bodyViews[activeBodyViewIndex].undoLastStep());
        toolContainer.addView(btnUndo, lp2);

        // Out of body button
        ImageButton btnOutOfBody = new ImageButton(getContext());
        btnOutOfBody.setBackground(requireContext().getDrawable(R.drawable.listitem_selector));
        btnOutOfBody.setImageDrawable(requireContext().getDrawable(R.drawable.icon_out));
        btnOutOfBody.setCropToPadding(false);
        btnOutOfBody.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        btnOutOfBody.setOnClickListener(view -> {
            allowOutsideDrawing = !allowOutsideDrawing;
            bodyViews[activeBodyViewIndex].setAllowOutsideDrawing(allowOutsideDrawing);
            bodyViews[(activeBodyViewIndex + 1) % bodyViews.length].setAllowOutsideDrawing(allowOutsideDrawing);
            if (allowOutsideDrawing) {
                btnOutOfBody.setImageDrawable(requireContext().getDrawable(R.drawable.icon_no_out));
            } else {
                btnOutOfBody.setImageDrawable(requireContext().getDrawable(R.drawable.icon_out));
            }
        });
        toolContainer.addView(btnOutOfBody, lp2);

        // Color picker button
        ImageButton btnColorPicker = new ImageButton(getContext());
        btnColorPicker.setBackground(requireContext().getDrawable(R.drawable.listitem_selector));
        btnColorPicker.setImageDrawable(requireContext().getDrawable(R.drawable.icon_color));
        btnColorPicker.setCropToPadding(false);
        btnColorPicker.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        btnColorPicker.setBackgroundColor(dampenedColor);
        CustomThumbDrawer thumbDrawer = new CustomThumbDrawer(65, Color.WHITE, Color.BLACK);
        btnColorPicker.setOnClickListener(v -> {
                    showColorPickerDialog(
                            getContext(),
                            currentBrush.paint.getColor(),
                            selectedColor -> {
                                color = selectedColor;
                                dampenedColor = dampen(color);
                                bodyViews[activeBodyViewIndex].setIntensity(currentIntensity);
                                intensityScale.setColorSeeds(defineMinMaxColors(selectedColor));
                                intensityScale.setMaxProgress(100);
                                btnColorPicker.setBackgroundColor(dampenedColor);
                                currentIntensity = color;
                                currentBrush.paint.setColor(selectedColor);
                                bodyViews[activeBodyViewIndex].setBrush(currentBrush);
                                // overwrite the appropriate colors in colors array of the parent DrawFragment
                                sharedViewModel.setColorAndIndex(selectedColor, getTabIndex());
                                ((DrawFragment) getParentFragment()).colors.set(getTabIndex(), selectedColor);
                                ((DrawFragment) getParentFragment()).colorsList.put(getTag(), selectedColor);
                                toolsBtns.get(currentBrushId).setPressed(true);
                            },
                            (dialog, selectedColor, allColors) -> {
                                intensityScale.setColorSeeds(defineMinMaxColors(selectedColor));
                                // update the color of the buttons on the left panel with the new color
                                for (String sensation : selectedSensations) {
                                    ToggleButton b = sensationsContainer.findViewWithTag(sensation);
                                    if (b != null) {
                                        b.setBackgroundTintList(ColorStateList.valueOf(dampenedColor));
                                    }
                                }
                                toolsBtns.get(currentBrushId).setPressed(true);
                                dialog.dismiss();
                            }
                    );
                    toolsBtns.get(currentBrushId).setPressed(true);
                }
        );
        toolContainer.addView(btnColorPicker, lp1);

        // Line width button
        ImageButton btnLineWidth = new ImageButton(getContext());
        btnLineWidth.setBackground(requireContext().getDrawable(R.drawable.listitem_selector));
        btnLineWidth.setImageDrawable(requireContext().getDrawable(R.drawable.icon_line_width));
        btnLineWidth.setCropToPadding(false);
        btnLineWidth.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        btnLineWidth.setOnClickListener(v -> {
            toolsBtns.get(currentBrushId).setPressed(true);
            // create a new dialog with a seekbar to select the line width
            showLineWidthDialog(getContext(), sharedViewModel, currentBrush.thickness, lineWidth -> {
                // Update the brush thickness
                currentBrush.thickness = lineWidth;
                toolsBtns.get(currentBrushId).setPressed(true);
            });
            // update the thinkness of the brush
            //noinspection DataFlowIssue
            currentBrush.thickness = sharedViewModel.getLineWidth().getValue();
            bodyViews[activeBodyViewIndex].setBrush(currentBrush);
            toolsBtns.get(currentBrushId).setPressed(true);
        });
        toolContainer.addView(btnLineWidth, lp1);

        // Init intensity scale
        intensityScale = mCanvas.findViewById(R.id.color_seek_bar);
        intensityScale.setColorSeeds(defineMinMaxColors(color));
        intensityScale.setThumbDrawer(thumbDrawer);

        // if the custom config is in use, set the text for textview_scale_max from the config
        if (customConfig) {
            TextView textViewScaleMax = mCanvas.findViewById(R.id.textview_scale_max);
            textViewScaleMax.setText(configuration.getTextMax());
            TextView textViewScaleMin = mCanvas.findViewById(R.id.textview_scale_min);
            textViewScaleMin.setText(configuration.getTextMin());
        }

        // Set intenstityScale on touch listener to process only stylised touch events
        intensityScale.setOnTouchListener((v, event) -> {
            boolean isPen = event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS;
            if (isPen) {
                bodyViews[activeBodyViewIndex].setIntensity(currentIntensity);
                return false;
            }
            return true;
        });

        intensityScale.setOnColorChangeListener((progress, color) -> {
            // recalculate the necessary color using this.color and progress

            bodyViews[activeBodyViewIndex].setIntensity(progress, color);
            currentIntensity = color;
            if (currentBrushId != lastBrushId) {
                toolsBtns.get(eraserId).setPressed(false);
                toolsBtns.get(lastBrushId).setPressed(true);
                currentBrushId = lastBrushId;
                currentBrush = brushes.get(currentBrushId);
                bodyViews[activeBodyViewIndex].setBrush(currentBrush);
            }
        });

        updateGeneralView(activeBodyViewIndex);
        for (BodyDrawingView bodyView : bodyViews) {
            bodyView.setOnDrawingChangeListener(this);
        }
        return mCanvas;
    }

    private void onLineWidthChanged(Integer lineWidth) {
        if (lineWidth != null) {
            currentBrush.paint.setStrokeWidth(lineWidth);
            bodyViews[activeBodyViewIndex].setBrush(currentBrush);
        }
    }

    @Override
    public void onDrawingChange() {
        updateGeneralView(activeBodyViewIndex);
        persistStepsDF();
        assert getParentFragment() != null;
        ((DrawFragment) getParentFragment()).saveTempData();

    }

    private float dp2px(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    public Bitmap setBodyImage(int bodyTypeId, boolean thumbed) {
        if (thumbed) {
            return Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(getResources(), bodyTypeId),
                    149, 220, true);
        } else {
            return BitmapFactory.decodeResource(getResources(), bodyTypeId);
        }
    }

    public Bitmap setBodyImage(String bodyTypeId, boolean thumbed) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            verifyStoragePermissions(requireActivity());
        }
        // take the body scheme file from inner app store, filder body_figures
        File bitmapFile = new File(requireActivity().getFilesDir(), "body_figures/" + bodyTypeId + ".png");
        Bitmap sensationsFront = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath());
        if (thumbed) {
            return Bitmap.createScaledBitmap(sensationsFront, 149, 220, true);
        } else {
            return sensationsFront;
        }
    }


    @SuppressLint("ResourceType")
    private void initBrushes() {
        brushes = new ArrayList<>();
        @SuppressLint("Recycle") TypedArray brushes_list =
                getResources().obtainTypedArray(R.array.brushes_list);

        for (int i = 0; i < brushes_list.length(); i++) {
            int id = brushes_list.getResourceId(i, 0);
            @SuppressLint("Recycle") TypedArray brush_array = getResources().obtainTypedArray(id);

            Brush tempBrush = new Brush();
            tempBrush.title = brush_array.getString(0);
            tempBrush.icon = brush_array.getDrawable(1);
            tempBrush.drawByMove = (brush_array.getBoolean(2, true));
            tempBrush.thickness = (brush_array.getInteger(3, 1));
            tempBrush.type = brush_array.getString(4);

            SerializablePaint tempPaint = new SerializablePaint();
            tempPaint.setDither(false);
            tempPaint.setColor(0xFF33B5E5);
            tempPaint.setStrokeJoin(Paint.Join.ROUND);
            tempPaint.setStrokeCap(Paint.Cap.ROUND);
            tempPaint.setPathEffect(new CornerPathEffect(10));
            tempPaint.setStyle(Paint.Style.valueOf(brush_array.getString(5)));
            tempPaint.setStrokeWidth(tempBrush.thickness);
            tempBrush.paint = tempPaint;
            brushes.add(tempBrush);
        }
    }

    void updateGeneralView(int index) {
        DrawFragment drawFragment = (DrawFragment) getParentFragment();
        assert drawFragment != null;

        int createdWindows = drawFragment.viewPagerAdapter.getItemCount();
        Bitmap fullPicture = null;
        Canvas canvas = null;

        for (int i = 0; i < createdWindows; i++) {
            CanvasFragment cf = (CanvasFragment) drawFragment.viewPagerAdapter.fragmentManager.findFragmentByTag("f" + i);
            if (cf != null) {
                if (fullPicture == null) {
                    fullPicture = Bitmap.createBitmap(cf.bodyViews[index].backgroundImage.getWidth(),
                            cf.bodyViews[index].backgroundImage.getHeight(), cf.bodyViews[index].backgroundImage.getConfig());
                    canvas = new Canvas(fullPicture);
                    canvas.drawBitmap(cf.bodyViews[index].backgroundImage, 0f, 0f, null);
                }
                if (cf.bodyViews[index].snapshot != null) {
                    Paint paint = new Paint();
                    paint.setColor(cf.color);
                    canvas.drawBitmap(cf.bodyViews[index].snapshot, 0f, 0f, paint);
                }
            }
        }

        if (fullPicture != null) {
            generalView[index] = fullPicture;
            buttonCompleteView.setImageBitmap(Bitmap.createScaledBitmap(fullPicture, 149, 220, true));
        }
    }

    private void updateBackView(Bitmap sensations, Bitmap background) {
        Bitmap fullPicture = Bitmap.createBitmap(background.getWidth(),
                background.getHeight(), background.getConfig());
        Canvas canvas = new Canvas(fullPicture);
        canvas.drawBitmap(background, 0f, 0f, null);
        if (sensations != null)
            canvas.drawBitmap(sensations, 0f, 0f, null);
        buttonBackView.setImageBitmap(Bitmap.createScaledBitmap(fullPicture, 149, 220, true));
    }


    @Override
    public void onPause() {
        Log.e("DEBUG", "OnPause of CanvasFragment " + this.getTag());
        super.onPause();
    }


    @Override
    public void onDestroy() {
        Log.e("DEBUG", "OnDestroy of CanvasFragment " + this.getTag());
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        Log.e("DEBUG", "OnDestroyView of CanvasFragment " + this.getTag());
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        Log.e("DEBUG", "OnDetach of CanvasFragment " + this.getTag());
        super.onDetach();
    }

    @Override
    public void onStop() {
        Log.e("DEBUG", "OnStop of CanvasFragment " + this.getTag());
        super.onStop();
    }

    @Override
    public void onResume() {
        Log.e("DEBUG", "OnResume of CanvasFragment " + this.getTag());
        super.onResume();
        sharedViewModel.setActiveCanvasFragmentTag(getTag());
        restoreStepsDF();
    }

    @Override
    public void onStart() {
        Log.e("DEBUG", "OnStart of CanvasFragment " + this.getTag());
        super.onStart();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        Log.e("DEBUG", "OnAttach of CanvasFragment " + this.getTag());
        super.onAttach(context);
    }

    public void restoreStepsDF() {
        DrawFragment drawFragment = (DrawFragment) getParentFragment();
        assert getParentFragment() != null;
        Map<String, List<List<Step>>> stepsList = drawFragment.stepsList;
        List<List<Step>> bodyViewStepsList = stepsList.get(getTag());
        if (bodyViewStepsList != null) {
            for (int i = 0; i < bodyViewStepsList.size(); i++) {
                List<Step> savedSteps = bodyViewStepsList.get(i);
                if (savedSteps != null) {
                    bodyViews[i].steps = savedSteps;
                    bodyViews[i].redrawAllSavedSteps();
                    bodyViews[i].invalidate();
                }
            }
        }
        Log.e("RESTORE STEPS", "Restoring steps for " + getTag() + ": " + bodyViews[activeBodyViewIndex].steps);
        int backViewIndex = (activeBodyViewIndex + 1) % bodyViews.length;
        updateBackView(bodyViews[backViewIndex].snapshot, bodyViews[backViewIndex].backgroundImage);
        updateGeneralView(activeBodyViewIndex);
    }


    private void persistStepsDF() {
        DrawFragment drawFragment = (DrawFragment) getParentFragment();
        assert drawFragment != null;
        drawFragment.sensationsList.put(this.getTag(), selectedSensations);
        drawFragment.colorsList.put(this.getTag(), color);
        List<List<Step>> bodyViewStepsList = new ArrayList<>();
        for (BodyDrawingView bodyView : bodyViews) {
            bodyViewStepsList.add(new ArrayList<>(bodyView.steps));
        }
        drawFragment.stepsList.put(this.getTag(), bodyViewStepsList);
    }

}
