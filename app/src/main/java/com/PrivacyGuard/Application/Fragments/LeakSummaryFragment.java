package com.PrivacyGuard.Application.Fragments;

import android.content.Context;
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

import com.PrivacyGuard.Application.Activities.R;
import com.PrivacyGuard.Application.Database.DataLeak;
import com.PrivacyGuard.Application.Interfaces.AppDataInterface;
import com.PrivacyGuard.Plugin.LeakReport;
import com.androidplot.pie.PieChart;
import com.androidplot.pie.PieRenderer;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.androidplot.util.PixelUtils;

import java.util.List;

/**
 * Created by lucas on 24/03/17.
 */

public class LeakSummaryFragment extends Fragment {

    public PieChart pie;

    private Segment locationSegment = null;
    private Segment contactSegment = null;
    private Segment deviceSegment = null;
    private Segment keywordSegment = null;

    private AppDataInterface activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (AppDataInterface)context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.leak_summary_fragment, null);

        List<DataLeak> locationLeaks = activity.getLeaks(LeakReport.LeakCategory.LOCATION);
        List<DataLeak> contactLeaks = activity.getLeaks(LeakReport.LeakCategory.CONTACT);
        List<DataLeak> deviceLeaks = activity.getLeaks(LeakReport.LeakCategory.DEVICE);
        List<DataLeak> keywordLeaks = activity.getLeaks(LeakReport.LeakCategory.KEYWORD);

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
            appIcon.setImageDrawable(pm.getApplicationIcon(activity.getAppPackageName()));
        } catch (PackageManager.NameNotFoundException e) {
            appIcon.setImageResource(R.drawable.default_icon);
        }

        TextView appNameText = (TextView)view.findViewById(R.id.app_name);
        appNameText.setText(activity.getAppName());

        pie = (PieChart) view.findViewById(R.id.mySimplePieChart);

        if (locationLeaks.size() > 0) locationSegment = new Segment("", locationLeaks.size());
        if (contactLeaks.size() > 0) contactSegment = new Segment("", contactLeaks.size());
        if (deviceLeaks.size() > 0) deviceSegment = new Segment("", deviceLeaks.size());
        if (keywordLeaks.size() > 0) keywordSegment = new Segment("", keywordLeaks.size());

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

        if (locationSegment != null) pie.addSegment(locationSegment, sfLocation);
        if (contactSegment != null) pie.addSegment(contactSegment, sfContact);
        if (deviceSegment != null) pie.addSegment(deviceSegment, sfDevice);
        if (keywordSegment != null) pie.addSegment(keywordSegment, sfKeyword);

        pie.getRenderer(PieRenderer.class).setDonutSize(0, PieRenderer.DonutMode.PERCENT);

        return view;
    }

    private String getStringPercentage(int size, double total) {
        return String.valueOf((int)Math.round(size*100/total)) + "%";
    }
}
