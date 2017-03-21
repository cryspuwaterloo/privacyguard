package com.PrivacyGuard.Application.Activities;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.PrivacyGuard.Application.Database.DataLeak;
import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Application.Helpers.ActivityRequestCodes;
import com.PrivacyGuard.Application.Helpers.PreferenceHelper;
import com.PrivacyGuard.Plugin.LeakReport;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepFormatter;
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

    private boolean invalidAndroidVersion = false;

    private View invalidAndroidVersionView;
    private View permissionDisabledView;
    private RelativeLayout contentView;
    private ImageButton navigateLeft;
    private ImageButton navigateRight;
    private TextView graphTitleText;

    private UsageStatsManager usageStatsManager;
    private DatabaseHandler databaseHandler;

    //Maps a date to an int[] that contains the count of each type of leak.
    private Map<Date, int[]> leakMap = new HashMap<>();
    private List<Date> leakMapKeys = new ArrayList<>();
    private int currentKeyIndex = -1;
    private XYPlot plot;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_app_data);

        Intent i = getIntent();
        String appName = i.getStringExtra(APP_NAME_INTENT);
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
                startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), ActivityRequestCodes.APP_DATA_PERMISSION_REQUEST);
            }
        });

        plot = (XYPlot)findViewById(R.id.plot);

        navigateLeft = (ImageButton)findViewById(R.id.navigate_left);
        navigateRight = (ImageButton)findViewById(R.id.navigate_right);
        navigateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentKeyIndex--;
                setGraphBounds();
                setUpDisplay();
            }
        });
        navigateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentKeyIndex++;
                setGraphBounds();
                setUpDisplay();
            }
        });

        graphTitleText = (TextView)findViewById(R.id.graph_title_text);

        PackageManager pm = getPackageManager();
        ImageView appIcon = (ImageView)findViewById(R.id.app_icon);
        try {
            appIcon.setImageDrawable(pm.getApplicationIcon(packageName));
        } catch (PackageManager.NameNotFoundException e) {
            appIcon.setImageResource(R.drawable.default_icon);
        }

        TextView appNameText = (TextView)findViewById(R.id.app_name);
        appNameText.setText(appName);

        setViewVisibility();

        if (!invalidAndroidVersion && hasUsageAccessPermission()) {
            setUpGraph();
            setGraphBounds();
            setUpDisplay();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ActivityRequestCodes.APP_DATA_PERMISSION_REQUEST) {
            if (hasUsageAccessPermission()) {
                setViewVisibility();
                setUpGraph();
                setGraphBounds();
                setUpDisplay();
            }
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

    private void setUpDisplay() {
        boolean navigateRightEnabled = currentKeyIndex < leakMapKeys.size() - 1;
        boolean navigateLeftEnabled = currentKeyIndex > 0;
        navigateRight.setEnabled(navigateRightEnabled);
        navigateRight.setAlpha(navigateRightEnabled ? 1.0f : 0.3f);
        navigateLeft.setEnabled(navigateLeftEnabled);
        navigateLeft.setAlpha(navigateLeftEnabled ? 1.0f : 0.3f);
    }

    private void setGraphBounds() {
        Date centerDate = leakMapKeys.get(currentKeyIndex);
        long centerMillis = centerDate.getTime();
        int halfRange = PreferenceHelper.getLeakReportGraphDomainSize(this)/2;
        long range = 1000 * halfRange;

        long domainLowerBound = centerMillis - range;
        long domainUpperBound = centerMillis + range;
        plot.setDomainBoundaries(domainLowerBound, domainUpperBound, BoundaryMode.FIXED);
        plot.setDomainStep(StepMode.INCREMENT_BY_VAL, (halfRange/5) * 1000);

        DateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.CANADA);
        String lowerBoundDate = dateFormat.format(new Date(domainLowerBound));
        String upperBoundDate = dateFormat.format(new Date(domainUpperBound));
        if (lowerBoundDate.equals(upperBoundDate)) {
            graphTitleText.setText(lowerBoundDate);
        } else {
            graphTitleText.setText(lowerBoundDate + " - " + upperBoundDate);
        }

        int rangeUpperBound = 0;

        int searchIndex = currentKeyIndex;
        while(searchIndex >=0 && leakMapKeys.get(searchIndex).getTime() >= domainLowerBound) {
            int[] summary = leakMap.get(leakMapKeys.get(searchIndex));
            for (int i : summary) {
                if (i > rangeUpperBound) {
                    rangeUpperBound = i;
                }
            }
            searchIndex--;
        }

        searchIndex = currentKeyIndex;
        while(searchIndex < leakMapKeys.size() && leakMapKeys.get(searchIndex).getTime() <= domainUpperBound) {
            int[] summary = leakMap.get(leakMapKeys.get(searchIndex));
            for (int i : summary) {
                if (i > rangeUpperBound) {
                    rangeUpperBound = i;
                }
            }
            searchIndex++;
        }

        rangeUpperBound++;
        rangeUpperBound = rangeUpperBound + rangeUpperBound % 2;
        plot.setRangeBoundaries(0, rangeUpperBound, BoundaryMode.FIXED);

        plot.redraw();
    }

    //Plot all the data on the graph. Should only be called once.
    private void setUpGraph() {
        //First, aggregate the usage events for the app that we are interested in (Foreground/Background).
        long currentTime = System.currentTimeMillis();
        UsageEvents usageEvents = usageStatsManager.queryEvents(currentTime - TimeUnit.DAYS.toMillis(30), currentTime);

        List<UsageEvents.Event> appUsageEvents = new ArrayList<>();
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);
            if (event.getPackageName().equals(packageName) &&
                    (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND ||
                     event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND)) {
                appUsageEvents.add(event);
            }
        }

        int maxNumberOfLeaks = 0;

        //Next, aggregate the leaks for the app by date and category.
        for (LeakReport.LeakCategory category : LeakReport.LeakCategory.values()) {
            List<DataLeak> leaks = databaseHandler.getAppLeaks(packageName, category.name());
            for (DataLeak leak : leaks) {
                int[] summary = leakMap.get(leak.timestampDate);
                if (summary == null) {
                    summary = new int[LeakReport.LeakCategory.values().length];
                    leakMap.put(leak.timestampDate, summary);
                }
                summary[category.ordinal()]++;
                int value = summary[category.ordinal()];
                if (value > maxNumberOfLeaks) {
                    maxNumberOfLeaks = value;
                }
            }
        }

        maxNumberOfLeaks++;
        maxNumberOfLeaks = maxNumberOfLeaks + maxNumberOfLeaks % 2;
        leakMapKeys.addAll(leakMap.keySet());
        Collections.sort(leakMapKeys);

        currentKeyIndex = leakMapKeys.size() - 1;

        int[] lineFormats = {R.xml.point_formatter_location, R.xml.point_formatter_contact, R.xml.point_formatter_device, R.xml.point_formatter_keyword};

        plot.setRangeStep(StepMode.INCREMENT_BY_VAL, 1);
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new GraphDomainFormat());
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new GraphRangeFormat());
        plot.getGraph().getDomainCursorPaint().setTextSize(10);
        plot.getGraph().setPaddingLeft(30);
        plot.getGraph().setPaddingRight(30);
        plot.getGraph().setPaddingBottom(100);
        plot.getLayoutManager().remove(plot.getLegend());
        plot.getLayoutManager().remove(plot.getDomainTitle());
        plot.getLayoutManager().remove(plot.getTitle());

        Paint lineFillForeground = new Paint();
        lineFillForeground.setColor(getResources().getColor(R.color.app_status_green));
        lineFillForeground.setAlpha(70);

        Paint lineFillBackground = new Paint();
        lineFillBackground.setColor(getResources().getColor(R.color.app_status_red));
        lineFillBackground.setAlpha(70);

        Paint lineFillNoData = new Paint();
        lineFillNoData.setColor(getResources().getColor(R.color.blue));
        lineFillNoData.setAlpha(70);

        StepFormatter stepFormatterForeground  = new StepFormatter(Color.WHITE, Color.WHITE);
        stepFormatterForeground.setVertexPaint(null);
        stepFormatterForeground.getLinePaint().setStrokeWidth(0);
        stepFormatterForeground.setFillPaint(lineFillForeground);

        StepFormatter stepFormatterBackground  = new StepFormatter(Color.WHITE, Color.WHITE);
        stepFormatterBackground.setVertexPaint(null);
        stepFormatterBackground.getLinePaint().setStrokeWidth(0);
        stepFormatterBackground.setFillPaint(lineFillBackground);

        StepFormatter stepFormatterNoData  = new StepFormatter(Color.WHITE, Color.WHITE);
        stepFormatterNoData.setVertexPaint(null);
        stepFormatterNoData.getLinePaint().setStrokeWidth(0);
        stepFormatterNoData.setFillPaint(lineFillNoData);

        SimpleXYSeries seriesForeground = new SimpleXYSeries(null);
        SimpleXYSeries seriesBackground = new SimpleXYSeries(null);
        SimpleXYSeries seriesNoData = new SimpleXYSeries(null);
        UsageEvents.Event lastEvent = null;
        UsageEvents.Event firstEvent = null;
        for (UsageEvents.Event event : appUsageEvents) {
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                seriesForeground.addLast(event.getTimeStamp(), 0);
                seriesForeground.addLast(event.getTimeStamp(), maxNumberOfLeaks);

                seriesBackground.addLast(event.getTimeStamp(), maxNumberOfLeaks);
                seriesBackground.addLast(event.getTimeStamp(), 0);
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                seriesForeground.addLast(event.getTimeStamp(), maxNumberOfLeaks);
                seriesForeground.addLast(event.getTimeStamp(), 0);

                seriesBackground.addLast(event.getTimeStamp(), 0);
                seriesBackground.addLast(event.getTimeStamp(), maxNumberOfLeaks);
            }

            lastEvent = event;
            if (firstEvent == null) firstEvent = event;
        }

        if (firstEvent == null) {
            seriesNoData.addFirst(currentTime, maxNumberOfLeaks);
            seriesNoData.addFirst(0, maxNumberOfLeaks);
        }
        else {
            seriesNoData.addFirst(firstEvent.getTimeStamp(), maxNumberOfLeaks);
            seriesNoData.addFirst(0, maxNumberOfLeaks);
        }

        if (lastEvent != null) {
            if (lastEvent.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                throw new RuntimeException("Should not happen.");
            }
            if (lastEvent.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                seriesBackground.addLast(currentTime, maxNumberOfLeaks);
            }
        }

        plot.addSeries(seriesForeground, stepFormatterForeground);
        plot.addSeries(seriesBackground, stepFormatterBackground);
        plot.addSeries(seriesNoData, stepFormatterNoData);

        List<SimpleXYSeries> leakSeries = new ArrayList<>();
        for (int i = 0; i < LeakReport.LeakCategory.values().length; i++) {
            leakSeries.add(new SimpleXYSeries(null));
        }

        for (Date date : leakMapKeys) {
            int[] summary = leakMap.get(date);
            for (LeakReport.LeakCategory category : LeakReport.LeakCategory.values()) {
                int index = category.ordinal();
                if (summary[index] > 0) {
                    leakSeries.get(index).addLast(date.getTime(), summary[index]);
                }
            }
        }

        for (int i = 0; i < leakSeries.size(); i++) {
            plot.addSeries(leakSeries.get(i), new LineAndPointFormatter(this, lineFormats[i]));
        }

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
}
