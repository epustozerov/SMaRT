package com.dm.smart.items;

import android.graphics.drawable.Drawable;

import java.io.Serializable;

public class Brush implements Serializable {

    public String title;
    public transient Drawable icon;
    public String type;
    public boolean drawByMove;
    public boolean drawOutside;
    public int thickness;
    public SerializablePaint paint;

    public Brush() {
    }

    public Brush(Brush brush) {
        this.title = brush.title;
        this.icon = brush.icon;
        this.type = brush.type;
        this.drawByMove = brush.drawByMove;
        this.drawOutside = brush.drawOutside;
        this.thickness = brush.thickness;
        this.paint = new SerializablePaint(brush.paint);
    }
}