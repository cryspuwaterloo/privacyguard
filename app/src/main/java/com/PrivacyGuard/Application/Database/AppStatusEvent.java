package com.PrivacyGuard.Application.Database;

/**
 * Created by lucas on 10/04/17.
 */

public class AppStatusEvent {
    private String packageName;
    private long timeStamp;
    private boolean foreground;

    public AppStatusEvent(String packageName, long timeStamp, int foreground) {
        this.packageName = packageName;
        this.timeStamp = timeStamp;
        this.foreground = foreground == 1;
    }

    public String getPackageName() {
        return packageName;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public boolean getForeground() {
        return foreground;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AppStatusEvent)) {
            return false;
        }

        AppStatusEvent event = (AppStatusEvent)o;

        return packageName.equals(event.packageName) &&
                timeStamp == event.timeStamp &&
                foreground == event.foreground;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + packageName.hashCode();
        result = 31 * result + Long.valueOf(timeStamp).hashCode();
        result = 31 * result + Boolean.valueOf(foreground).hashCode();
        return result;
    }
}
