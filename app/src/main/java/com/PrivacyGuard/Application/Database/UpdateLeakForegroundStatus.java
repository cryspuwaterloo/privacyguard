package com.PrivacyGuard.Application.Database;

import android.annotation.TargetApi;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.AsyncTask;

import com.PrivacyGuard.Application.Helpers.PermissionsHelper;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by lucas on 10/04/17.
 */

@TargetApi(22)
public class UpdateLeakForegroundStatus extends AsyncTask<Long, Void, Void> {
    private Context context;

    public UpdateLeakForegroundStatus(Context context) {
        super();
        this.context = context;
    }

    @Override
    protected Void doInBackground(Long... params) {
        // To run this task, build version must be valid and usage access permission must be granted.
        if (!PermissionsHelper.validBuildVersionForAppUsageAccess() ||
                !PermissionsHelper.hasUsageAccessPermission(context)) {
            return null;
        }

        long id = params[0];
        DatabaseHandler databaseHandler = DatabaseHandler.getInstance(context);
        DataLeak leak = databaseHandler.getLeakById(id);

        long leakTime = leak.getTimestampDate().getTime();

        UsageStatsManager usageStatsManager = (UsageStatsManager)context.getSystemService(Context.USAGE_STATS_SERVICE);
        long currentTime = (new Date()).getTime();

        UsageEvents usageEvents = usageStatsManager.queryEvents(currentTime - TimeUnit.DAYS.toMillis(1), currentTime);

        UsageEvents.Event lastEvent = null;
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);
            if (event.getTimeStamp() > leakTime) break;

            if (event.getPackageName().equals(leak.getPackageName()) &&
                    (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                     event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND)) {
                lastEvent = event;
            }
        }

        if (lastEvent == null) throw new RuntimeException("Failed to retrieve app status.");

        if (lastEvent.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
            databaseHandler.setDataLeakStatus(id, DatabaseHandler.FOREGROUND_STATUS);
        }
        else {
            databaseHandler.setDataLeakStatus(id, DatabaseHandler.BACKGROUND_STATUS);
        }

        return null;
    }
}
