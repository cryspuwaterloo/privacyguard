package com.PrivacyGuard.Application.Network;

import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Plugin.IPlugin;
import com.PrivacyGuard.Plugin.LeakReport;
import com.PrivacyGuard.Application.Network.FakeVPN.MyVpnService;

import java.util.concurrent.LinkedBlockingQueue;

public class FilterThread extends Thread {
    private static final String TAG = FilterThread.class.getSimpleName();
    private static final boolean DEBUG = false;
    private LinkedBlockingQueue<FilterMsg> toFilter = new LinkedBlockingQueue<>();
    private MyVpnService vpnService;
    private String appName, packageName, destIP;
    private int srcPort, destPort;

    public FilterThread(MyVpnService vpnService) {
        this.vpnService= vpnService;
    }

    public FilterThread(MyVpnService vpnService, String appName, String packageName, int srcPort, String destIP, int destPort) {
        this.vpnService= vpnService;
        this.appName = appName;
        this.packageName = packageName;
        this.srcPort = srcPort;
        this.destIP = destIP;
        this.destPort = destPort;
    }

    public void offer(String msg, String appName, String packageName, int srcPort, String destIP, int destPort) {
        FilterMsg filterData = new FilterMsg(msg, appName, packageName, srcPort, destIP, destPort);
        toFilter.offer(filterData);
    }

    public void filter(String msg) {
        filter(msg, appName, packageName, srcPort, destIP, destPort);
    }

    public void filter(String msg, String appName, String packageName, int srcPort, String destIP, int destPort) {

        Logger.logTraffic(packageName, appName, srcPort, destIP, destPort, msg);

        for (IPlugin plugin : vpnService.getNewPlugins()) {
            LeakReport leak = plugin.handleRequest(msg);
            if (leak != null) {
                leak.appName = appName;
                leak.packageName = packageName;
                vpnService.notify(msg, leak);
                if (DEBUG) Logger.v(TAG, appName + " is leaking " + leak.category.name());
                Logger.logLeak(leak.category.name());
            }
        }
    }

    public void run() {
        try {
            while (!interrupted()) {
                FilterMsg temp = toFilter.take();
                filter(temp.msg, temp.packageName, temp.appName, temp.srcPort, temp.destIP, temp.destPort);
            }
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }

    class FilterMsg {
        String msg, packageName, appName, destIP;
        int srcPort, destPort;

        FilterMsg(String msg, String packageName, String appName, int srcPort, String destIP, int destPort) {
            this.msg = msg;
            this.packageName = packageName;
            this.appName = appName;
            this.srcPort = srcPort;
            this.destIP = destIP;
            this.destPort = destPort;
        }
    }
}
