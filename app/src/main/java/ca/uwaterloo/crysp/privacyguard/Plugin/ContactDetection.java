package ca.uwaterloo.crysp.privacyguard.Plugin;

import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.provider.ContactsContract;

import ca.uwaterloo.crysp.privacyguard.Application.Logger;
import ca.uwaterloo.crysp.privacyguard.Utilities.StringUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by frank on 23/07/14.
 */
public class ContactDetection implements IPlugin {
    private final String TAG = "ContactDetection";
    private final boolean DEBUG = false;

    private static boolean init = false;

    private static final HashSet<String> emailList = new HashSet<>();
    private static final HashSet<String> phoneList = new HashSet<>();

    @Override
    @Nullable
    public LeakReport handleRequest(String request) {
        ArrayList<LeakInstance> leaks = new ArrayList<>();

        // don't do regex based search for email/phone since this would assume that a) we can define such regex and
        // b) app implementators ensure that their phone numbers/email addresses follow these regex
        for (String phoneNumber: phoneList) {
            if (request.contains(phoneNumber)) {
                leaks.add(new LeakInstance("Contact Phone Number", phoneNumber));
            }
        }
        for (String email: emailList) {
            if (request.contains(email)) {
                leaks.add(new LeakInstance("Contact Email Address", email));
            }
        }

        if(leaks.isEmpty()){
            return null;
        }
        LeakReport rpt = new LeakReport(LeakReport.LeakCategory.CONTACT);
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
        synchronized (emailList) {
            if (init) return;
            // TODO: this way we will miss new/updated contacts, but it is more efficient than
            // re-reading the contacts for each new connection; can we ask to be notified when
            // there is a new/updated contact?
            init = true;
            getNumber(context.getContentResolver());
            getEmail(context.getContentResolver());
        }
    }


    public void getNumber(ContentResolver cr)
    {
        Cursor phones = null;
        try {
            phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            int phoneNumberIdx = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            if(phones.moveToFirst()){
                do {
                    String phoneNumber = phones.getString(phoneNumberIdx);
                    if (DEBUG) Logger.d(TAG, "contact phone number: " + phoneNumber);
                    phoneList.add(phoneNumber);
                } while (phones.moveToNext());
            }
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
        } finally {
            if (phones != null) {
                phones.close();
            }
        }
    }
    public void getEmail(ContentResolver cr)
    {
        Cursor emails = null;
        try {
            emails = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);
            int emailAddressIdx = emails.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);

            if(emails.moveToFirst()){
                do {
                    String email = emails.getString(emailAddressIdx);
                    if (DEBUG) Logger.d(TAG, "contact email address: " + email);
                    emailList.add(email);
                    if (DEBUG) Logger.d(TAG, "contact email address: " + StringUtil.encodeAt(email));
                    emailList.add(StringUtil.encodeAt(email));
                } while (emails.moveToNext());
            }

        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
        } finally {
            if (emails != null) {
                emails.close();
            }
        }
    }

}
