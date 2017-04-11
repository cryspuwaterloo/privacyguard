package com.PrivacyGuard.Application.Database;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

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
    private Context context;

    private boolean hasUsageAccessPermission() {
        AppOpsManager appOps = (AppOpsManager)context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        String strAction = intent.getAction();
        if (!strAction.equals(Intent.ACTION_SCREEN_ON)) throw new RuntimeException("Nooo");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 || !hasUsageAccessPermission()) {
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
            int foreground = event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND ? 1 : 0;
            AppStatusEvent temp = new AppStatusEvent(event.getPackageName(), event.getTimeStamp(), foreground);
            if (!databaseStatusEvents.contains(temp)) {
                databaseHandler.addAppStatusEvent(event.getPackageName(), event.getTimeStamp(), foreground);
            }
        }
    }
}
