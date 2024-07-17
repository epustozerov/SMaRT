package com.dm.smart.items;

import java.io.Serializable;

public class Step implements Serializable {

    public Brush brush;
    public SerializablePath path;
    public int intensity_mark;

    public Step() {
    }

}
