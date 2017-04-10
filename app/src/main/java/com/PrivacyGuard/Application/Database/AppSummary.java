package com.PrivacyGuard.Application.Database;

public class AppSummary{
    private String packageName;
    private String appName;
    private int totalLeaks;
    private int ignore;

    public AppSummary(String packageName, String appName, int totalLeaks, int ignore){
        this.packageName = packageName;
        this.appName = appName;
        this.totalLeaks = totalLeaks;
        this.ignore = ignore;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public int getTotalLeaks() {
        return totalLeaks;
    }

    public int getIgnore() {
        return ignore;
    }
}
