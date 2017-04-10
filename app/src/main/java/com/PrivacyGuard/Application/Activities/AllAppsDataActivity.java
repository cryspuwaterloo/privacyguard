package com.PrivacyGuard.Application.Activities;

import android.os.Bundle;

import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Plugin.LeakReport;

/**
 * Created by lucas on 27/03/17.
 */

public class AllAppsDataActivity extends DataActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DatabaseHandler databaseHandler = DatabaseHandler.getInstance(this);
        locationLeaks = databaseHandler.getAppLeaks(LeakReport.LeakCategory.LOCATION.name());
        contactLeaks = databaseHandler.getAppLeaks(LeakReport.LeakCategory.CONTACT.name());
        deviceLeaks = databaseHandler.getAppLeaks(LeakReport.LeakCategory.DEVICE.name());
        keywordLeaks = databaseHandler.getAppLeaks(LeakReport.LeakCategory.KEYWORD.name());
    }

    @Override
    public String getAppName() {
        return "All Apps";
    }
}
