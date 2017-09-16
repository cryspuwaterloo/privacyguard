package ca.uwaterloo.crysp.privacyguard.Application.Activities;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;

import ca.uwaterloo.crysp.privacyguard.Application.Database.DatabaseHandler;
import ca.uwaterloo.crysp.privacyguard.Application.Database.Traffic;
import ca.uwaterloo.crysp.privacyguard.Plugin.LeakReport;
import ca.uwaterloo.crysp.privacyguard.R;

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

        trafficsOutE = databaseHandler.getTraffics(true, true);
        trafficsInE = databaseHandler.getTraffics(true, false);
        trafficsOutNe = databaseHandler.getTraffics(false, true);
        trafficsInNe = databaseHandler.getTraffics(false, false);
    }

    @Override
    public String getAppName() {
        return "All Apps";
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
                                .setMessage(R.string.report_message_all_apps)
                                .setPositiveButton(R.string.dialog_accept, null)
                                .create();
                        alertDialog.show();
                        return true;

                    case 1:
                        alertDialog = new AlertDialog.Builder(this)
                                .setTitle(R.string.leak_summary_title)
                                .setIcon(R.drawable.info_outline)
                                .setMessage(R.string.summary_message_all_apps)
                                .setPositiveButton(R.string.dialog_accept, null)
                                .create();
                        alertDialog.show();
                        return true;

                    case 2:
                        alertDialog = new AlertDialog.Builder(this)
                                .setTitle(R.string.leak_query_title)
                                .setIcon(R.drawable.info_outline)
                                .setMessage(R.string.query_message_all_apps)
                                .setPositiveButton(R.string.dialog_accept, null)
                                .create();
                        alertDialog.show();
                        return true;

                    case 3:
                        alertDialog = new AlertDialog.Builder(this)
                                .setTitle(R.string.traffic_title)
                                .setIcon(R.drawable.info_outline)
                                .setMessage(R.string.traffic_message_all_apps)
                                .setPositiveButton(R.string.dialog_accept, null)
                                .create();
                        alertDialog.show();
                        return true;

                }
        }

        return super.onOptionsItemSelected(item);
    }
}
