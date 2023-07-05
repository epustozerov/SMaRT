package com.dm.smart;

import static com.dm.smart.DrawFragment.dampen;
import static com.dm.smart.DrawFragment.define_min_max_colors;
import static com.dm.smart.ui.elements.CustomAlertDialogs.showGeneralView;
import static com.dm.smart.ui.elements.CustomToasts.showToast;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
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
import androidx.fragment.app.Fragment;

import com.dm.smart.ui.elements.CustomThumbDrawer;
import com.rtugeek.android.colorseekbar.ColorSeekBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class CanvasFragment extends Fragment {

    public ArrayList<String> selectedSensations;
    public int color;
    public LinearLayout tagContainerSensations;
    public BodyDrawingView bodyViewFront;
    public BodyDrawingView bodyViewBack;
    public Bitmap generalViewFront;
    public Bitmap generalViewBack;
    ImageView buttonCompleteView;
    ImageView buttonBackView;
    TypedArray bodyFigures;
    boolean currentStateIsFront;
    Toast showedToast = null;
    private List<String> sortedChoices = new ArrayList<>();
    private BodyDrawingView currentBodyView;
    private BodyDrawingView hiddenBodyView;
    private Brush currentBrush;
    private List<Brush> brushes;
    private int currentIntensity;
    private boolean tagsVisible = true;
    private int dampenedColor;
    private View mCanvas = null;
    private int currentBrushId, eraserId, lastBrushId;
    private int mShortAnimationDuration;
    private boolean allowOutsideDrawing = false;

    public CanvasFragment(Bundle b) {
        color = b.getInt("color");
        dampenedColor = dampen(color);
    }

    public CanvasFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBrushes();
    }

    @SuppressLint({"ClickableViewAccessibility", "UseCompatLoadingForDrawables", "ResourceType"})
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        currentStateIsFront = true;
        bodyFigures = getResources().obtainTypedArray(R.array.body_figures_neutral);
        switch (MainActivity.currentlySelectedSubject.getGender()) {
            case 0:
                bodyFigures = getResources().obtainTypedArray(R.array.body_figures_neutral);
                break;
            case 1:
                bodyFigures = getResources().obtainTypedArray(R.array.body_figures_female);
                break;
            case 2:
                bodyFigures = getResources().obtainTypedArray(R.array.body_figures_male);
        }

        if (mCanvas != null) {
            return mCanvas;
        }
        mCanvas = inflater.inflate(R.layout.fragment_canvas, container, false);

        // Show current fragment tag
        Log.e("RECREATION", "CanvasFragment");
        Log.e("SELECTED SENSATIONS", selectedSensations + "");
        if (selectedSensations == null) {
            selectedSensations = new ArrayList<>();
        }

        // Init container with drawn sensations
        tagContainerSensations = mCanvas.findViewById(R.id.drawn_sensations);

        // Init body figures for drawing
        bodyViewFront = mCanvas.findViewById(R.id.drawing_view_front);
        bodyViewFront.setBGImage(setBodyImage(bodyFigures.getResourceId(0, 0), false));
        bodyViewFront.setMaskImage(setBodyImage(bodyFigures.getResourceId(1, 0), false));

        bodyViewBack = mCanvas.findViewById(R.id.drawing_view_back);
        bodyViewBack.setBGImage(setBodyImage(bodyFigures.getResourceId(2, 0), false));
        bodyViewBack.setMaskImage(setBodyImage(bodyFigures.getResourceId(3, 0), false));

        // Init gray overlay
        final LinearLayout viewA = mCanvas.findViewById(R.id.viewA);
        final View grayOverlay = mCanvas.findViewById(R.id.gray_overlay_opened_tab);
        grayOverlay.setClickable(true);
        grayOverlay.setAlpha(.618f);

        // Open or close the sensation tab (on the left)
        final Button buttonSensationsTool = mCanvas.findViewById(R.id.button_sensations_tool);
        buttonSensationsTool.setOnClickListener(v -> {
            AnimatorSet animSet = new AnimatorSet();
            if (tagsVisible) {
                if (sortedChoices.size() == 0) {
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
                if (sortedChoices.size() == 0) {
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
        if (currentStateIsFront) {
            buttonCompleteView.setImageBitmap(setBodyImage(bodyFigures.getResourceId(0, 0), true));
        } else {
            buttonCompleteView.setImageBitmap(setBodyImage(bodyFigures.getResourceId(2, 0), true));
        }
        buttonCompleteView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        generalViewFront = Bitmap.createBitmap(bodyViewFront.backgroundImage.getWidth(),
                bodyViewFront.backgroundImage.getHeight(), Bitmap.Config.ARGB_8888);
        generalViewBack = Bitmap.createBitmap(bodyViewBack.backgroundImage.getWidth(),
                bodyViewBack.backgroundImage.getHeight(), Bitmap.Config.ARGB_8888);
        buttonCompleteView.setOnClickListener(v -> {
            if (currentStateIsFront) {
                Bitmap generalViewFrontBitmap =
                        Bitmap.createBitmap(bodyViewFront.backgroundImage.getWidth(), bodyViewFront.backgroundImage.getHeight(),
                                Bitmap.Config.ARGB_8888);
                Canvas generalViewFrontCanvas = new Canvas(generalViewFrontBitmap);
                generalViewFrontCanvas.drawBitmap(setBodyImage(
                        bodyFigures.getResourceId(0, 0), false), 0, 0, null);
                generalViewFrontCanvas.drawBitmap(generalViewFront, 0, 0, null);
                if (bodyViewFront.snapshot != null) {
                    generalViewFrontCanvas.drawBitmap(bodyViewFront.snapshot, 0, 0, null);
                }
                AlertDialog alertDialog = showGeneralView(getContext(), generalViewFrontBitmap);
                alertDialog.show();
            } else {
                Bitmap generalViewBackBitmap =
                        Bitmap.createBitmap(bodyViewBack.backgroundImage.getWidth(), bodyViewBack.backgroundImage.getHeight(),
                                Bitmap.Config.ARGB_8888);
                Canvas generalViewBackCanvas = new Canvas(generalViewBackBitmap);
                generalViewBackCanvas.drawBitmap(setBodyImage(
                        bodyFigures.getResourceId(2, 0), false), 0, 0, null);
                generalViewBackCanvas.drawBitmap(generalViewBack, 0, 0, null);
                if (bodyViewBack.snapshot != null) {
                    generalViewBackCanvas.drawBitmap(bodyViewBack.snapshot, 0, 0, null);
                }
                AlertDialog alertDialog = showGeneralView(getContext(), generalViewBackBitmap);
                alertDialog.show();
            }
        });

        // Init switch of front and back body images
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        buttonBackView = mCanvas.findViewById(R.id.button_switch_bodyview);
        final TextView textViewSwitchBody = mCanvas.findViewById(R.id.textview_switch_bodyview);
        buttonBackView.setImageBitmap(setBodyImage(bodyFigures.getResourceId(3, 0), true));
        buttonBackView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        textViewSwitchBody.setText(getResources().getString(R.string.back_view));
        TypedArray finalBody_figures = bodyFigures;
        buttonBackView.setOnClickListener(v -> {
            if (!currentStateIsFront) {
                buttonBackView.setImageBitmap(setBodyImage(finalBody_figures.getResourceId(3, 0), true));
                textViewSwitchBody.setText(getResources().getString(R.string.back_view));
                currentStateIsFront = true;
                currentBodyView = bodyViewBack;
                hiddenBodyView = bodyViewFront;
                updateGeneralView(generalViewFront, bodyViewFront.backgroundImage);
                updateBackView(bodyViewBack.snapshot, bodyViewBack.backgroundImage);
            } else {
                buttonBackView.setImageBitmap(setBodyImage(finalBody_figures.getResourceId(1, 0), true));
                textViewSwitchBody.setText(getResources().getString(R.string.front_view));
                currentStateIsFront = false;
                currentBodyView = bodyViewFront;
                hiddenBodyView = bodyViewBack;
                updateGeneralView(generalViewBack, bodyViewBack.backgroundImage);
                updateBackView(bodyViewFront.snapshot, bodyViewFront.backgroundImage);
            }
            v.setEnabled(false);
            hiddenBodyView.setAlpha(0f);
            hiddenBodyView.setVisibility(View.VISIBLE);

            // Animate the content view to 100% opacity, and clear any animation
            // listener set on the view
            hiddenBodyView.animate()
                    .alpha(1f)
                    .setDuration(mShortAnimationDuration)
                    .setListener(null);

            // Animate the loading view to 0% opacity. After the animation ends,
            // set its visibility to GONE as an optimization step
            // (it won't participate in layout passes, etc.)
            currentBodyView.animate()
                    .alpha(0f)
                    .setDuration(mShortAnimationDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            currentBodyView.setVisibility(View.GONE);
                            hiddenBodyView.setBrush(currentBrush);
                            hiddenBodyView.setIntensity(currentIntensity);
                            BodyDrawingView tmp = hiddenBodyView;
                            hiddenBodyView = currentBodyView;
                            currentBodyView = tmp;
                            v.setEnabled(true);
                        }
                    });
        });

        // Init the list of sensations to select in the top panel
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Math.round(dp2px(4)), Math.round(dp2px(4)),
                Math.round(dp2px(4)), Math.round(dp2px(4)));
        LinearLayout sensationsContainer = mCanvas.findViewById(R.id.sensations_container);

        // Create buttons with sensations in the side panel
        View.OnClickListener choiceClickListener = v -> {
            if (selectedSensations.size() == 0) {
                ((LinearLayout) mCanvas.findViewById(R.id.drawn_sensations)).removeAllViews();
            }
            int bid = ((ViewGroup) v.getParent()).indexOfChild(v);
            String selectedSensation =
                    Arrays.asList(getResources().getStringArray(R.array.sensation_types)).get(bid);
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
                tagContainerSensations.removeViewAt(index);
            }
        };

        String[] sensationTypes = getResources().getStringArray(R.array.sensation_types);
        for (String choice : sensationTypes) {
            ToggleButton b = new ToggleButton(getContext());
            b.setBackground(requireContext().getDrawable(R.drawable.custom_radio));
            b.setTextColor(Color.BLACK);
            b.setTextOn(choice);
            b.setTextOff(choice);
            b.setText(choice);
            b.setPadding(Math.round(dp2px(8)), Math.round(dp2px(8)),
                    Math.round(dp2px(8)), Math.round(dp2px(8)));
            b.setOnClickListener(choiceClickListener);
            sensationsContainer.addView(b, lp);
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
                currentBodyView.setBrush(currentBrush);
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
        currentBodyView = bodyViewFront;
        hiddenBodyView = bodyViewBack;
        hiddenBodyView.setVisibility(View.GONE);

        // Select the initially selected tool
        currentBrushId = 0;
        lastBrushId = 0;
        currentBrush = brushes.get(0);
        currentIntensity = -1;
        toolsBtns.get(0).setPressed(true);
        currentBodyView.setBrush(currentBrush);

        // Undo button
        ImageButton btnUndo = new ImageButton(getContext());
        btnUndo.setBackground(requireContext().getDrawable(R.drawable.listitem_selector));
        btnUndo.setImageDrawable(requireContext().getDrawable(R.drawable.icon_undo));
        btnUndo.setCropToPadding(false);
        btnUndo.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        btnUndo.setOnClickListener(view -> currentBodyView.undoLastStep());
        toolContainer.addView(btnUndo, lp2);

        // Out of body button
        ImageButton btnOutOfBody = new ImageButton(getContext());
        btnOutOfBody.setBackground(requireContext().getDrawable(R.drawable.listitem_selector));
        btnOutOfBody.setImageDrawable(requireContext().getDrawable(R.drawable.icon_out));
        btnOutOfBody.setCropToPadding(false);
        btnOutOfBody.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        btnOutOfBody.setOnClickListener(view -> {
            allowOutsideDrawing = !allowOutsideDrawing;
            currentBodyView.setAllowOutsideDrawing(allowOutsideDrawing);
            hiddenBodyView.setAllowOutsideDrawing(allowOutsideDrawing);
            if (allowOutsideDrawing) {
                btnOutOfBody.setImageDrawable(requireContext().getDrawable(R.drawable.icon_no_out));
            } else {
                btnOutOfBody.setImageDrawable(requireContext().getDrawable(R.drawable.icon_out));
            }

        });
        toolContainer.addView(btnOutOfBody, lp2);

        // Init intensity scale
        ColorSeekBar intensityScale = mCanvas.findViewById(R.id.color_seek_bar);
        intensityScale.setColorSeeds(define_min_max_colors(color));
        intensityScale.setThumbDrawer(new CustomThumbDrawer(65, Color.WHITE, Color.BLACK));

        // set intenstityScale on touch listener to process only stylised touch events
        intensityScale.setOnTouchListener((v, event) -> {
            boolean isPen = event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS;
            if (isPen) {
                currentBodyView.setIntensity(currentIntensity);
                return false;
            }
            return true;
        });

        intensityScale.setOnColorChangeListener((progress, color) -> {
            currentBodyView.setIntensity(color);
            currentIntensity = color;
            if (currentBrushId != lastBrushId) {
                toolsBtns.get(eraserId).setPressed(false);
                toolsBtns.get(lastBrushId).setPressed(true);
                currentBrushId = lastBrushId;
                currentBrush = brushes.get(currentBrushId);
                currentBodyView.setBrush(currentBrush);
            }
        });
        return mCanvas;
    }

    private float dp2px(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    public Bitmap setBodyImage(int body_type_id, boolean thumbed) {
        if (thumbed) {
            return Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(getResources(), body_type_id),
                    149, 220, true);
        } else {
            return BitmapFactory.decodeResource(getResources(), body_type_id);
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

            Paint tempPaint = new Paint();
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


    private void updateGeneralView(Bitmap merged, Bitmap background) {
        Bitmap full_picture = Bitmap.createBitmap(background.getWidth(),
                background.getHeight() + 100, background.getConfig());
        Canvas canvas = new Canvas(full_picture);
        canvas.drawBitmap(background, 0f, 0f, null);
        canvas.drawBitmap(merged, 0f, 0f, null);
        buttonCompleteView.setImageBitmap(Bitmap.createScaledBitmap(full_picture, 149, 220, true));
    }

    private void updateBackView(Bitmap sensations, Bitmap background) {
        Bitmap full_picture = Bitmap.createBitmap(background.getWidth(),
                background.getHeight() + 100, background.getConfig());
        Canvas canvas = new Canvas(full_picture);
        canvas.drawBitmap(background, 0f, 0f, null);
        if (sensations != null)
            canvas.drawBitmap(sensations, 0f, 0f, null);
        buttonBackView.setImageBitmap(Bitmap.createScaledBitmap(full_picture, 149, 220, true));
    }

    public void restoreSensations() {
        // update the select sensation tab with the list of cf.selectedSensations
        sortedChoices = new ArrayList<>(selectedSensations);
        tagContainerSensations = mCanvas.findViewById(R.id.drawn_sensations);
        for (String sensation : sortedChoices) {
            TextView txt = new TextView(getContext());
            txt.setText(sensation);
            txt.setPadding(Math.round(dp2px(6)), Math.round(dp2px(6)),
                    Math.round(dp2px(6)), Math.round(dp2px(6)));
            tagContainerSensations.addView(txt);
        }
        tagContainerSensations.invalidate();
        Log.e("TAG CONTAINER RESTORED", sortedChoices.toString());
    }

    @Override
    public void onPause() {
        Log.e("DEBUG", "OnPause of CanvasFragment " + this.getTag());
        DrawFragment drawFragment = (DrawFragment) getParentFragment();
        assert drawFragment != null;
        drawFragment.allSelectedSensations.put(this.getTag(), selectedSensations);
        Log.e("SAVING", "Saving sensations for " + this.getTag() + " " + selectedSensations.size());
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

    public static class Brush {

        public String title;
        public Drawable icon;
        public String type;
        public boolean drawByMove;
        public boolean drawOutside;
        public int thickness;
        public Paint paint;

        public Brush() {
        }

        public Brush(Brush brush) {
            this.title = brush.title;
            this.icon = brush.icon;
            this.type = brush.type;
            this.drawByMove = brush.drawByMove;
            this.drawOutside = brush.drawOutside;
            this.thickness = brush.thickness;
            this.paint = new Paint(brush.paint);
        }
    }
}
