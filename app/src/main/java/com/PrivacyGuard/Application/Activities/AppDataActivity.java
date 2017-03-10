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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.PrivacyGuard.Application.Database.DataLeak;
import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Plugin.LeakReport;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private RelativeLayout contentView;
    private ImageButton navigateLeft;
    private ImageButton navigateRight;

    private UsageStatsManager usageStatsManager;
    private DatabaseHandler databaseHandler;

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
        contentView = (RelativeLayout)findViewById(R.id.content);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            invalidAndroidVersion = true;
            TextView message = (TextView)findViewById(R.id.invalid_android_version_message);
            message.setText(getString(R.string.invalid_android_version_message, Build.VERSION.SDK_INT, Build.VERSION_CODES.LOLLIPOP_MR1));
            return;
        }

        usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        databaseHandler = new DatabaseHandler(this);

        Button turnOnPermissionButton = (Button)findViewById(R.id.turn_on_permission_button);
        turnOnPermissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            }
        });

        navigateLeft = (ImageButton)findViewById(R.id.navigate_left);
        navigateRight = (ImageButton)findViewById(R.id.navigate_right);
        navigateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentKeyIndex--;
                setUpGraph();
            }
        });
        navigateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentKeyIndex++;
                setUpGraph();
            }
        });

        ImageButton infoButton = (ImageButton)findViewById(R.id.info_button);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alertDialog = new AlertDialog.Builder(v.getContext())
                        .setTitle(R.string.leak_report_title)
                        .setIcon(R.drawable.info_outline)
                        .setMessage(R.string.graph_message)
                        .setPositiveButton(R.string.dialog_accept, null)
                        .create();
                alertDialog.show();
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
            //boolean retVal = invalidAndroidVersionView.getVisibility() == View.VISIBLE;
            invalidAndroidVersionView.setVisibility(View.VISIBLE);
            permissionDisabledView.setVisibility(View.INVISIBLE);
            contentView.setVisibility(View.INVISIBLE);
            return;
        }

        boolean hasPermission = hasUsageAccessPermission();
        permissionDisabledView.setVisibility(hasPermission ? View.INVISIBLE : View.VISIBLE);
        contentView.setVisibility(hasPermission ? View.VISIBLE : View.INVISIBLE);
    }

    private List<UsageEvents.Event> appUsageEvents = new ArrayList<>();
    private int currentKeyIndex = -1;
    private Map<Date, List<DataLeak>> organizedLeakMap = new HashMap<>();
    private List<Date> organizedLeakMapKeys = new ArrayList<>();

    private void setUpData() {
        long time = System.currentTimeMillis();
        UsageEvents usageEvents = usageStatsManager.queryEvents(time - TimeUnit.DAYS.toMillis(30), time);

        appUsageEvents.clear();
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);
            if (event.getPackageName().equals(packageName)) {
                appUsageEvents.add(event);
            }
        }

        organizedLeakMap.clear();
        for (LeakReport.LeakCategory category : LeakReport.LeakCategory.values()) {
            List<DataLeak> leaks = databaseHandler.getAppLeaks(packageName, category.name());
            for (DataLeak leak : leaks) {
                List<DataLeak> list = organizedLeakMap.get(leak.timestampDate);
                if (list == null) {
                    list = new ArrayList<>();
                    organizedLeakMap.put(leak.timestampDate, list);
                }
                list.add(leak);
            }
        }

        organizedLeakMapKeys.clear();
        organizedLeakMapKeys.addAll(organizedLeakMap.keySet());
        Collections.sort(organizedLeakMapKeys);

        currentKeyIndex = organizedLeakMapKeys.size() - 1;

        setUpGraph();
    }

    private void setUpGraph() {
        boolean navigateRightEnabled = currentKeyIndex < organizedLeakMapKeys.size() - 1;
        boolean navigateLeftEnabled = currentKeyIndex > 0;
        navigateRight.setEnabled(navigateRightEnabled);
        navigateRight.setAlpha(navigateRightEnabled ? 1.0f : 0.3f);
        navigateLeft.setEnabled(navigateLeftEnabled);
        navigateLeft.setAlpha(navigateLeftEnabled ? 1.0f : 0.3f);

        XYPlot plot = (XYPlot) findViewById(R.id.plot);
        plot.clear();

        Date centerDate = organizedLeakMapKeys.get(currentKeyIndex);
        long centerMillis = centerDate.getTime();
        long range = 1000 * 10;

        long domainLowerBound = centerMillis - range;
        long domainUpperBound = centerMillis + range;
        plot.setDomainBoundaries(centerMillis - range, centerMillis + range, BoundaryMode.FIXED);
        int rangeUpperBound = 0;

        LeakReport.LeakCategory[] leakCategories = LeakReport.LeakCategory.values();
        List<Map<String, Integer>> leakMaps = new ArrayList<>();
        int[] lineFormats = {R.xml.point_formatter_location, R.xml.point_formatter_contact, R.xml.point_formatter_device, R.xml.point_formatter_keyword};

        for (int i = 0; i < leakCategories.length; i++) {
            leakMaps.add(new HashMap<String, Integer>());
        }
        
        int searchIndex = currentKeyIndex;
        while (searchIndex >= 0) {
            Date date = organizedLeakMapKeys.get(searchIndex);
            if (date.getTime() < domainLowerBound) {
                break;
            }

            for (DataLeak leak : organizedLeakMap.get(date)) {
                LeakReport.LeakCategory category = LeakReport.LeakCategory.valueOf(leak.category);
                Map<String, Integer> map = leakMaps.get(category.ordinal());
                Integer value = map.get(leak.timestamp);
                if (value == null) {
                    value = 0;
                }
                map.put(leak.timestamp, value + 1);
                if (value + 1 > rangeUpperBound) rangeUpperBound = value + 1;
            }
            
            searchIndex--;
        }
        
        searchIndex = currentKeyIndex + 1;
        while (searchIndex < organizedLeakMapKeys.size()) {
            Date date = organizedLeakMapKeys.get(searchIndex);
            if (date.getTime() > domainUpperBound) {
                break;
            }

            for (DataLeak leak : organizedLeakMap.get(date)) {
                LeakReport.LeakCategory category = LeakReport.LeakCategory.valueOf(leak.category);
                Map<String, Integer> map = leakMaps.get(category.ordinal());
                Integer value = map.get(leak.timestamp);
                if (value == null) {
                    value = 0;
                }
                map.put(leak.timestamp, value + 1);
                if (value + 1 > rangeUpperBound) rangeUpperBound = value + 1;
            }

            searchIndex++;
        }

        for (int i = 0; i < leakCategories.length; i++) {
            SimpleXYSeries series = new SimpleXYSeries(leakCategories[i].name().substring(0, 1));
            Map<String, Integer> leaks = leakMaps.get(i);
            for(String timeStamp : leaks.keySet()) {
                series.addLast(databaseHandler.getDateFromTimestamp(timeStamp).getTime(), leaks.get(timeStamp));
            }
            plot.addSeries(series, new LineAndPointFormatter(this, lineFormats[i]));
        }

        rangeUpperBound++;
        plot.setRangeBoundaries(0, rangeUpperBound + rangeUpperBound % 2, BoundaryMode.FIXED);
        plot.setRangeStep(StepMode.INCREMENT_BY_VAL, 1);
        plot.setDomainStep(StepMode.INCREMENT_BY_VAL, 2000);
        DateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.CANADA);
        plot.setTitle(dateFormat.format(new Date(domainUpperBound)));
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new GraphDomainFormat());
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new GraphRangeFormat());
        plot.getGraph().getDomainCursorPaint().setTextSize(10);
        plot.getGraph().setPaddingLeft(40);
        plot.getGraph().setPaddingRight(40);
        plot.getGraph().setPaddingBottom(100);
        plot.getLayoutManager().remove(plot.getLegend());
        plot.getLayoutManager().remove(plot.getDomainTitle());
        plot.redraw();
    }

    private static class GraphDomainFormat extends Format {
        private static DateFormat dateFormat = new SimpleDateFormat("h:mm:ss aa", Locale.CANADA);

        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            long millis = ((Number) obj).longValue();
            return toAppendTo.append(dateFormat.format(new Date(millis)));
        }
        @Override
        public Object parseObject(String source, ParsePosition pos) {
            return null;
        }
    }

    private static class GraphRangeFormat extends Format {

        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            int quantity = ((Number) obj).intValue();
            return toAppendTo.append(quantity % 2 == 0 ? quantity : "");
        }
        @Override
        public Object parseObject(String source, ParsePosition pos) {
            return null;
        }
    }

    private boolean hasUsageAccessPermission() {
        AppOpsManager appOps = (AppOpsManager)getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
