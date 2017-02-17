package com.PrivacyGuard.Application.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

/**
 * Created by lucas on 16/02/17.
 */

public class AppDataActivity extends AppCompatActivity {

    public static final String APP_NAME_INTENT = "APP_NAME";
    public static final String APP_PACKAGE_INTENT = "PACKAGE_NAME";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_app_data);

        Intent i = getIntent();
        String appName = i.getStringExtra(APP_NAME_INTENT);
        String packageName = i.getStringExtra(APP_PACKAGE_INTENT);

        Toast.makeText(this, appName + packageName, Toast.LENGTH_LONG).show();
    }
}
