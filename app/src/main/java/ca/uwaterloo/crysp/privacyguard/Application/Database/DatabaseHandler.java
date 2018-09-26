package ca.uwaterloo.crysp.privacyguard.Application.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Patterns;

import ca.uwaterloo.crysp.privacyguard.Application.Logger;
import ca.uwaterloo.crysp.privacyguard.Plugin.LeakInstance;
import ca.uwaterloo.crysp.privacyguard.Plugin.LeakReport;

import ca.uwaterloo.crysp.privacyguard.Plugin.TrafficReport;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

/**
 * Created by MAK on 03/11/2015.
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "dataLeaksManager";
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.CANADA);
    private SQLiteDatabase mDB;
    public static final String LEAK_ID_KEY = "leak_id_key";
    private static final boolean DEBUG = false;

    private static DatabaseHandler sInstance = null;

    private Context applicationContext = null;

    //The singleton pattern is used here because multiple threads could potentially be writing to the database.
    //Because of this pattern, only one DataBaseHandler is created for the application lifecycle.
    //As a result, do not call .close() on a DatabaseHandler instance.
    public static synchronized DatabaseHandler getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DatabaseHandler(context.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        applicationContext = context;
        mDB = getReadableDatabase();
    }

    public String[] getTables() {
        return new String[]{TABLE_DATA_LEAKS, TABLE_LEAK_SUMMARY, TABLE_URL, TABLE_APP_STATUS_EVENTS, CREATE_TRAFFIC_TABLE};
    }

    public static SimpleDateFormat getDateFormat() {
        return DATE_FORMAT;
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        //create table data_leaks
        db.execSQL(CREATE_DATA_LEAKS_TABLE);
        db.execSQL(CREATE_LEAK_SUMMARY_TABLE);
        db.execSQL(CREATE_APP_STATUS_TABLE);
        // w3kim@uwaterloo.ca
        db.execSQL(CREATE_URL_TABLE);
        db.execSQL(CREATE_TRAFFIC_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA_LEAKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEAK_SUMMARY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_APP_STATUS_EVENTS);
        // w3kim@uwaterloo.ca
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_URL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRAFFIC_SUMMARY);
        // Create tables again
        onCreate(db);
    }

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
    private static final String KEY_HOSTNAME = "host_name";

    public static final int FOREGROUND_STATUS = 1;
    public static final int BACKGROUND_STATUS = 0;
    public static final int UNSPECIFIED_STATUS = -1;

    //Note: foreground status is either 1 for foreground, 0 for background, or -1 if not specified.
    private static final String KEY_FOREGROUND_STATUS = "foreground_status";

    // App status events table name
    private static final String TABLE_APP_STATUS_EVENTS = "app_status_events";
    // App status events Table Columns names
    private static final String CREATE_APP_STATUS_TABLE = "CREATE TABLE " + TABLE_APP_STATUS_EVENTS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_PACKAGE + " TEXT,"
            + KEY_TIME_STAMP + " INTEGER,"
            + KEY_FOREGROUND_STATUS + " INTEGER,"
            + KEY_HOSTNAME + " TEXT)";

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
            + KEY_TIME_STAMP + " TEXT,"
            + KEY_FOREGROUND_STATUS + " INTEGER,"
            + KEY_HOSTNAME + " TEXT)";
    private static final String CREATE_LEAK_SUMMARY_TABLE = "CREATE TABLE " + TABLE_LEAK_SUMMARY + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_PACKAGE + " TEXT,"
            + KEY_NAME + " TEXT,"
            + KEY_CATEGORY + " TEXT,"
            + KEY_FREQUENCY + " INTEGER,"
            + KEY_IGNORE + " INTEGER)";

    ////////////////////////////////////////////
    private static final String TABLE_TRAFFIC_SUMMARY = "traffic_summary";
    private static final String KEY_TRAFFIC_ID = "_id";
    private static final String KEY_TRAFFIC_APP_NAME = "app_name";
    private static final String KEY_TRAFFIC_DEST_ADDR = "dest_addr";
    private static final String KEY_TRAFFIC_ENCRYPTION = "encryption";
    private static final String KEY_TRAFFIC_SIZE = "data_size";
    private static final String KEY_TRAFFIC_DIRECTION_OUT = "direction_out";

    private static final String CREATE_TRAFFIC_TABLE = "CREATE TABLE " + TABLE_TRAFFIC_SUMMARY + "("
            + KEY_TRAFFIC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_TRAFFIC_APP_NAME + " TEXT,"
            + KEY_TRAFFIC_DEST_ADDR + " TEXT,"
            + KEY_TRAFFIC_ENCRYPTION + " INTEGER,"
            + KEY_TRAFFIC_SIZE + " INTEGER,"
            + KEY_TRAFFIC_DIRECTION_OUT + " INTEGER )";

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    public void addtraffic(TrafficReport traffic){
        int encryption;
        if (traffic.metaData.encrypted){
            encryption = 1;
        } else{
            encryption = 0;
        }
        int outgoing= 0;
        String destIP = traffic.metaData.srcIP;
        if(traffic.metaData.outgoing){
            outgoing = 1;
            destIP = traffic.metaData.destIP;
        }
        Cursor cursor = mDB.query(TABLE_TRAFFIC_SUMMARY,
                new String[]{KEY_TRAFFIC_ID, KEY_TRAFFIC_SIZE},
                KEY_TRAFFIC_APP_NAME + "=? AND " + KEY_TRAFFIC_DEST_ADDR + "=? AND "
                        + KEY_TRAFFIC_ENCRYPTION + "=? AND " + KEY_TRAFFIC_DIRECTION_OUT + "=?",
                new String[]{traffic.metaData.appName, destIP, Integer.toString(encryption), Integer.toString(outgoing)}, null, null, null, null);

        if (cursor != null) {
            if (!cursor.moveToFirst()) { // this package(app) has no leak of this category previously
                ContentValues values = new ContentValues();
                values.put(KEY_TRAFFIC_APP_NAME, traffic.metaData.appName);
                values.put(KEY_TRAFFIC_DEST_ADDR, destIP);
                values.put(KEY_TRAFFIC_ENCRYPTION, encryption);
                values.put(KEY_TRAFFIC_SIZE, traffic.size);
                values.put(KEY_TRAFFIC_DIRECTION_OUT, outgoing);
                mDB.insert(TABLE_TRAFFIC_SUMMARY, null, values);
                cursor = mDB.query(TABLE_TRAFFIC_SUMMARY,
                        new String[]{KEY_TRAFFIC_ID, KEY_TRAFFIC_SIZE},
                        KEY_TRAFFIC_APP_NAME + "=? AND " + KEY_TRAFFIC_DEST_ADDR + "=? AND "
                                + KEY_TRAFFIC_ENCRYPTION + "=? AND " + KEY_TRAFFIC_DIRECTION_OUT + "=?",
                        new String[]{traffic.metaData.appName, destIP, Integer.toString(encryption)}, null, null, null, null);
            }
            if (!cursor.moveToFirst()) {
                if (DEBUG) Logger.i("DatabaseHandler", "fail to create summary table");
                cursor.close();
                return;
            }

            int Id = cursor.getInt(0);
            int size = cursor.getInt(1);

            cursor.close();

            // Need to update frequency in summary table accordingly
            // Which row to update, based on the package and category
            ContentValues values = new ContentValues();
            values.put(KEY_TRAFFIC_SIZE, size + traffic.size);

            String selection = KEY_TRAFFIC_ID + " =?";
            String[] selectionArgs = {String.valueOf(Id)};

            int count = mDB.update(
                    TABLE_TRAFFIC_SUMMARY,
                    values,
                    selection,
                    selectionArgs);

            if (DEBUG) Logger.d(TAG, "Testing: update appname: "+ traffic.metaData.appName + " to: " + destIP + " : " + (size + traffic.size)) ;

            if (count == 0) {
                if (DEBUG) Logger.i("DatabaseHandler", "fail to update summary table");
            }
        }
    }

    public List<Traffic> getTraffics(String appName, boolean encrypted, boolean outgoing) {
        List<Traffic> traffics = new ArrayList<>();
        Cursor cursor = mDB.query(TABLE_TRAFFIC_SUMMARY, new String[]{KEY_TRAFFIC_DEST_ADDR, KEY_TRAFFIC_SIZE},
                KEY_TRAFFIC_APP_NAME + "=? AND " + KEY_TRAFFIC_ENCRYPTION + "=? AND " + KEY_TRAFFIC_DIRECTION_OUT + "=? ",
                new String[]{appName, encrypted ? "1" : "0", outgoing ? "1" : "0"}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Traffic traffic = new Traffic(appName, cursor.getString(0), encrypted, cursor.getInt(1), outgoing) ;
                    traffics.add(traffic);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        // return contact list
        return traffics;
    }

    public List<Traffic> getTraffics(boolean encrypted, boolean outgoing) {
        List<Traffic> traffics = new ArrayList<>();
        Cursor cursor = mDB.query(TABLE_TRAFFIC_SUMMARY, new String[]{KEY_TRAFFIC_APP_NAME, KEY_TRAFFIC_DEST_ADDR, KEY_TRAFFIC_SIZE},
                KEY_TRAFFIC_ENCRYPTION + "=? AND " + KEY_TRAFFIC_DIRECTION_OUT + "=? ",
                new String[]{encrypted ? "1" : "0", outgoing ? "1" : "0"}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Traffic traffic = new Traffic(cursor.getString(0), cursor.getString(1), encrypted, cursor.getInt(2), outgoing);
                    traffics.add(traffic);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        // return contact list
        return traffics;
    }

    // w3kim@uwaterloo.ca
    private void addUrl(String appName, String packageName, String url, String host, String res, String queryParams) {
        ContentValues values = new ContentValues();
        values.put(KEY_URL_PACKAGE, packageName);
        values.put(KEY_URL_APP_NAME, appName);
        values.put(KEY_URL_TIMESTAMP, DATE_FORMAT.format(new Date()));
        values.put(KEY_URL_HOST, host);
        values.put(KEY_URL_RES, res);
        values.put(KEY_URL_QUERY_PARAMS, queryParams);
        values.put(KEY_URL_URL, url);

        mDB.insert(TABLE_URL, null, values);
    }

    public void deletePackage(String packageName) {
        mDB.delete(TABLE_DATA_LEAKS, KEY_PACKAGE + "=?", new String[] {packageName});
        mDB.delete(TABLE_LEAK_SUMMARY, KEY_PACKAGE + "=?", new String[] {packageName});
        mDB.delete(TABLE_APP_STATUS_EVENTS, KEY_PACKAGE + "=?", new String[] {packageName});
        mDB.delete(TABLE_URL, KEY_URL_PACKAGE + "=?", new String[] {packageName});
    }

    // Adding new data leak
    private void addDataLeak(String packageName, String appName, String category, String type, String content, String hostName) {
        ContentValues values = new ContentValues();
        values.put(KEY_PACKAGE, packageName); // App Name
        values.put(KEY_NAME, appName); // App Name
        values.put(KEY_CATEGORY, category);
        values.put(KEY_TYPE, type); // Leak type
        values.put(KEY_CONTENT, content);
        values.put(KEY_TIME_STAMP, DATE_FORMAT.format(new Date())); // Leak time stamp
        values.put(KEY_FOREGROUND_STATUS, UNSPECIFIED_STATUS); // Leak foreground status
        values.put(KEY_HOSTNAME, hostName);

        // Inserting Row
        final long id = mDB.insert(TABLE_DATA_LEAKS, null, values);

        // A bug occurred when the update task ran so quickly after the app left the foreground
        // that the most recent status event was not yet available in the api. Hence, the leak was
        // incorrectly classified as foreground. To fix this, run the update task after 10 seconds
        // to ensure that the most recent status event is available.
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                new UpdateLeakForegroundStatus(applicationContext).execute(id);
            }
        }, TimeUnit.SECONDS.toMillis(10));
    }

    public void addAppStatusEvent(String packageName, long timeStamp, int foreground) {
        if (foreground != 0 && foreground != 1) throw new RuntimeException("Must be 0 or 1");

        ContentValues values = new ContentValues();
        values.put(KEY_PACKAGE, packageName);
        values.put(KEY_TIME_STAMP, timeStamp);
        values.put(KEY_FOREGROUND_STATUS, foreground);

        // Inserting Row
        mDB.insert(TABLE_APP_STATUS_EVENTS, null, values);
    }

    private void addLeakSummary(LeakReport rpt) {
        ContentValues values = new ContentValues();
        values.put(KEY_PACKAGE, rpt.metaData.packageName);
        values.put(KEY_NAME, rpt.metaData.appName);
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
                    AppSummary app = new AppSummary(cursor.getString(0), cursor.getString(1), cursor.getInt(2), cursor.getInt(3));
                    apps.add(app);
                } while (cursor.moveToNext());

            }
            cursor.close();
        }
        return apps;
    }

    public List<AppStatusEvent> getAppStatusEvents() {
        List<AppStatusEvent> appStatusEvents = new ArrayList<>();
        Cursor cursor = mDB.query(TABLE_APP_STATUS_EVENTS, new String[]{KEY_PACKAGE, KEY_TIME_STAMP, KEY_FOREGROUND_STATUS}, null, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    AppStatusEvent appStatusEvent = new AppStatusEvent(cursor.getString(0), cursor.getLong(1), cursor.getInt(2));
                    appStatusEvents.add(appStatusEvent);
                } while (cursor.moveToNext());

            }
            cursor.close();
        }
        return appStatusEvents;
    }

    public List<AppStatusEvent> getAppStatusEvents(String packageName) {
        List<AppStatusEvent> appStatusEvents = new ArrayList<>();
        Cursor cursor = mDB.query(TABLE_APP_STATUS_EVENTS, new String[]{KEY_PACKAGE, KEY_TIME_STAMP, KEY_FOREGROUND_STATUS}, KEY_PACKAGE + "=?", new String[]{packageName}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    AppStatusEvent appStatusEvent = new AppStatusEvent(cursor.getString(0), cursor.getLong(1), cursor.getInt(2));
                    appStatusEvents.add(appStatusEvent);
                } while (cursor.moveToNext());

            }
            cursor.close();
        }
        return appStatusEvents;
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

    public DataLeak getLeakById(long id) {
        Cursor cursor = mDB.query(TABLE_DATA_LEAKS, new String[]{KEY_PACKAGE, KEY_NAME, KEY_CATEGORY, KEY_TYPE, KEY_CONTENT, KEY_TIME_STAMP, KEY_FOREGROUND_STATUS, KEY_HOSTNAME}, KEY_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                DataLeak leak = new DataLeak(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getInt(6), cursor.getString(7));
                cursor.close();
                return leak;
            }
            cursor.close();
        }
        return null;
    }


    public List<DataLeak> getAppLeaks(String packageName, String category) {
        List<DataLeak> leakList = new ArrayList<>();
        Cursor cursor = mDB.query(TABLE_DATA_LEAKS, new String[]{KEY_PACKAGE, KEY_NAME, KEY_TYPE, KEY_CONTENT, KEY_TIME_STAMP, KEY_FOREGROUND_STATUS, KEY_HOSTNAME}, KEY_PACKAGE + "=? AND " + KEY_CATEGORY + "=?", new String[]{packageName, category}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    DataLeak leak = new DataLeak(cursor.getString(0), cursor.getString(1), category, cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getInt(5), cursor.getString(6));
                    leakList.add(leak);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        // return contact list
        return leakList;
    }

    public List<DataLeak> getAppLeaks(String category) {
        List<DataLeak> leakList = new ArrayList<>();
        Cursor cursor = mDB.query(TABLE_DATA_LEAKS, new String[]{KEY_PACKAGE, KEY_NAME, KEY_TYPE, KEY_CONTENT, KEY_TIME_STAMP, KEY_FOREGROUND_STATUS, KEY_HOSTNAME}, KEY_CATEGORY + "=?", new String[]{category}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    DataLeak leak = new DataLeak(cursor.getString(0), cursor.getString(1), category, cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getInt(5), cursor.getString(6));
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
            Logger.e(TAG, "Failed to parse an URL (appName=" + appName + ", packageName=" + packageName + "): " + e.getMessage());
        }

        return false;
    }

    public int findNotificationId(LeakReport rpt) {
        Cursor cursor = mDB.query(TABLE_LEAK_SUMMARY,
                new String[]{KEY_ID, KEY_FREQUENCY, KEY_IGNORE},
                KEY_PACKAGE + "=? AND " + KEY_CATEGORY + "=?",
                new String[]{rpt.metaData.packageName, rpt.category.name()}, null, null, null, null);

        if (cursor != null) {
            if (!cursor.moveToFirst()) { // this package(app) has no leak of this category previously
                addLeakSummary(rpt);
                cursor = mDB.query(TABLE_LEAK_SUMMARY,
                        new String[]{KEY_ID, KEY_FREQUENCY, KEY_IGNORE},
                        KEY_PACKAGE + "=? AND " + KEY_CATEGORY + "=?",
                        new String[]{rpt.metaData.packageName, rpt.category.name()}, null, null, null, null);
            }
            if (!cursor.moveToFirst()) {
                if (DEBUG) Logger.i("DatabaseHandler", "fail to create summary table");
                cursor.close();
                return -1;
            }

            int notifyId = cursor.getInt(0);
            int frequency = cursor.getInt(1);
            int ignore = cursor.getInt(2);

            cursor.close();

            for (LeakInstance li : rpt.leaks) {
                addDataLeak(rpt.metaData.packageName, rpt.metaData.appName, rpt.category.name(), li.type, li.content, rpt.metaData.destHostName);
            }
            // Need to update frequency in summary table accordingly
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
                Logger.i("DatabaseHandler", "fail to update summary table");
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

    public void setDataLeakStatus(long id, int status) {
        if (status != BACKGROUND_STATUS && status != FOREGROUND_STATUS) throw new RuntimeException("Invalid status value.");
        ContentValues values = new ContentValues();
        values.put(KEY_FOREGROUND_STATUS, status);

        String selection = KEY_ID + " =?";
        String[] selectionArgs = {String.valueOf(id)};

        int count = mDB.update(
                TABLE_DATA_LEAKS,
                values,
                selection,
                selectionArgs);

        if (count == 0) {
            if (DEBUG) Logger.i("DatabaseHandler", "fail to set status for id: " + id);
        }
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
            if (DEBUG) Logger.i("DatabaseHandler", "fail to set ignore for " + packageName);
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
            if (DEBUG) Logger.i("DatabaseHandler", "fail to set ignore for " + notifyId);
        }
    }

}
