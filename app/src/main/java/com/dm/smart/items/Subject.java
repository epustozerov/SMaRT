package com.dm.smart.items;

public class Subject {
    private final int id;
    private final String name;
    private final String gender;
    private final long timestamp;

    public Subject(int id, String name, String gender, long timestamp) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.timestamp = timestamp;
    }

    public Subject(String name, String gender) {
        this.id = -1;
        this.name = name;
        this.gender = gender;
        this.timestamp = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
