package ca.uwaterloo.crysp.privacyguard.Plugin;

import android.content.Context;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;

import ca.uwaterloo.crysp.privacyguard.Utilities.HashHelpers;
import ca.uwaterloo.crysp.privacyguard.Application.Logger;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by frank on 23/07/14.
 */
public class DeviceDetection implements IPlugin {
    private static HashMap<String, String> nameofValue = new HashMap<String, String>();
    private static boolean init = false;
    private final boolean DEBUG = false;
    private final String TAG = DeviceDetection.class.getSimpleName();

    @Override
    @Nullable
    public LeakReport handleRequest(String request) {
        ArrayList<LeakInstance> leaks = new ArrayList<>();
        for(String key : nameofValue.keySet()) {
            if (request.contains(key)){
                leaks.add(new LeakInstance(nameofValue.get(key),key));
            }
        }
        if(leaks.isEmpty()){
            return null;
        }
        LeakReport rpt = new LeakReport(LeakReport.LeakCategory.DEVICE);
        rpt.addLeaks(leaks);
        return rpt;
    }

    @Override
    public LeakReport handleResponse(String response) {
        return null;
    }

    @Override
    public String modifyRequest(String request) {
        return request;
    }

    @Override
    public String modifyResponse(String response) {
        return response;
    }

    private void addIdentifiers(String type, String content) {
        nameofValue.put(content, type);
        if (DEBUG) Logger.d(TAG, "Looking for " + type + ": " + content);
        nameofValue.put(HashHelpers.SHA1(content, 8), "SHA1 of " + type);
        if (DEBUG) Logger.d(TAG, "Looking for its SHA1: " + HashHelpers.SHA1(content, 8));
        nameofValue.put(HashHelpers.SHA2(content, 8), "SHA2 of " + type);
        if (DEBUG) Logger.d(TAG, "Looking for its SHA2: " + HashHelpers.SHA2(content, 8));
        nameofValue.put(HashHelpers.MD5(content, 8), "MD5 of " + type);
        if (DEBUG) Logger.d(TAG, "Looking for its MD5 : " + HashHelpers.MD5(content, 8));
    }

    @Override
    public void setContext(Context context) {
        synchronized (nameofValue) {
            if (init) return;
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String deviceID = telephonyManager.getDeviceId();
            if (deviceID != null && deviceID.length() > 0) {
                addIdentifiers("IMEI", deviceID);
            }
            String phoneNumber = telephonyManager.getLine1Number();
            if (phoneNumber != null && phoneNumber.length() > 0) {
                addIdentifiers("Phone Number", phoneNumber);
            }
            String subscriberID = telephonyManager.getSubscriberId();
            if (subscriberID != null && subscriberID.length() > 0) {
                addIdentifiers("IMSI", subscriberID);
            }
            String SIMCardSerial = telephonyManager.getSimSerialNumber();
            if (SIMCardSerial != null && SIMCardSerial.length() > 0) {
                addIdentifiers("SIM Card Serial Number", SIMCardSerial);
            }
            String androidId = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            if (androidId != null && androidId.length() > 0) {
                addIdentifiers("Android ID", androidId);
            }
            try {
                String advertisingId = AdvertisingIdClient.getAdvertisingIdInfo(context).getId();
                if (advertisingId != null && advertisingId.length() > 0) {
                    addIdentifiers("Advertising ID", advertisingId);
                }
            } catch (GooglePlayServicesNotAvailableException e) {
            } catch (IOException e) {
            } catch (GooglePlayServicesRepairableException e) {
            }

            init = true;
        }
    }
}
