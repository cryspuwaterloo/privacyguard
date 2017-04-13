package com.PrivacyGuard.Application.Database;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

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

    private boolean hasUsageAccessPermission() {
        AppOpsManager appOps = (AppOpsManager)context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @Override
    protected Void doInBackground(Long... params) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 || !hasUsageAccessPermission()) {
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
