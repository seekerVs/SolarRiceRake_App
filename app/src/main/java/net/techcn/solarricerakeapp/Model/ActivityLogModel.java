package net.techcn.solarricerakeapp.Model;

public class ActivityLogModel {
    private String timestamp;
    private String activity;

    public ActivityLogModel(String timestamp, String activity) {
        this.timestamp = timestamp;
        this.activity = activity;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getActivity() {
        return activity;
    }
}
