package net.techcn.solarricerakeapp.Model;

public class DryingHistoryModel {
    private String startTime;
    private String endTime;
    private String duration;
    private String mode;

    public DryingHistoryModel(String startTime, String endTime, String duration, String mode) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.mode = mode;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getDuration() {
        return duration;
    }

    public String getMode() {
        return mode;
    }
}
