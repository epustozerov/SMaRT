package com.dm.smart.items;

public class Record {
    private final int id;
    private final int patient_id;
    private final long timestamp;

    public Record(int id, int patient_id, long timestamp) {
        this.id = id;
        this.patient_id = patient_id;
        this.timestamp = timestamp;
    }

    public Record(int patient_id) {
        this.id = -1;
        this.patient_id = patient_id;
        this.timestamp = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public int getPatientId() {
        return patient_id;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
