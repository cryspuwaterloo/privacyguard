package com.PrivacyGuard.Application.Activities;

import android.annotation.TargetApi;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by lucas on 16/02/17.
 */

public class AppDataActivity extends AppCompatActivity {

    public static final String APP_NAME_INTENT = "APP_NAME";
    public static final String APP_PACKAGE_INTENT = "PACKAGE_NAME";

    UsageStatsManager usageStatsManager;

    @Override
    @TargetApi(22)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_app_data);

        Intent i = getIntent();
        String appName = i.getStringExtra(APP_NAME_INTENT);
        String packageName = i.getStringExtra(APP_PACKAGE_INTENT);

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1){
            Toast.makeText(this, "Sorry, this feature is not supported in your version of Android", Toast.LENGTH_LONG).show();
        }

        usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);

        long time = System.currentTimeMillis();
        System.out.println(TimeUnit.DAYS.toMillis(30));
        UsageEvents usageEvents = usageStatsManager.queryEvents(time - TimeUnit.DAYS.toMillis(30), time);

        List<UsageEvents.Event> myUsageEvents = new ArrayList<>();
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);
            System.out.println(event.getPackageName() + "    ~~~!!!!!00");
            if (event.getPackageName().equals(packageName)) {
                myUsageEvents.add(event);
            }
        }

        Toast.makeText(this, "" + myUsageEvents.size(), Toast.LENGTH_LONG).show();

        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), 0);
    }
}
