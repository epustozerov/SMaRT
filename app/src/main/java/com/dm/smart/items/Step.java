package com.dm.smart.items;

import java.io.Serializable;

public class Step implements Serializable {

    public Brush brush;
    public SerializablePath path;
    public int intensity_mark;

    public Step() {
    }

    public Step(Step step) {
        this.brush = new Brush(step.brush);
        this.path = new SerializablePath(step.path);
        this.intensity_mark = step.intensity_mark;
    }
}
