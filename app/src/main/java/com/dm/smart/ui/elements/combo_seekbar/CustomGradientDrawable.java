package com.dm.smart.ui.elements.combo_seekbar;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import java.util.List;


public class CustomGradientDrawable extends CustomDrawable {
    private final int[] gcolor;

    public CustomGradientDrawable(Drawable base, ComboSeekBar slider, List<ComboSeekBar.Dot> dots,
                                  int color, int[] gcolor, int textSize, boolean isMultiline) {
        super(base, slider, dots, color, textSize, isMultiline);
        this.gcolor = gcolor;
    }

    public void draw(Canvas canvas) {

        Shader s = new LinearGradient(0f, 0f, (float) mySlider.getWidth(),
                (float) mySlider.getHeight(), gcolor, null, Shader.TileMode.CLAMP);
        selectLinePaint.setStrokeWidth(toPix(10));
        selectLinePaint.setShader(s);
        super.draw(canvas);
    }
}