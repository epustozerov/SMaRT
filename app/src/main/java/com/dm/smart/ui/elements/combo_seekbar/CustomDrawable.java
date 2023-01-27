package com.dm.smart.ui.elements.combo_seekbar;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import java.util.List;


public class CustomDrawable extends Drawable {
    public final float mDotRadius;
    protected final ComboSeekBar mySlider;
    protected final Paint selectLinePaint;
    private final Drawable myBase;
    private final List<ComboSeekBar.Dot> mDots;
    private final float mTextMargin;
    private final int mTextHeight;
    private final boolean mIsMultiline;

    public CustomDrawable(Drawable base, ComboSeekBar slider, List<ComboSeekBar.Dot> dots, int color, int textSize, boolean isMultiline) {
        mIsMultiline = isMultiline;
        mySlider = slider;
        myBase = base;
        mDots = dots;
        Paint textUnselected = new Paint(Paint.ANTI_ALIAS_FLAG);
        textUnselected.setColor(color);
        textUnselected.setAlpha(255);

        Paint textSelected = new Paint(Paint.ANTI_ALIAS_FLAG);
        textSelected.setTypeface(Typeface.DEFAULT_BOLD);
        textSelected.setColor(color);
        textSelected.setAlpha(255);

        Paint unselectLinePaint = new Paint();
        unselectLinePaint.setColor(color);

        unselectLinePaint.setStrokeWidth(toPix(1));

        selectLinePaint = new Paint();
        selectLinePaint.setColor(color);
        selectLinePaint.setStrokeWidth(toPix(3));

        Paint circleLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circleLinePaint.setColor(color);

        Rect textBounds = new Rect();
        textSelected.setTextSize(textSize * 2);
        textSelected.getTextBounds("M", 0, 1, textBounds);

        textUnselected.setTextSize(textSize);
        textSelected.setTextSize(textSize);

        mTextHeight = textBounds.height();
        mDotRadius = toPix(5);
        mTextMargin = toPix(3);
    }

    protected float toPix(int size) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, mySlider.getContext().getResources().getDisplayMetrics());
    }

    @Override
    protected final void onBoundsChange(Rect bounds) {
        myBase.setBounds(bounds);
    }

    @Override
    protected final boolean onStateChange(int[] state) {
        invalidateSelf();
        return false;
    }

    @Override
    public final boolean isStateful() {
        return true;
    }

    @Override
    public void draw(Canvas canvas) {
        int height = this.getIntrinsicHeight() / 2;
        canvas.drawLine(0, height, mySlider.getWidth() - 2 * mDotRadius, height, selectLinePaint);
    }

    @Override
    public final int getIntrinsicHeight() {


        if (mIsMultiline) {
            return (int) (selectLinePaint.getStrokeWidth() + mDotRadius + (mTextHeight) * 2 + mTextMargin);
        } else {

            int numberOfLines = 1;
            for (ComboSeekBar.Dot dot : mDots) {
                int numberOfLinesInDot = dot.text.split("\n").length;
                if (numberOfLinesInDot > numberOfLines) {
                    numberOfLines = numberOfLinesInDot;
                }
            }

            return (int) (mTextMargin + mTextHeight * numberOfLines + mDotRadius);
        }
    }

    @Override
    public final int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }
}