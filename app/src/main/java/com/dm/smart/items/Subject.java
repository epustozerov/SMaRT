package com.dm.smart.items;

public class Subject {
    private final String name;
    private final String config;
    private final String bodyScheme;
    private final long timestamp;
    private int id;

    public Subject(int id, String name, String config, String bodyScheme, long timestamp) {
        this.id = id;
        this.name = name;
        this.config = config;
        this.bodyScheme = bodyScheme;
        this.timestamp = timestamp;
    }

    public Subject(String name, String config, String bodyScheme) {
        this.id = -1;
        this.name = name;
        this.config = config;
        this.bodyScheme = bodyScheme;
        this.timestamp = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getConfig() {
        return config;
    }

    public String getBodyScheme() {
        return bodyScheme;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
