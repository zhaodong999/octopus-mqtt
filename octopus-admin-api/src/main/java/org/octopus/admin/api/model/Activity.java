package org.octopus.admin.api.model;

public class Activity {

    private String key;
    private String activity;
    private String type;
    private int participants;
    private double price;

    public Activity(String key, String activity, String type, int participants, double price) {
        this.key = key;
        this.activity = activity;
        this.type = type;
        this.participants = participants;
        this.price = price;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getParticipants() {
        return participants;
    }

    public void setParticipants(int participants) {
        this.participants = participants;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
