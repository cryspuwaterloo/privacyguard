package com.PrivacyGuard.Application.Activities;

import android.content.Intent;
import android.os.Bundle;

import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Plugin.LeakReport;

/**
 * Created by lucas on 16/02/17.
 */

public class AppDataActivity extends DataActivity {

    public static final String APP_NAME_INTENT = "APP_NAME";
    public static final String APP_PACKAGE_INTENT = "PACKAGE_NAME";

    private String packageName;
    private String appName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        appName = i.getStringExtra(APP_NAME_INTENT);
        packageName = i.getStringExtra(APP_PACKAGE_INTENT);

        DatabaseHandler databaseHandler = DatabaseHandler.getInstance(this);
        locationLeaks = databaseHandler.getAppLeaks(packageName, LeakReport.LeakCategory.LOCATION.name());
        contactLeaks = databaseHandler.getAppLeaks(packageName, LeakReport.LeakCategory.CONTACT.name());
        deviceLeaks = databaseHandler.getAppLeaks(packageName, LeakReport.LeakCategory.DEVICE.name());
        keywordLeaks = databaseHandler.getAppLeaks(packageName, LeakReport.LeakCategory.KEYWORD.name());
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public String getAppPackageName() {
        return packageName;
    }
}