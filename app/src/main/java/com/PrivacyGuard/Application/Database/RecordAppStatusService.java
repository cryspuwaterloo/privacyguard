package com.PrivacyGuard.Application.Database;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by lucas on 07/04/17.
 */

@TargetApi(22)
public class RecordAppStatusService extends GcmTaskService {
    private boolean hasUsageAccessPermission() {
        AppOpsManager appOps = (AppOpsManager)getApplicationContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getApplicationContext().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @Override
    public int onRunTask(TaskParams params) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 || !hasUsageAccessPermission()) {
            return GcmNetworkManager.RESULT_SUCCESS;
        }

        DatabaseHandler databaseHandler = DatabaseHandler.getInstance(getApplicationContext());

        List<AppSummary> apps = databaseHandler.getAllApps();
        HashSet<String> appPackageNames = new HashSet<>();
        for (AppSummary summary : apps) {
            appPackageNames.add(summary.getPackageName());
        }

        UsageStatsManager usageStatsManager = (UsageStatsManager)getApplicationContext().getSystemService(Context.USAGE_STATS_SERVICE);
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

        return GcmNetworkManager.RESULT_SUCCESS;
    }
}
