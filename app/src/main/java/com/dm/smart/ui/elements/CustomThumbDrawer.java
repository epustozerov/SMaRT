package com.dm.smart.ui.elements;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.rtugeek.android.colorseekbar.AlphaSeekBar;
import com.rtugeek.android.colorseekbar.BaseSeekBar;
import com.rtugeek.android.colorseekbar.ColorSeekBar;
import com.rtugeek.android.colorseekbar.thumb.DefaultThumbDrawer;

public class CustomThumbDrawer extends DefaultThumbDrawer {

    private final Paint thumbStrokePaint = new Paint();
    private final Paint thumbSolidPaint = new Paint();
    private final Paint thumbColorPaint = new Paint();
    private final Path outerCircle = new Path();
    private final Path innerCircle = new Path();

    public CustomThumbDrawer(int size, int ringSolidColor, int ringBorderColor) {
        super(size, ringSolidColor, ringBorderColor);
        thumbStrokePaint.setAntiAlias(true);
        thumbSolidPaint.setAntiAlias(true);
        thumbColorPaint.setAntiAlias(true);

        thumbStrokePaint.setStyle(Paint.Style.STROKE);

        setRingBorderColor(ringBorderColor);
        setRingSolidColor(ringSolidColor);
        setRingBorderSize(3);
    }

    @Override
    public void onDrawThumb(RectF thumbBounds, BaseSeekBar seekBar, Canvas canvas) {
        float centerX = thumbBounds.centerX();
        float centerY = thumbBounds.centerY();
        outerCircle.reset();
        innerCircle.reset();
        if (seekBar instanceof ColorSeekBar) {
            thumbColorPaint.setColor(((ColorSeekBar) seekBar).getColor());
        } else if (seekBar instanceof AlphaSeekBar) {
            thumbColorPaint.setAlpha(((AlphaSeekBar) seekBar).getAlphaValue());
        }
        float outerRadius = thumbBounds.height() / 2f;
        int ringSize = 5;
        float innerRadius = outerRadius - ringSize;
        outerCircle.addCircle(centerX, centerY, outerRadius, Path.Direction.CW);
        innerCircle.addCircle(centerX, centerY, innerRadius, Path.Direction.CW);
        outerCircle.op(innerCircle, Path.Op.DIFFERENCE);
        canvas.drawCircle(centerX, centerY, innerRadius, thumbColorPaint);
        canvas.drawRect(thumbBounds.left, centerY - 3, thumbBounds.right, centerY + 3, thumbSolidPaint);
        canvas.drawPath(outerCircle, thumbSolidPaint);
        canvas.drawPath(outerCircle, thumbStrokePaint);
    }

}
