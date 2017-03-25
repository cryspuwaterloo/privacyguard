package com.PrivacyGuard.Application.Fragments;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.PrivacyGuard.Application.Activities.AppDataActivity;
import com.PrivacyGuard.Application.Activities.R;
import com.PrivacyGuard.Application.Database.DataLeak;
import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Plugin.LeakReport;
import com.androidplot.pie.PieChart;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.androidplot.util.PixelUtils;

import java.util.List;

/**
 * Created by lucas on 24/03/17.
 */

public class LeakSummaryFragment extends Fragment {

    public static final int SELECTED_SEGMENT_OFFSET = 50;

    public PieChart pie;

    private Segment locationSegment;
    private Segment contactSegment;
    private Segment deviceSegment;
    private Segment keywordSegment;

    List<DataLeak> locationLeaks;
    List<DataLeak> contactLeaks;
    List<DataLeak> deviceLeaks;
    List<DataLeak> keywordLeaks;

    private String packageName;
    private String appName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appName = getArguments().getString(AppDataActivity.APP_NAME_BUNDLE);
        packageName = getArguments().getString(AppDataActivity.APP_PACKAGE_BUNDLE);

        DatabaseHandler databaseHandler = new DatabaseHandler(getContext());
        locationLeaks = databaseHandler.getAppLeaks(packageName, LeakReport.LeakCategory.LOCATION.name());
        contactLeaks = databaseHandler.getAppLeaks(packageName, LeakReport.LeakCategory.CONTACT.name());
        deviceLeaks = databaseHandler.getAppLeaks(packageName, LeakReport.LeakCategory.DEVICE.name());
        keywordLeaks = databaseHandler.getAppLeaks(packageName, LeakReport.LeakCategory.KEYWORD.name());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.leak_summary_fragment, null);

        double total = locationLeaks.size() + contactLeaks.size() + deviceLeaks.size() + keywordLeaks.size();

        TextView locationPercentage = (TextView)view.findViewById(R.id.location_percentage);
        locationPercentage.setText(getStringPercentage(locationLeaks.size(), total));

        TextView contactPercentage = (TextView)view.findViewById(R.id.contact_percentage);
        contactPercentage.setText(getStringPercentage(contactLeaks.size(), total));

        TextView devicePercentage = (TextView)view.findViewById(R.id.device_percentage);
        devicePercentage.setText(getStringPercentage(deviceLeaks.size(), total));

        TextView keywordPercentage = (TextView)view.findViewById(R.id.keyword_percentage);
        keywordPercentage.setText(getStringPercentage(keywordLeaks.size(), total));

        PackageManager pm = getContext().getPackageManager();
        ImageView appIcon = (ImageView)view.findViewById(R.id.app_icon);
        try {
            appIcon.setImageDrawable(pm.getApplicationIcon(packageName));
        } catch (PackageManager.NameNotFoundException e) {
            appIcon.setImageResource(R.drawable.default_icon);
        }

        TextView appNameText = (TextView)view.findViewById(R.id.app_name);
        appNameText.setText(appName);

        pie = (PieChart) view.findViewById(R.id.mySimplePieChart);

        locationSegment = new Segment("", locationLeaks.size());
        contactSegment = new Segment("", contactLeaks.size());
        deviceSegment = new Segment("", deviceLeaks.size());
        keywordSegment = new Segment("", keywordLeaks.size());

        final float fontSize = PixelUtils.spToPix(30);

        EmbossMaskFilter emf = new EmbossMaskFilter(
                new float[]{1, 1, 1}, 0.4f, 10, 8.2f);

        SegmentFormatter sfLocation = new SegmentFormatter(getResources().getColor(R.color.location_color));
        sfLocation.getLabelPaint().setTextSize(fontSize);
        sfLocation.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
        sfLocation.getFillPaint().setMaskFilter(emf);

        SegmentFormatter sfContact = new SegmentFormatter(getResources().getColor(R.color.contact_color));
        sfContact.getLabelPaint().setTextSize(fontSize);
        sfContact.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
        sfContact.getFillPaint().setMaskFilter(emf);

        SegmentFormatter sfDevice = new SegmentFormatter(getResources().getColor(R.color.device_color));
        sfDevice.getLabelPaint().setTextSize(fontSize);
        sfDevice.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
        sfDevice.getFillPaint().setMaskFilter(emf);

        SegmentFormatter sfKeyword = new SegmentFormatter(getResources().getColor(R.color.keyword_color));
        sfKeyword.getLabelPaint().setTextSize(fontSize);
        sfKeyword.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
        sfKeyword.getFillPaint().setMaskFilter(emf);

        pie.addSegment(locationSegment, sfLocation);
        pie.addSegment(contactSegment, sfContact);
        pie.addSegment(deviceSegment, sfDevice);
        pie.addSegment(keywordSegment, sfKeyword);

        return view;
    }

    private String getStringPercentage(int size, double total) {
        return String.valueOf((int)(size*100/total)) + "%";
    }
}
