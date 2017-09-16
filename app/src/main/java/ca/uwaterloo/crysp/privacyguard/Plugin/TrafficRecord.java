package ca.uwaterloo.crysp.privacyguard.Plugin;
import android.content.Context;

/**
 * Created by rongjunyan on 2017-08-07.
 */

public class TrafficRecord {

    private final String TAG = "TrafficRecord";
    private final boolean DEBUG = false;

    private static boolean init = false;

    public TrafficReport handle(String msg) {
        int data = msg.length();
        TrafficReport report = new TrafficReport();
        report.size = data;
        return report;
    }

    public void setContext(Context context) {
        if (init) return;
        init = true;
    }
}
