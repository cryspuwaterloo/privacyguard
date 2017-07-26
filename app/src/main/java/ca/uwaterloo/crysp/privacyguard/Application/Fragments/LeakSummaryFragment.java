package ca.uwaterloo.crysp.privacyguard.Application.Fragments;

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

import ca.uwaterloo.crysp.privacyguard.R;
import ca.uwaterloo.crysp.privacyguard.Application.Database.DataLeak;
import ca.uwaterloo.crysp.privacyguard.Application.Database.DatabaseHandler;
import ca.uwaterloo.crysp.privacyguard.Application.Interfaces.AppDataInterface;
import ca.uwaterloo.crysp.privacyguard.Plugin.LeakReport;
import com.androidplot.pie.PieChart;
import com.androidplot.pie.PieRenderer;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.androidplot.util.PixelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lucas on 24/03/17.
 */

public class LeakSummaryFragment extends Fragment {

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

        List<DataLeak> allLeaks = new ArrayList<>();
        allLeaks.addAll(locationLeaks);
        allLeaks.addAll(contactLeaks);
        allLeaks.addAll(deviceLeaks);
        allLeaks.addAll(keywordLeaks);

        int foreground = 0;
        int background = 0;
        int unspecified = 0;
        for (DataLeak leak : allLeaks) {
            if (leak.getForegroundStatus() == DatabaseHandler.FOREGROUND_STATUS) foreground++;
            if (leak.getForegroundStatus() == DatabaseHandler.BACKGROUND_STATUS) background++;
            if (leak.getForegroundStatus() == DatabaseHandler.UNSPECIFIED_STATUS) unspecified++;
        }

        double total = allLeaks.size();

        TextView locationPercentage = (TextView)view.findViewById(R.id.location_percentage);
        locationPercentage.setText(getStringPercentage(locationLeaks.size(), total));

        TextView contactPercentage = (TextView)view.findViewById(R.id.contact_percentage);
        contactPercentage.setText(getStringPercentage(contactLeaks.size(), total));

        TextView devicePercentage = (TextView)view.findViewById(R.id.device_percentage);
        devicePercentage.setText(getStringPercentage(deviceLeaks.size(), total));

        TextView keywordPercentage = (TextView)view.findViewById(R.id.keyword_percentage);
        keywordPercentage.setText(getStringPercentage(keywordLeaks.size(), total));

        TextView foregroundPercentage = (TextView)view.findViewById(R.id.foreground_percentage);
        foregroundPercentage.setText(getStringPercentage(foreground, total));

        TextView backgroundPercentage = (TextView)view.findViewById(R.id.background_percentage);
        backgroundPercentage.setText(getStringPercentage(background, total));

        PackageManager pm = getContext().getPackageManager();
        ImageView appIcon = (ImageView)view.findViewById(R.id.app_icon);
        try {
            appIcon.setImageDrawable(pm.getApplicationIcon(activity.getAppPackageName()));
        } catch (PackageManager.NameNotFoundException e) {
            appIcon.setImageResource(R.drawable.default_icon);
        }

        TextView appNameText = (TextView)view.findViewById(R.id.app_name);
        appNameText.setText(activity.getAppName());

        Segment locationSegment = null;
        Segment contactSegment = null;
        Segment deviceSegment = null;
        Segment keywordSegment = null;

        Segment foregroundSegment = null;
        Segment backgroundSegment = null;
        Segment unspecifiedSegment = null;

        PieChart categoryPieChart = (PieChart) view.findViewById(R.id.category_pie_chart);
        PieChart foregroundPieChart = (PieChart) view.findViewById(R.id.foreground_pie_chart);

        if (locationLeaks.size() > 0) locationSegment = new Segment("", locationLeaks.size());
        if (contactLeaks.size() > 0) contactSegment = new Segment("", contactLeaks.size());
        if (deviceLeaks.size() > 0) deviceSegment = new Segment("", deviceLeaks.size());
        if (keywordLeaks.size() > 0) keywordSegment = new Segment("", keywordLeaks.size());

        if (foreground > 0) foregroundSegment = new Segment("", foreground);
        if (background > 0) backgroundSegment = new Segment("", background);
        if (unspecified > 0) unspecifiedSegment = new Segment("", unspecified);

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

        SegmentFormatter sfForeground = new SegmentFormatter(getResources().getColor(R.color.app_status_green));
        sfForeground.getLabelPaint().setTextSize(fontSize);
        sfForeground.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
        sfForeground.getFillPaint().setMaskFilter(emf);

        SegmentFormatter sfBackground = new SegmentFormatter(getResources().getColor(R.color.app_status_red));
        sfBackground.getLabelPaint().setTextSize(fontSize);
        sfBackground.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
        sfBackground.getFillPaint().setMaskFilter(emf);

        SegmentFormatter sfUnspecified = new SegmentFormatter(getResources().getColor(R.color.blue));
        sfUnspecified.getLabelPaint().setTextSize(fontSize);
        sfUnspecified.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
        sfUnspecified.getFillPaint().setMaskFilter(emf);

        if (locationSegment != null) categoryPieChart.addSegment(locationSegment, sfLocation);
        if (contactSegment != null) categoryPieChart.addSegment(contactSegment, sfContact);
        if (deviceSegment != null) categoryPieChart.addSegment(deviceSegment, sfDevice);
        if (keywordSegment != null) categoryPieChart.addSegment(keywordSegment, sfKeyword);

        if (foregroundSegment != null) foregroundPieChart.addSegment(foregroundSegment, sfForeground);
        if (backgroundSegment != null) foregroundPieChart.addSegment(backgroundSegment, sfBackground);
        if (unspecifiedSegment != null) foregroundPieChart.addSegment(unspecifiedSegment, sfUnspecified);

        categoryPieChart.getRenderer(PieRenderer.class).setDonutSize(0, PieRenderer.DonutMode.PERCENT);
        foregroundPieChart.getRenderer(PieRenderer.class).setDonutSize(0, PieRenderer.DonutMode.PERCENT);

        return view;
    }

    private String getStringPercentage(int size, double total) {
        return String.valueOf((int)Math.round(size*100/total)) + "%";
    }
}
