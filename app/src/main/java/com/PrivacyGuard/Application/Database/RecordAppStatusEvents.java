package com.PrivacyGuard.Application.Database;

import android.annotation.TargetApi;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.PrivacyGuard.Application.Helpers.PermissionsHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by lucas on 07/04/17.
 */

@TargetApi(22)
public class RecordAppStatusEvents extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals("android.intent.action.DATE_CHANGED")) {
            // Just for testing purposes, document how many times this service has been called.
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putLong("date_changed", sp.getLong("date_changed", 0) + 1).apply();
        }

        if (intent.getAction().equals("android.intent.action.ACTION_SHUTDOWN")) {
            // Just for testing purposes, document how many times this service has been called.
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putLong("shutdown_action", sp.getLong("shutdown_action", 0) + 1).apply();
        }

        if (intent.getAction().equals("android.intent.action.QUICKBOOT_POWEROFF")) {
            // Just for testing purposes, document how many times this service has been called.
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putLong("quick_boot", sp.getLong("quick_boot", 0) + 1).apply();
        }

        // Just for testing purposes, document how many times this service has been called.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong("service_called", sp.getLong("service_called", 0) + 1).apply();

        // To run this service, build version must be valid and usage access permission must be granted.
        if (!PermissionsHelper.validBuildVersionForAppUsageAccess() ||
                !PermissionsHelper.hasUsageAccessPermission(context)) {
            return;
        }

        DatabaseHandler databaseHandler = DatabaseHandler.getInstance(context);

        List<AppSummary> apps = databaseHandler.getAllApps();
        HashSet<String> appPackageNames = new HashSet<>();
        for (AppSummary summary : apps) {
            appPackageNames.add(summary.getPackageName());
        }

        UsageStatsManager usageStatsManager = (UsageStatsManager)context.getSystemService(Context.USAGE_STATS_SERVICE);
        long currentTime = (new Date()).getTime();

        // This receiver is called every time the phone powers off or there is a date change in the phone.
        // So unrecorded status events can exist at most 1 day in the past. Look 2 days in the past to ensure
        // that all status events are recorded.
        UsageEvents usageEvents = usageStatsManager.queryEvents(currentTime - TimeUnit.DAYS.toMillis(2), currentTime);

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
