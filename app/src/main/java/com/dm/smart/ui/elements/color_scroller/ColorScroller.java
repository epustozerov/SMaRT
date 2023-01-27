package com.dm.smart.ui.elements.color_scroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;


public class ColorScroller extends View {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<Integer> colors = new ArrayList<>();
    private final Paint paint = new Paint();

    public ColorScroller(Context context) {
        this(context, null);
    }

    public ColorScroller(Context context, AttributeSet attrs) {
        super(context, attrs);

    }


    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int offset = 0;
        int width = getWidth() - offset;
        int height = getHeight();

        // Draw background (skip main color)
        double sumLeft = 0;
        double sumRight = 1;
        for (int i = 1; i < colors.size(); i++) {
            int mainColorIndex = 0;
            int color = colors.get((mainColorIndex + i) % colors.size());
            paint.setColor(color);
            double l = 1f;
            canvas.drawRect((float) (sumLeft * width / l), 0, (float) (sumRight * width / l), height, paint);
            double g = .618f;
            sumLeft += Math.pow(g, i - 1);
            sumRight += Math.pow(g, i);
        }
    }
}
