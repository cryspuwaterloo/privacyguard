package ca.uwaterloo.crysp.privacyguard.Application.Interfaces;

import ca.uwaterloo.crysp.privacyguard.Application.Database.DataLeak;
import ca.uwaterloo.crysp.privacyguard.Plugin.LeakReport;

import java.util.List;

/**
 * Created by lucas on 25/03/17.
 */

public interface AppDataInterface {
    String getAppName();
    String getAppPackageName();
    List<DataLeak> getLeaks(LeakReport.LeakCategory category);
}
