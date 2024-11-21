package net.techcn.solarricerakeapp.Model;

public class BatteryStatusModel {
    private String timestamp;
    private String percentage;
    private String status;

    public BatteryStatusModel(String timestamp, String percentage, String status) {
        this.timestamp = timestamp;
        this.percentage = percentage;
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getPercentage() {
        return percentage;
    }

    public String getStatus() {
        return status;
    }
}
