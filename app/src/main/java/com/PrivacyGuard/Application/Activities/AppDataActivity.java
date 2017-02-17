package com.PrivacyGuard.Application.Activities;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by lucas on 16/02/17.
 */

@TargetApi(22)
public class AppDataActivity extends AppCompatActivity {

    public static final String APP_NAME_INTENT = "APP_NAME";
    public static final String APP_PACKAGE_INTENT = "PACKAGE_NAME";

    private String packageName;
    private String appName;

    private boolean invalidAndroidVersion = false;

    private View invalidAndroidVersionView;
    private View permissionDisabledView;
    private View contentView;

    private UsageStatsManager usageStatsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_app_data);

        Intent i = getIntent();
        appName = i.getStringExtra(APP_NAME_INTENT);
        packageName = i.getStringExtra(APP_PACKAGE_INTENT);

        invalidAndroidVersionView = findViewById(R.id.invalid_android_version);
        permissionDisabledView = findViewById(R.id.permission_disabled_message);
        contentView = findViewById(R.id.content);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            invalidAndroidVersion = true;
            TextView message = (TextView)findViewById(R.id.invalid_android_version_message);
            message.setText(getString(R.string.invalid_android_version_message, Build.VERSION.SDK_INT, Build.VERSION_CODES.LOLLIPOP_MR1));
            return;
        }

        usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);

        Button turnOnPermissionButton = (Button)findViewById(R.id.turn_on_permission_button);
        turnOnPermissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setViewVisibility();

        if (!invalidAndroidVersion && hasUsageAccessPermission()) {
            setUpData();
        }
    }

    private void setViewVisibility() {
        if (invalidAndroidVersion) {
            invalidAndroidVersionView.setVisibility(View.VISIBLE);
            permissionDisabledView.setVisibility(View.INVISIBLE);
            contentView.setVisibility(View.INVISIBLE);
            return;
        }

        boolean hasPermission = hasUsageAccessPermission();
        permissionDisabledView.setVisibility(hasPermission ? View.INVISIBLE : View.VISIBLE);
        contentView.setVisibility(hasPermission ? View.VISIBLE : View.INVISIBLE);
    }

    private void setUpData() {
        long time = System.currentTimeMillis();
        UsageEvents usageEvents = usageStatsManager.queryEvents(time - TimeUnit.DAYS.toMillis(30), time);

        List<UsageEvents.Event> myUsageEvents = new ArrayList<>();
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);
            if (event.getPackageName().equals(packageName)) {
                myUsageEvents.add(event);
            }
        }
    }

    private boolean hasUsageAccessPermission() {
        AppOpsManager appOps = (AppOpsManager)getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
