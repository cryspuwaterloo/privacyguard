package com.PrivacyGuard.Application.Database;

import android.annotation.TargetApi;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.PrivacyGuard.Application.Helpers.PermissionsHelper;
import com.PrivacyGuard.Application.Helpers.PreferenceHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by lucas on 07/04/17.
 */

@TargetApi(22)
public class RecordAppStatusService extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // To run this service, build version must be valid and usage access permission must be granted.
        if (!PermissionsHelper.validBuildVersionForAppUsageAccess() ||
                !PermissionsHelper.hasUsageAccessPermission(context)) {
            return;
        }

        // Only run this service at most once every 5 days.
        if ((new Date()).getTime() - PreferenceHelper.getRecordAppStatusServiceLastTimeRun(context) < TimeUnit.DAYS.toMillis(5)) {
            return;
        }

        // Record the time of this service being run.
        PreferenceHelper.setRecordAppStatusServiceLastTimeRun(context);

        DatabaseHandler databaseHandler = DatabaseHandler.getInstance(context);

        List<AppSummary> apps = databaseHandler.getAllApps();
        HashSet<String> appPackageNames = new HashSet<>();
        for (AppSummary summary : apps) {
            appPackageNames.add(summary.getPackageName());
        }

        UsageStatsManager usageStatsManager = (UsageStatsManager)context.getSystemService(Context.USAGE_STATS_SERVICE);
        long currentTime = (new Date()).getTime();

        UsageEvents usageEvents = usageStatsManager.queryEvents(currentTime - TimeUnit.DAYS.toMillis(30), currentTime);

        List<UsageEvents.Event> appUsageEvents = new ArrayList<>();
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);
            if (appPackageNames.contains(event.getPackageName()) &&
                    (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND ||
                     event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND)) {
                appUsageEvents.add(event);
            }
        }

        HashSet<AppStatusEvent> databaseStatusEvents = new HashSet<>();
        databaseStatusEvents.addAll(databaseHandler.getAppStatusEvents());

        for (UsageEvents.Event event : appUsageEvents) {
            int foreground = event.getEventType() ==
                    UsageEvents.Event.MOVE_TO_FOREGROUND ? DatabaseHandler.FOREGROUND_STATUS : DatabaseHandler.BACKGROUND_STATUS;
            AppStatusEvent temp = new AppStatusEvent(event.getPackageName(), event.getTimeStamp(), foreground);
            if (!databaseStatusEvents.contains(temp)) {
                databaseHandler.addAppStatusEvent(event.getPackageName(), event.getTimeStamp(), foreground);
            }
        }
    }
}
