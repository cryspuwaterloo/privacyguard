package ca.uwaterloo.crysp.privacyguard.Application.Activities;

import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import ca.uwaterloo.crysp.privacyguard.R;
import ca.uwaterloo.crysp.privacyguard.Application.Logger;
import ca.uwaterloo.crysp.privacyguard.Application.Database.DatabaseHandler;
import ca.uwaterloo.crysp.privacyguard.Plugin.KeywordDetection;
import ca.uwaterloo.crysp.privacyguard.Utilities.FileChooser;
import ca.uwaterloo.crysp.privacyguard.Utilities.FileUtils;
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

            LinearLayout updateFilterKeywords = (LinearLayout) view.findViewById(R.id.update_filter_keywords);

            final MyPreferencesActivity activity = (MyPreferencesActivity) getActivity();

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
                Logger.d(TAG, "keywords have been updated");
            }
        }).showDialog();
    }
}


