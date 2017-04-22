package com.PrivacyGuard.Application.Activities;

import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.PrivacyGuard.Application.Database.CategorySummary;
import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Application.PrivacyGuard;

import java.util.List;

public class AppSummaryActivity extends AppCompatActivity {

    private String packageName;
    private String appName;
    private int ignore;
    private ListView list;
    private SummaryListViewAdapter adapter;
    private Switch notificationSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_app_summary);

        // Get the message from the intent
        Intent intent = getIntent();
        packageName= intent.getStringExtra(PrivacyGuard.EXTRA_PACKAGE_NAME);
        appName = intent.getStringExtra(PrivacyGuard.EXTRA_APP_NAME);
        ignore = intent.getIntExtra(PrivacyGuard.EXTRA_IGNORE,0);

        TextView title = (TextView) findViewById(R.id.summary_title);
        title.setText(appName);
        TextView subtitle = (TextView) findViewById(R.id.summary_subtitle);
        subtitle.setText("[" + packageName + "]");

        notificationSwitch = (Switch) findViewById(R.id.summary_switch);
        notificationSwitch.setChecked(ignore == 1);
        notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DatabaseHandler db = DatabaseHandler.getInstance(AppSummaryActivity.this);
                if (isChecked) {
                    // The toggle is enabled
                    db.setIgnoreApp(packageName, true);
                    ignore = 1;
                } else {
                    // The toggle is disabled
                    db.setIgnoreApp(packageName, false);
                    ignore = 0;
                }
            }
        });

        list = (ListView) findViewById(R.id.summary_list);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CategorySummary category = (CategorySummary) parent.getItemAtPosition(position);
                Intent intent;
                if(category.category.equalsIgnoreCase("location")){
                    intent = new Intent(AppSummaryActivity.this, LocationDetailActivity.class);
                }else{
                    intent = new Intent(AppSummaryActivity.this, DetailActivity.class);
                }

                intent.putExtra(PrivacyGuard.EXTRA_ID, category.notifyId);
                intent.putExtra(PrivacyGuard.EXTRA_PACKAGE_NAME, packageName);
                intent.putExtra(PrivacyGuard.EXTRA_APP_NAME, appName);
                intent.putExtra(PrivacyGuard.EXTRA_CATEGORY, category.category);
                intent.putExtra(PrivacyGuard.EXTRA_IGNORE, category.ignore);

                startActivity(intent);
            }
        });

        FloatingActionButton viewStats = (FloatingActionButton)findViewById(R.id.stats_button);
        viewStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), AppDataActivity.class);
                i.putExtra(AppDataActivity.APP_NAME_INTENT, appName);
                i.putExtra(AppDataActivity.APP_PACKAGE_INTENT, packageName);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateList();
    }

    private void updateList(){
        DatabaseHandler db = DatabaseHandler.getInstance(this);
        List<CategorySummary> details = db.getAppDetail(packageName);

        if (details == null) {
            return;
        }
        if (adapter == null) {
            adapter = new SummaryListViewAdapter(this, details);
            list.setAdapter(adapter);
        } else {
            adapter.updateData(details);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent upIntent = getParentActivityIntent();
                if (shouldUpRecreateTask(upIntent)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                                    // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    navigateUpTo(upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
