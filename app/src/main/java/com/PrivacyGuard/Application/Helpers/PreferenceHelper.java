package com.PrivacyGuard.Application.Helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.PrivacyGuard.Application.Activities.R;
import com.PrivacyGuard.Application.Database.AppSummary;

import java.util.Comparator;
import java.util.Date;

/**
 * Created by lucas on 05/02/17.
 */

public class PreferenceHelper {
    private static final String RECORD_APP_STATUS_SERVICE_LAST_TIME_RUN = "record_app_status_service_last_time_run";

    public static void setRecordAppStatusServiceLastTimeRun(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(RECORD_APP_STATUS_SERVICE_LAST_TIME_RUN, (new Date()).getTime()).apply();
    }

    public static long getRecordAppStatusServiceLastTimeRun(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(RECORD_APP_STATUS_SERVICE_LAST_TIME_RUN, 0);
    }

    public static int getLeakReportGraphDomainSize(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String leakReportDomainSize = sp.getString(context.getResources().getString(R.string.pref_graph_domain_size_key), "20");
        return Integer.valueOf(leakReportDomainSize);
    }

    public static Comparator<AppSummary> getAppLeakOrder(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String appLeakOrderString = sp.getString(context.getResources().getString(R.string.pref_leak_display_order_key), "1");
        int appLeakOrder = Integer.valueOf(appLeakOrderString);

        switch (appLeakOrder) {
            case 1:
                return DECREASING_ORDER_BY_NUMBER_OF_LEAKS;
            case 2:
                return INCREASING_ORDER_BY_NUMBER_OF_LEAKS;
            case 3:
                return ALPHABETICAL_ORDER;
            default:
                return null;

        }
    }

    private static Comparator<AppSummary> INCREASING_ORDER_BY_NUMBER_OF_LEAKS = new Comparator<AppSummary>() {
        @Override
        public int compare(AppSummary lhs, AppSummary rhs) {
            if (lhs.getTotalLeaks() == rhs.getTotalLeaks()) return 0;
            return lhs.getTotalLeaks() < rhs.getTotalLeaks() ? -1 : 1;
        }
    };

    private static Comparator<AppSummary> DECREASING_ORDER_BY_NUMBER_OF_LEAKS = new Comparator<AppSummary>() {
        @Override
        public int compare(AppSummary lhs, AppSummary rhs) {
            if (lhs.getTotalLeaks() == rhs.getTotalLeaks()) return 0;
            return lhs.getTotalLeaks() < rhs.getTotalLeaks() ? 1 : -1;
        }
    };

    private static Comparator<AppSummary> ALPHABETICAL_ORDER = new Comparator<AppSummary>() {
        @Override
        public int compare(AppSummary lhs, AppSummary rhs) {
            return lhs.getAppName().compareTo(rhs.getAppName());
        }
    };
}
