package ca.uwaterloo.crysp.privacyguard.Application.Helpers;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.Build;

/**
 * Created by lucas on 21/04/17.
 */

@TargetApi(22)
public class PermissionsHelper {

    public static boolean hasUsageAccessPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager)context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static boolean validBuildVersionForAppUsageAccess() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
    }
}
