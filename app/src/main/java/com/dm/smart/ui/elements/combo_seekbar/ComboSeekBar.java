package com.dm.smart.ui.elements.combo_seekbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatSeekBar;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL")
public class ComboSeekBar extends AppCompatSeekBar {
    private final List<Dot> mDots = new ArrayList<>();
    private final int mColor;
    private final int mTextSize;
    private final boolean mIsMultiline;
    private final CustomDrawable customDrawable;

    public ComboSeekBar(Context context) {
        super(context);

        mColor = Color.parseColor("#000000");
        mTextSize = (30);
        mIsMultiline = false;

        customDrawable = new CustomDrawable(this.getProgressDrawable(), this, mDots, mColor, mTextSize, false);
        setProgressDrawable(customDrawable);

        setBackground(null);
        setSplitTrack(false);
    }

    public ComboSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        mColor = Color.parseColor("#000000");
        mTextSize = 30;
        mIsMultiline = false;

        customDrawable = new CustomDrawable(this.getProgressDrawable(), this, mDots, mColor, mTextSize, false);
        setProgressDrawable(customDrawable);

        setPadding((int) customDrawable.mDotRadius, 0, (int) customDrawable.mDotRadius, 0);
    }

    @Override
    public void setThumb(Drawable thumb) {
        super.setThumb(thumb);
        super.setSplitTrack(false);

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    public void setColors(int[] gcolor) {
        setProgressDrawable(new CustomGradientDrawable(this.getProgressDrawable(), this, mDots, mColor, gcolor, mTextSize, mIsMultiline));
    }


    @Override
    protected synchronized void onDraw(Canvas canvas) {

        Drawable mThumb = this.getThumb();
        Rect bounds = mThumb.copyBounds();
        bounds.top = -6;
        mThumb.setBounds(bounds);
        super.onDraw(canvas);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        CustomDrawable d = (CustomDrawable) getProgressDrawable();

        int dw = 0;
        int dh = 0;
        if (d != null) {
            dw = d.getIntrinsicWidth();
            dh = d.getIntrinsicHeight();
        }

        dw += getPaddingLeft() + getPaddingRight();
        dh += getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(resolveSize(dw, widthMeasureSpec), resolveSize(dh, heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public static class Dot {
        @SuppressWarnings("unused")
        public String text;
        private boolean id;

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @Override
        public boolean equals(Object o) {
            return ((Dot) o).id == id;
        }
    }
}
