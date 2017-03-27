package com.PrivacyGuard.Application.Activities;

/**
 * Created by lucas on 27/03/17.
 */

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
import com.PrivacyGuard.Application.Fragments.LeakQueryFragment;
import com.PrivacyGuard.Application.Fragments.LeakReportFragment;
import com.PrivacyGuard.Application.Fragments.LeakSummaryFragment;
import com.PrivacyGuard.Application.Helpers.ActivityRequestCodes;
import com.PrivacyGuard.Application.Interfaces.AppDataInterface;
import com.PrivacyGuard.Plugin.LeakReport;

import java.util.List;

public class DataActivity extends AppCompatActivity implements AppDataInterface {

    private static final int TAB_COUNT = 3;

    private LeakReportFragment leakReportFragment;
    private LeakSummaryFragment leakSummaryFragment;
    private LeakQueryFragment leakQueryFragment;

    protected List<DataLeak> locationLeaks;
    protected List<DataLeak> contactLeaks;
    protected List<DataLeak> deviceLeaks;
    protected List<DataLeak> keywordLeaks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_app_data);

        leakReportFragment = new LeakReportFragment();
        leakSummaryFragment = new LeakSummaryFragment();
        leakQueryFragment = new LeakQueryFragment();

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

        public CustomFragmentPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
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
                case 2:
                    return leakQueryFragment;
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitles[position];
        }
    }

    @Override
    public String getAppName() {
        return null;
    }

    @Override
    public String getAppPackageName() {
        return null;
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

