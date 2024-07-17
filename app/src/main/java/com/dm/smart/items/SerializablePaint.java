package com.dm.smart.items;

import android.graphics.Paint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SerializablePaint extends Paint implements Serializable {

    public SerializablePaint() {
    }

    public SerializablePaint(Paint paint) {
        super(paint);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Read and set the properties of the Paint class
        this.setColor(in.readInt());
        this.setStrokeWidth(in.readFloat());
        this.setStyle(Style.values()[in.readInt()]);
        this.setStrokeJoin(Join.values()[in.readInt()]);
        this.setStrokeCap(Cap.values()[in.readInt()]);
        this.setAntiAlias(in.readBoolean());
        this.setDither(in.readBoolean());
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        // Write the properties of the Paint class
        out.writeInt(this.getColor());
        out.writeFloat(this.getStrokeWidth());
        out.writeInt(this.getStyle().ordinal());
        out.writeInt(this.getStrokeJoin().ordinal());
        out.writeInt(this.getStrokeCap().ordinal());
        out.writeBoolean(this.isAntiAlias());
        out.writeBoolean(this.isDither());
    }

}