package net.techcn.solarricerakeapp.Model;

public class MoistureHistoryModel {
    private String description;
    private String timestamp;
    private String value;

    public MoistureHistoryModel(String description, String timestamp, String value) {
        this.description = description;
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getValue() {
        return value;
    }
}
