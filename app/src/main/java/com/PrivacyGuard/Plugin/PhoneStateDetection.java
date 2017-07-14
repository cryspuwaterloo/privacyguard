package com.PrivacyGuard.Plugin;

import android.content.Context;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;

import com.PrivacyGuard.Utilities.HashHelpers;
import com.PrivacyGuard.Application.Logger;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by frank on 23/07/14.
 */
public class PhoneStateDetection implements IPlugin {
    private static HashMap<String, String> nameofValue = new HashMap<String, String>();
    private static boolean init = false;
    private final boolean DEBUG = true;
    private final String TAG = PhoneStateDetection.class.getSimpleName();

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

    @Override
    public void setContext(Context context) {
        if(init) return;
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        ArrayList<String> info = new ArrayList<String>();
        String deviceID = telephonyManager.getDeviceId();
        if(deviceID != null && deviceID.length() > 0) {
            if (DEBUG) Logger.d(TAG, "Looking for IMEI: " + deviceID);
            nameofValue.put(deviceID, "IMEI");
            info.add(deviceID);
        }
        String phoneNumber = telephonyManager.getLine1Number();
        if(phoneNumber != null && phoneNumber.length() > 0){
            if (DEBUG) Logger.d(TAG, "Looking for phone number: " + phoneNumber);
            nameofValue.put(phoneNumber, "Phone Number");
            info.add(phoneNumber);
        }
        String subscriberID = telephonyManager.getSubscriberId();
        if(subscriberID != null && subscriberID.length()>0) {
            if (DEBUG) Logger.d(TAG, "Looking for IMSI: " + subscriberID);
            nameofValue.put(subscriberID, "IMSI");
            info.add(subscriberID);
        }
        String androidId = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        if(androidId != null && androidId.length()>0) {
            if (DEBUG) Logger.d(TAG, "Looking for Android ID: " + androidId);
            nameofValue.put(androidId, "Android ID");
            info.add(androidId);
        }
        try {
            String advertisingId = AdvertisingIdClient.getAdvertisingIdInfo(context).getId();
            if (advertisingId != null && advertisingId.length() > 0) {
                if (DEBUG) Logger.d(TAG, "Looking for advertising ID: " + advertisingId);
                nameofValue.put(advertisingId, "Advertising ID");
                info.add(advertisingId);
            }
        } catch (GooglePlayServicesNotAvailableException e) {
        } catch (IOException e) {
        } catch (GooglePlayServicesRepairableException e) {
    }
        for(String key : info) {
            nameofValue.put(HashHelpers.SHA1(key), nameofValue.get(key));
        }
        init = true;
    }
}
