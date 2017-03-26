package com.PrivacyGuard.Application.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.PrivacyGuard.Application.Database.DataLeak;
import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Application.Fragments.LeakReportFragment;
import com.PrivacyGuard.Application.Fragments.LeakSummaryFragment;
import com.PrivacyGuard.Application.Helpers.ActivityRequestCodes;
import com.PrivacyGuard.Application.Interfaces.AppDataInterface;
import com.PrivacyGuard.Plugin.LeakReport;

import java.util.List;

/**
 * Created by lucas on 16/02/17.
 */

public class AppDataActivity extends AppCompatActivity implements AppDataInterface {

    public static final String APP_NAME_INTENT = "APP_NAME";
    public static final String APP_PACKAGE_INTENT = "PACKAGE_NAME";

    private static final int TAB_COUNT = 3;

    private LeakReportFragment leakReportFragment;
    private LeakSummaryFragment leakSummaryFragment;

    private List<DataLeak> locationLeaks;
    private List<DataLeak> contactLeaks;
    private List<DataLeak> deviceLeaks;
    private List<DataLeak> keywordLeaks;

    private String packageName;
    private String appName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_app_data);

        Intent i = getIntent();
        appName = i.getStringExtra(APP_NAME_INTENT);
        packageName = i.getStringExtra(APP_PACKAGE_INTENT);

        leakReportFragment = new LeakReportFragment();
        leakSummaryFragment = new LeakSummaryFragment();

        DatabaseHandler databaseHandler = new DatabaseHandler(this);
        locationLeaks = databaseHandler.getAppLeaks(packageName, LeakReport.LeakCategory.LOCATION.name());
        contactLeaks = databaseHandler.getAppLeaks(packageName, LeakReport.LeakCategory.CONTACT.name());
        deviceLeaks = databaseHandler.getAppLeaks(packageName, LeakReport.LeakCategory.DEVICE.name());
        keywordLeaks = databaseHandler.getAppLeaks(packageName, LeakReport.LeakCategory.KEYWORD.name());
        databaseHandler.close();

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new CustomFragmentPagerAdapter(getSupportFragmentManager(), this));
        viewPager.setOffscreenPageLimit(TAB_COUNT - 1);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ActivityRequestCodes.APP_DATA_PERMISSION_REQUEST) {
            leakReportFragment.permissionSetAttempt();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_data_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.info:
                AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.leak_report_title)
                        .setIcon(R.drawable.info_outline)
                        .setMessage(R.string.graph_message)
                        .setPositiveButton(R.string.dialog_accept, null)
                        .create();
                alertDialog.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class CustomFragmentPagerAdapter extends FragmentPagerAdapter {
        private String tabTitles[] = new String[] { "Report", "Summary", "Query"};
        private Context context;

        public CustomFragmentPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            this.context = context;
        }

        @Override
        public int getCount() {
            return TAB_COUNT;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return leakReportFragment;
                case 1:
                    return leakSummaryFragment;
            }

            return new Fragment();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitles[position];
        }
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
    public List<DataLeak> getLeaks(LeakReport.LeakCategory category) {
        switch (category) {
            case LOCATION:
                return locationLeaks;
            case CONTACT:
                return contactLeaks;
            case DEVICE:
                return deviceLeaks;
            case KEYWORD:
                return keywordLeaks;
        }
        return null;
    }
}