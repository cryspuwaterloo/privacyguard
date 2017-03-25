package com.PrivacyGuard.Application.Fragments;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.PrivacyGuard.Application.Activities.AppDataActivity;
import com.PrivacyGuard.Application.Activities.R;

/**
 * Created by lucas on 24/03/17.
 */

public class LeakSummaryFragment extends Fragment {

    private String packageName;
    private String appName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appName = getArguments().getString(AppDataActivity.APP_NAME_BUNDLE);
        packageName = getArguments().getString(AppDataActivity.APP_PACKAGE_BUNDLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.leak_summary_fragment, null);

        PackageManager pm = getContext().getPackageManager();
        ImageView appIcon = (ImageView)view.findViewById(R.id.app_icon);
        try {
            appIcon.setImageDrawable(pm.getApplicationIcon(packageName));
        } catch (PackageManager.NameNotFoundException e) {
            appIcon.setImageResource(R.drawable.default_icon);
        }

        TextView appNameText = (TextView)view.findViewById(R.id.app_name);
        appNameText.setText(appName);

        return view;
    }
}
