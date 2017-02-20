package com.PrivacyGuard.Application.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Patterns;

import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Plugin.LeakInstance;
import com.PrivacyGuard.Plugin.LeakReport;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;

/**
 * Created by MAK on 03/11/2015.
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "dataLeaksManager";

    // DataLeaks table name
    private static final String TABLE_DATA_LEAKS = "data_leaks";
    private static final String TABLE_LEAK_SUMMARY = "leak_summary";

    // DataLeaks Table Columns names
    private static final String KEY_ID = "_id";
    private static final String KEY_NAME = "app_name";
    private static final String KEY_PACKAGE = "package_name";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_TYPE = "type";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_TIME_STAMP = "time_stamp";

    // ------------------------------ w3kim@uwaterloo.ca -----------------------------

    private static final String TAG = DatabaseHandler.class.getSimpleName();

    // URL Table Name
    private static final String TABLE_URL = "urls";
    // URL Table Column names
    private static final String KEY_URL_ID = "_id";
    private static final String KEY_URL_APP_NAME = "app_name";
    private static final String KEY_URL_PACKAGE = "package_name";
    private static final String KEY_URL_URL = "url";
    private static final String KEY_URL_HOST = "host";
    private static final String KEY_URL_RES = "res";
    private static final String KEY_URL_QUERY_PARAMS = "query_params";

    private static final String KEY_URL_TIMESTAMP = "_timestamp";

    private static final String CREATE_URL_TABLE = "CREATE TABLE " + TABLE_URL + "("
            + KEY_URL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_URL_APP_NAME + " TEXT,"
            + KEY_URL_PACKAGE + " TEXT,"
            + KEY_URL_URL + " TEXT,"
            + KEY_URL_HOST + " TEXT,"
            + KEY_URL_RES + " TEXT,"
            + KEY_URL_QUERY_PARAMS + " TEXT,"
            + KEY_URL_TIMESTAMP + " TEXT )";
    private static final String KEY_FREQUENCY = "frequency";

    // ------------------------------ w3kim@uwaterloo.ca -----------------------------
    private static final String KEY_IGNORE = "ignore";
    private static final String CREATE_DATA_LEAKS_TABLE = "CREATE TABLE " + TABLE_DATA_LEAKS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_PACKAGE + " TEXT,"
            + KEY_NAME + " TEXT,"
            + KEY_CATEGORY + " TEXT,"
            + KEY_TYPE + " TEXT,"
            + KEY_CONTENT + " TEXT,"
            + KEY_TIME_STAMP + " TEXT" + ")";
    private static final String[] DATA_LEAK_TABLE_COLUMNS = new String[]{KEY_ID, KEY_PACKAGE, KEY_NAME, KEY_CATEGORY, KEY_TYPE, KEY_CONTENT, KEY_TIME_STAMP};
    private static final String CREATE_LEAK_SUMMARY_TABLE = "CREATE TABLE " + TABLE_LEAK_SUMMARY + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_PACKAGE + " TEXT,"
            + KEY_NAME + " TEXT,"
            + KEY_CATEGORY + " TEXT,"
            + KEY_FREQUENCY + " INTEGER,"
            + KEY_IGNORE + " INTEGER" + ")";
    private static final String[] LEAK_SUMMARY_TABLE_COLUMNS = new String[]{KEY_ID, KEY_PACKAGE, KEY_NAME, KEY_CATEGORY, KEY_FREQUENCY, KEY_IGNORE};
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private SQLiteDatabase mDB;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mDB = getReadableDatabase();
    }

    public String[] getTables() {
        return new String[]{TABLE_DATA_LEAKS, TABLE_LEAK_SUMMARY, TABLE_URL};
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        //create table data_leaks
        db.execSQL(CREATE_DATA_LEAKS_TABLE);
        db.execSQL(CREATE_LEAK_SUMMARY_TABLE);
        // w3kim@uwaterloo.ca
        db.execSQL(CREATE_URL_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA_LEAKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEAK_SUMMARY);
        // w3kim@uwaterloo.ca
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_URL);
        // Create tables again
        onCreate(db);
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    void resetDataLeakTable() {
        mDB.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA_LEAKS);
        mDB.execSQL(CREATE_DATA_LEAKS_TABLE);
    }

    void resetLeakSummaryTable() {
        mDB.execSQL("DROP TABLE IF EXISTS " + TABLE_LEAK_SUMMARY);
        mDB.execSQL(CREATE_LEAK_SUMMARY_TABLE);
    }

    // w3kim@uwaterloo.ca
    private void addUrl(String appName, String packageName, String url, String host, String res, String queryParams) {
        ContentValues values = new ContentValues();
        values.put(KEY_URL_PACKAGE, packageName);
        values.put(KEY_URL_APP_NAME, appName);
        values.put(KEY_URL_TIMESTAMP, mDateFormat.format(new Date()));
        values.put(KEY_URL_HOST, host);
        values.put(KEY_URL_RES, res);
        values.put(KEY_URL_QUERY_PARAMS, queryParams);
        values.put(KEY_URL_URL, url);

        mDB.insert(TABLE_URL, null, values);
    }

    // Adding new data leak
    private void addDataLeak(String packageName, String appName, String category, String type, String content) {
        ContentValues values = new ContentValues();
        values.put(KEY_PACKAGE, packageName); // App Name
        values.put(KEY_NAME, appName); // App Name
        values.put(KEY_CATEGORY, category);
        values.put(KEY_TYPE, type); // Leak type
        values.put(KEY_CONTENT, content);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        values.put(KEY_TIME_STAMP, dateFormat.format(new Date())); // Leak time stamp

        // Inserting Row
        mDB.insert(TABLE_DATA_LEAKS, null, values);
    }

    public Date getDateFromTimestamp(String timestamp) {
        Date date = null;
        try {
            date = mDateFormat.parse(timestamp);
        }
        catch (ParseException ex) {}

        return date;
    }

    private void addLeakSummary(LeakReport rpt) {
        ContentValues values = new ContentValues();
        values.put(KEY_PACKAGE, rpt.packageName);
        values.put(KEY_NAME, rpt.appName);
        values.put(KEY_CATEGORY, rpt.category.name());
        values.put(KEY_FREQUENCY, 0);
        values.put(KEY_IGNORE, 0);
        mDB.insert(TABLE_LEAK_SUMMARY, null, values);
    }


    public List<AppSummary> getAllApps() {
        List<AppSummary> apps = new ArrayList<>();
        Cursor cursor = mDB.query(TABLE_LEAK_SUMMARY, new String[]{KEY_PACKAGE, KEY_NAME, "SUM(" + KEY_FREQUENCY + ")", "MIN(" + KEY_IGNORE + ")"}, null, null, KEY_PACKAGE + ", " + KEY_NAME, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    AppSummary app = new AppSummary();
                    app.packageName = cursor.getString(0);
                    app.appName = cursor.getString(1);
                    app.totalLeaks = cursor.getInt(2);
                    app.ignore = cursor.getInt(3);
                    apps.add(app);
                } while (cursor.moveToNext());

            }
            cursor.close();
        }
        return apps;
    }

    public List<CategorySummary> getAppDetail(String packageName) {
        List<CategorySummary> categories = new ArrayList<>();
        Cursor cursor = mDB.query(TABLE_LEAK_SUMMARY, new String[]{KEY_ID, KEY_CATEGORY, KEY_FREQUENCY, KEY_IGNORE}, KEY_PACKAGE + "=?", new String[]{packageName}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    int notifyId = cursor.getInt(0);
                    String category = cursor.getString(1);
                    int count = cursor.getInt(2);
                    int ignore = cursor.getInt(3);
                    categories.add(new CategorySummary(notifyId, category, count, ignore));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return categories;
    }


    public List<DataLeak> getAppLeaks(String packageName, String category) {
        List<DataLeak> leakList = new ArrayList<DataLeak>();
        Cursor cursor = mDB.query(TABLE_DATA_LEAKS, new String[]{KEY_TYPE, KEY_CONTENT, KEY_TIME_STAMP}, KEY_PACKAGE + "=? AND " + KEY_CATEGORY + "=?", new String[]{packageName, category}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    DataLeak leak = new DataLeak();
                    leak.type = cursor.getString(0);
                    leak.leakContent = cursor.getString(1);
                    leak.timestamp = cursor.getString(2);
                    leakList.add(leak);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        // return contact list
        return leakList;
    }

    // w3kim@uwaterloo.ca : simple helper
    private boolean isHttpMethod(String s) {
        return s.equals("GET")
                || s.equals("POST")
                || s.equals("PUT")
                || s.equals("HEAD")
                || s.equals("CONNECT")
                || s.equals("DELETE")
                || s.equals("OPTIONS");
    }

    /**
     * [w3kim@uwaterloo.ca]
     * Parse an URL and Record in Naive Way
     *
     * @param appName app identifier
     * @param packageName app package name
     * @param request network request string
     * @return true iff there is a parsable URL present in request
     */
    public boolean addUrlIfAny(String appName, String packageName, String request) {
        if (request == null
                || request.isEmpty()
                || !isHttpMethod(request.trim().split("\n")[0].split(" ")[0])) {
            return false;
        }
        String statusLine = null;
        String hostLine = null;
        try {
            Scanner scanner = new Scanner(request);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (isHttpMethod(line.split(" ")[0])) {
                    statusLine = line;
                } else if (line.startsWith("Host")) {
                    hostLine = line;
                }
            }
            if (statusLine != null && hostLine != null) {
                Matcher matcher = Patterns.WEB_URL.matcher(hostLine);
                if (matcher.find()) {
                    String[] statusLineTokens = statusLine.split(" ");
                    String resWithParams = statusLineTokens[1];

                    int queryParamsStart = resWithParams.indexOf('?');
                    int cut = (queryParamsStart < 0) ? resWithParams.length() : queryParamsStart;
                    String res = resWithParams.substring(0, cut);
                    String queryParams = (queryParamsStart < 0) ? "" : resWithParams.substring(res.length() + 1);

                    String host = matcher.group();
                    addUrl(appName, packageName, host + res + '?' + queryParams, host, res, queryParams);
                    return true;
                }
            }
        } catch (Exception e) {
            // just in case
            Log.e(TAG, "Failed to parse an URL (appName=" + appName + ", packageName=" + packageName + "): " + e.getMessage());
        }

        return false;
    }

    public int findNotificationId(LeakReport rpt) {

        Cursor cursor = mDB.query(TABLE_LEAK_SUMMARY,
                new String[]{KEY_ID, KEY_FREQUENCY, KEY_IGNORE},
                KEY_PACKAGE + "=? AND " + KEY_CATEGORY + "=?",
                new String[]{rpt.packageName, rpt.category.name()}, null, null, null, null);

        if (cursor != null) {
            if (!cursor.moveToFirst()) { // this package(app) has no leak of this category previously
                addLeakSummary(rpt);
                cursor = mDB.query(TABLE_LEAK_SUMMARY,
                        new String[]{KEY_ID, KEY_FREQUENCY, KEY_IGNORE},
                        KEY_PACKAGE + "=? AND " + KEY_CATEGORY + "=?",
                        new String[]{rpt.packageName, rpt.category.name()}, null, null, null, null);
            }
            if (!cursor.moveToFirst()) {
                Logger.i("DBHandler", "fail to create summary table");
                cursor.close();
                return -1;
            }
            int notifyId = -1;
            int frequency = 0;
            int ignore = 0;

            notifyId = cursor.getInt(0);
            frequency = cursor.getInt(1);
            ignore = cursor.getInt(2);

            cursor.close();

            for (LeakInstance li : rpt.leaks) {
                addDataLeak(rpt.packageName, rpt.appName, rpt.category.name(), li.type, li.content);
            }
            //need to update frequency in summary table accordingly
            // Which row to update, based on the package and category
            ContentValues values = new ContentValues();
            values.put(KEY_FREQUENCY, frequency + rpt.leaks.size());

            String selection = KEY_ID + " =?";
            String[] selectionArgs = {String.valueOf(notifyId)};

            int count = mDB.update(
                    TABLE_LEAK_SUMMARY,
                    values,
                    selection,
                    selectionArgs);

            if (count == 0) {
                Logger.i("DBHandler", "fail to update summary table");
            }
            return ignore == 1 ? -1 : notifyId;
        }
        return -1;
    }


    public int findNotificationCounter(int id, String category) {
        Cursor cursor = mDB.query(TABLE_LEAK_SUMMARY,
                new String[]{KEY_ID, KEY_FREQUENCY},
                KEY_ID + "=? AND " + KEY_CATEGORY + "=?",
                new String[]{String.valueOf(id), category}, null, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int frequency = cursor.getInt(1);
                cursor.close();
                return frequency;
            }
            cursor.close();
        }

        return -1;
    }


    public void setIgnoreApp(String packageName, boolean ignore) {
        ContentValues values = new ContentValues();
        values.put(KEY_IGNORE, ignore ? 1 : 0);

        String selection = KEY_PACKAGE + " =?";
        String[] selectionArgs = {packageName};

        int count = mDB.update(
                TABLE_LEAK_SUMMARY,
                values,
                selection,
                selectionArgs);

        if (count == 0) {
            Logger.i("DBHandler", "fail to set ignore for " + packageName);
        }
    }

    public void setIgnoreAppCategory(int notifyId, boolean ignore) {
        ContentValues values = new ContentValues();
        values.put(KEY_IGNORE, ignore ? 1 : 0);

        String selection = KEY_ID + " =?";
        String[] selectionArgs = {String.valueOf(notifyId)};
        int count = mDB.update(
                TABLE_LEAK_SUMMARY,
                values,
                selection,
                selectionArgs);
        if (count == 0) {
            Logger.i("DBHandler", "fail to set ignore for " + notifyId);
        }
    }

}
