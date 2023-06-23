package com.dm.smart.items;

public class Record {
    private final int id;
    private final int patient_id;
    private int n;

    private final String sensations;
    private final long timestamp;

    public Record(int id, int patient_id, int n, String sensations, long timestamp) {
        this.id = id;
        this.patient_id = patient_id;
        this.n = n;
        this.sensations = sensations;
        this.timestamp = timestamp;
    }

    public Record(int patient_id, String sensations) {
        this.id = -1;
        this.n = -1;
        this.patient_id = patient_id;
        this.sensations = sensations;
        this.timestamp = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public int getSubjectId() {
        return patient_id;
    }

    public int getN() {
        return n;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSensations() {
        return sensations;
    }

    public void setN(int n) {
        this.n = n;
    }
}
