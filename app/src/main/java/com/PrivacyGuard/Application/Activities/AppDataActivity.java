package com.PrivacyGuard.Application.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.info:
                AlertDialog alertDialog;

                switch (tabLayout.getSelectedTabPosition()) {
                    case 0:
                        alertDialog = new AlertDialog.Builder(this)
                                .setTitle(R.string.leak_report_title)
                                .setIcon(R.drawable.info_outline)
                                .setMessage(R.string.report_message_single_app)
                                .setPositiveButton(R.string.dialog_accept, null)
                                .create();
                        alertDialog.show();
                        return true;

                    case 1:
                        alertDialog = new AlertDialog.Builder(this)
                                .setTitle(R.string.leak_summary_title)
                                .setIcon(R.drawable.info_outline)
                                .setMessage(R.string.summary_message_single_app)
                                .setPositiveButton(R.string.dialog_accept, null)
                                .create();
                        alertDialog.show();
                        return true;

                    case 2:
                        alertDialog = new AlertDialog.Builder(this)
                                .setTitle(R.string.leak_query_title)
                                .setIcon(R.drawable.info_outline)
                                .setMessage(R.string.query_message_single_app)
                                .setPositiveButton(R.string.dialog_accept, null)
                                .create();
                        alertDialog.show();
                        return true;
                }
        }

        return super.onOptionsItemSelected(item);
    }
}