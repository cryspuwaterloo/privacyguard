package com.PrivacyGuard.Application.Helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.PrivacyGuard.Application.Activities.R;
import com.PrivacyGuard.Application.Database.AppSummary;

import java.util.Comparator;

/**
 * Created by lucas on 05/02/17.
 */

public class PreferenceHelper {

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
                return null;
            case 2:
                return INCREASING_ORDER_BY_NUMBER_OF_LEAKS;
            case 3:
                return DECREASING_ORDER_BY_NUMBER_OF_LEAKS;
            case 4:
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
