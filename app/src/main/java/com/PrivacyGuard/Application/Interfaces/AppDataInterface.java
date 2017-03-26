package com.PrivacyGuard.Application.Interfaces;

import com.PrivacyGuard.Application.Database.DataLeak;
import com.PrivacyGuard.Plugin.LeakReport;

import java.util.List;

/**
 * Created by lucas on 25/03/17.
 */

public interface AppDataInterface {
    String getAppName();
    String getAppPackageName();
    List<DataLeak> getLeaks(LeakReport.LeakCategory category);
}
