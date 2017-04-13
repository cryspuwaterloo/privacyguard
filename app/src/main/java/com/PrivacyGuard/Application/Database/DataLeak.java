package com.PrivacyGuard.Application.Database;

import java.text.ParseException;
import java.util.Date;

public class DataLeak {

    private String packageName;
    private String appName;
    private String category;
    private String type;
    private String leakContent;
    private String timestamp;
    private Date timestampDate;
    private int foregroundStatus;

    public DataLeak(String packageName, String appName, String category, String type, String content, String timestamp, int foregroundStatus){
        this.packageName = packageName;
        this.appName = appName;
        this.category = category;
        this.type = type;
        this.leakContent = content;
        this.timestamp = timestamp;
        this.foregroundStatus = foregroundStatus;

        try {
            this.timestampDate = DatabaseHandler.getDateFormat().parse(timestamp);
        }
        catch (ParseException ex) {
            throw new RuntimeException("Invalid timestamp for DataLeak, tried to parse: " + timestamp);
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public String getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public String getLeakContent() {
        return leakContent;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Date getTimestampDate() {
        return timestampDate;
    }

    public int getForegroundStatus() {
        return foregroundStatus;
    }
}
