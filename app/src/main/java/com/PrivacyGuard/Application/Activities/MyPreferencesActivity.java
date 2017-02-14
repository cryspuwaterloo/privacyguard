package com.PrivacyGuard.Application.Activities;

import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Plugin.KeywordDetection;
import com.PrivacyGuard.Utilities.FileChooser;
import com.PrivacyGuard.Utilities.FileUtils;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;

/**
 * Created by lucas on 05/02/17.
 */

public class MyPreferencesActivity extends AppCompatActivity {
    private static String TAG = "MyPreferencesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.preferences_fragment, null);

            LinearLayout updateFilterKeywords = (LinearLayout)view.findViewById(R.id.update_filter_keywords);
            LinearLayout exportData = (LinearLayout)view.findViewById(R.id.export_data);

            final MyPreferencesActivity activity = (MyPreferencesActivity)getActivity();

            updateFilterKeywords.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.update_filter_keywords_title)
                            .setMessage(R.string.update_filter_keywords_message)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.updateFilterKeywords();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            });

            exportData.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.export_data_title)
                            .setMessage(R.string.export_data_message)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.exportData();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            });

            return view;
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    /**
     * [w3kim@uwaterloo.ca]
     * Update Filtering Keywords
     */
    public void updateFilterKeywords() {
        new FileChooser(this).setFileListener(new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(final File file) {
                // this is the path where the chosen file gets copied to
                String path = String.format("%s/%s",
                        getFilesDir().getAbsolutePath(), KeywordDetection.KEYWORDS_FILE_NAME);

                // check if there is an existing file
                File keywords = new File(path);
                if (keywords.exists()) {
                    keywords.delete();
                }

                // copy the file to the path
                FileUtils.copyFile(file, keywords.getAbsolutePath());
                // notify the plugin the file has been updated
                KeywordDetection.invalidate();
            }
        }).showDialog();
    }

    /**
     * [w3kim@uwaterloo.ca]
     * Export DB contents to CSV files
     */
    public void exportData() {
        DatabaseHandler mDbHandler = new DatabaseHandler(this);

        File exportDir = new File(Environment.getExternalStorageDirectory(), "privacyguard");
        if (!exportDir.exists()) {
            if (!exportDir.mkdirs()) {
                Log.e(TAG, "cannot create directories: " + exportDir.getAbsolutePath());
            }
        }

        long timestamp = System.currentTimeMillis();
        for (String table : mDbHandler.getTables()) {
            File file = new File(exportDir,
                    String.format("pg-export-%s-%s.csv", timestamp, table));
            try {
                file.createNewFile();
                CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
                SQLiteDatabase db = mDbHandler.getReadableDatabase();
                Cursor curCSV = db.rawQuery("SELECT * FROM " + table, null);
                csvWrite.writeNext(curCSV.getColumnNames());
                while (curCSV.moveToNext()) {
                    //Which column you want to exprort
                    int numColumns = curCSV.getColumnCount();
                    String[] arrStr = new String[numColumns];
                    for (int i = 0; i < numColumns; i++) {
                        arrStr[i] = curCSV.getString(i);
                    }
                    csvWrite.writeNext(arrStr);
                }
                csvWrite.close();
                curCSV.close();

                Log.d(TAG, String.format("table '%s' has been exported to '%s'", table, file.getAbsolutePath()));
            } catch (Exception sqlEx) {
                Log.e(TAG, sqlEx.getMessage(), sqlEx);
            }
        }
    }
}
