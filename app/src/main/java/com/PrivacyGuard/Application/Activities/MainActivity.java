/*
 * Main activity
 * Copyright (C) 2014  Yihang Song

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.PrivacyGuard.Application.Activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.security.KeyChain;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.PrivacyGuard.Application.Database.AppSummary;
import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Application.Helpers.ActivityRequestCodes;
import com.PrivacyGuard.Application.Helpers.PermissionsHelper;
import com.PrivacyGuard.Application.Helpers.PreferenceHelper;
import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.Network.FakeVPN.MyVpnService;
import com.PrivacyGuard.Application.Network.FakeVPN.MyVpnService.MyVpnServiceBinder;
import com.PrivacyGuard.Application.PrivacyGuard;
import com.PrivacyGuard.Utilities.CertificateManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.security.cert.Certificate;
import javax.security.cert.CertificateEncodingException;

@TargetApi(22)
public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    private static float DISABLED_ALPHA = 0.3f;

    private ListView listLeak;
    private MainListViewAdapter adapter;

    private View permissionDisabledView;
    private View applicationPermissionDisabledView;
    private View usageStatsPermissionDisabledView;

    private View mainLayout;
    private View onIndicator;
    private View offIndicator;
    private View loadingIndicator;
    private FloatingActionButton vpnToggle;
    private FloatingActionButton statsButton;

    private boolean bounded = false;
    //private boolean keyChainInstalled = false;
    ServiceConnection mSc;
    MyVpnService mVPN;

    // When the VPN has started running, remove the loading view so that the user can continue
    // interacting with the application.
    private class ReceiveMessages extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            long difference = System.currentTimeMillis() - loadingViewShownTime;

            //The loading view should show for a minimum of 2 seconds to prevent the loading view
            //from appearing and disappearing rapidly.
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showIndicator(Status.VPN_ON);
                }
            }, Math.max(2000 - difference, 0));
        }
    }

    private ReceiveMessages myReceiver = null;
    private boolean myReceiverIsRegistered = false;
    private long loadingViewShownTime = 0;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        myReceiver = new ReceiveMessages();

        permissionDisabledView = findViewById(R.id.permission_disabled_message);
        applicationPermissionDisabledView = findViewById(R.id.application_settings_view);
        usageStatsPermissionDisabledView = findViewById(R.id.usage_status_settings_view);

        mainLayout = findViewById(R.id.main_layout);
        onIndicator = findViewById(R.id.on_indicator);
        offIndicator = findViewById(R.id.off_indicator);
        loadingIndicator = findViewById(R.id.loading_indicator);
        listLeak = (ListView)findViewById(R.id.leaksList);
        vpnToggle = (FloatingActionButton)findViewById(R.id.on_off_button);

        CertificateManager.initiateFactory(MyVpnService.CADir, MyVpnService.CAName, MyVpnService.CertName, MyVpnService.KeyType, MyVpnService.Password.toCharArray());

        vpnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!MyVpnService.isRunning()) {
                    Logger.d(TAG, "Connect toggled ON");
                    if (!CertificateManager.fakeRootCAIsTrusted()) {
                        Intent intent = CertificateManager.trustfakeRootCA();
                        if (intent != null) startActivityForResult(intent, ActivityRequestCodes.REQUEST_CERT);
                    } else {
                        startVPN();
                    }
                } else {
                    Logger.d(TAG, "Connect toggled OFF");
                    showIndicator(Status.VPN_OFF);
                    stopVPN();
                }
            }
        });

        statsButton = (FloatingActionButton)findViewById(R.id.stats_button);
        statsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), AllAppsDataActivity.class);
                startActivity(i);
            }
        });

        /** use bound service here because stopservice() doesn't immediately trigger onDestroy of VPN service */
        mSc = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Logger.d(TAG, "VPN Service connected");
                mVPN = ((MyVpnServiceBinder) service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Logger.d(TAG, "VPN Service disconnected");
            }
        };

        Button permissionButton = (Button)findViewById(R.id.turn_on_permission_button);
        permissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, ActivityRequestCodes.PERMISSIONS_SETTINGS);
            }
        });

        Button usageStatsButton = (Button)findViewById(R.id.turn_on_usage_stats_button);
        usageStatsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), ActivityRequestCodes.USAGE_STATS_PERMISSION_REQUEST);
            }
        });

        // Only enable the app if all required permissions are granted.
        // Otherwise, prompt the user to grant the permissions.
        checkPermissionsAndRequestAndEnableViews();
    }

    /**
     * Check for and request permissions while updating view visibility accordingly.
     */
    private void checkPermissionsAndRequestAndEnableViews() {
        if (checkPermissionsAndRequest()) {
            if ((!PermissionsHelper.validBuildVersionForAppUsageAccess() || PermissionsHelper.hasUsageAccessPermission(getApplicationContext()))) {
                mainLayout.setVisibility(View.VISIBLE);
                permissionDisabledView.setVisibility(View.GONE);
            }
            else {
                mainLayout.setVisibility(View.GONE);
                permissionDisabledView.setVisibility(View.VISIBLE);
                applicationPermissionDisabledView.setVisibility(View.GONE);
                usageStatsPermissionDisabledView.setVisibility(View.VISIBLE);
            }
        }
        else {
            mainLayout.setVisibility(View.GONE);
            permissionDisabledView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!bounded) {
            Intent service = new Intent(this, MyVpnService.class);
            this.bindService(service, mSc, Context.BIND_AUTO_CREATE);
            bounded = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        populateLeakList();

        if (!myReceiverIsRegistered) {
            registerReceiver(myReceiver, new IntentFilter(getString(R.string.vpn_running_broadcast_intent)));
            myReceiverIsRegistered = true;
        }

        if (MyVpnService.isStarted()) {
            //If the VPN was started before the user closed the app and still is not running, show
            //the loading indicator once again.
            showIndicator(Status.VPN_STARTING);
        } else if (MyVpnService.isRunning()) {
            showIndicator(Status.VPN_ON);
        } else {
            showIndicator(Status.VPN_OFF);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (myReceiverIsRegistered) {
            unregisterReceiver(myReceiver);
            myReceiverIsRegistered = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bounded) {//must unbind the service otherwise the ServiceConnection will be leaked.
            this.unbindService(mSc);
            bounded = false;
        }
    }

    private enum Status {
        VPN_ON,
        VPN_OFF,
        VPN_STARTING
    }

    private void showIndicator(Status status) {
        onIndicator.setVisibility(status == Status.VPN_ON ? View.VISIBLE : View.GONE);
        offIndicator.setVisibility(status == Status.VPN_OFF ? View.VISIBLE : View.GONE);
        loadingIndicator.setVisibility(status == Status.VPN_STARTING ? View.VISIBLE : View.GONE);

        vpnToggle.setEnabled(status != Status.VPN_STARTING);
        vpnToggle.setAlpha(status == Status.VPN_STARTING ? DISABLED_ALPHA : 1.0f);

        if (status == Status.VPN_STARTING) {
            loadingViewShownTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.settings:
                Intent i = new Intent(this, MyPreferencesActivity.class);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void populateLeakList() {
        DatabaseHandler db = DatabaseHandler.getInstance(this);
        List<AppSummary> apps = db.getAllApps();

        if (apps.isEmpty()) {
            statsButton.setEnabled(false);
            statsButton.setAlpha(DISABLED_ALPHA);
        }
        else {
            statsButton.setEnabled(true);
            statsButton.setAlpha(1.0f);
        }

        Comparator<AppSummary> comparator = PreferenceHelper.getAppLeakOrder(getApplicationContext());
        Collections.sort(apps, comparator);

        if (adapter == null) {
            adapter = new MainListViewAdapter(this, apps);
            listLeak.setAdapter(adapter);
            listLeak.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(MainActivity.this, AppSummaryActivity.class);

                    AppSummary app = (AppSummary)parent.getItemAtPosition(position);

                    intent.putExtra(PrivacyGuard.EXTRA_PACKAGE_NAME, app.getPackageName());
                    intent.putExtra(PrivacyGuard.EXTRA_APP_NAME, app.getAppName());
                    intent.putExtra(PrivacyGuard.EXTRA_IGNORE, app.getIgnore());

                    startActivity(intent);
                }
            });

            listLeak.setLongClickable(true);
            listLeak.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {
                    final AppSummary app = (AppSummary)parent.getItemAtPosition(position);
                    PackageManager pm = getPackageManager();
                    Drawable appIcon;
                    try {
                        appIcon = pm.getApplicationIcon(app.getPackageName());
                    } catch (PackageManager.NameNotFoundException e) {
                        appIcon = getResources().getDrawable(R.drawable.default_icon);
                    }

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.delete_package_title)
                            .setMessage(String.format(getResources().getString(R.string.delete_package_message), app.getAppName()))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    DatabaseHandler databaseHandler = DatabaseHandler.getInstance(MainActivity.this);
                                    databaseHandler.deletePackage(app.getPackageName());
                                    populateLeakList();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(appIcon)
                            .show();
                    return true;
                }
            });
        } else {
            adapter.updateData(apps);
        }
    }

    /**
     * Gets called immediately before onResume() when activity is re-starting
     */
    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request == ActivityRequestCodes.REQUEST_CERT) {
            boolean keyChainInstalled = result == RESULT_OK;
            if (keyChainInstalled) {
                startVPN();
            }
        } else if (request == ActivityRequestCodes.REQUEST_VPN) {
            if (result == RESULT_OK) {
                Logger.d(TAG, "Starting VPN service");

                showIndicator(Status.VPN_STARTING);
                mVPN.startVPN(this);
            }
        } else if (request == ActivityRequestCodes.PERMISSIONS_SETTINGS) {
            // After giving the user the opportunity to manually turn on
            // the required permissions, check whether they have been granted.
            checkPermissionsAndRequestAndEnableViews();
        } else if (request == ActivityRequestCodes.USAGE_STATS_PERMISSION_REQUEST) {
            // After giving the user the opportunity to manually turn on
            // the usage stats permission, check whether it has been granted.
            checkPermissionsAndRequestAndEnableViews();
        }
    }

    private void startVPN() {
        if (!bounded) {
            Intent service = new Intent(this, MyVpnService.class);
            this.bindService(service, mSc, Context.BIND_AUTO_CREATE);
            bounded = true;
        }
        /**
         * prepare() sometimes would misbehave:
         * https://code.google.com/p/android/issues/detail?id=80074
         *
         * if this affects our app, we can let vpnservice update main activity for status
         * http://stackoverflow.com/questions/4111398/notify-activity-from-service
         *
         */
        Intent intent = VpnService.prepare(this);
        Logger.d(TAG, "VPN prepare done");
        if (intent != null) {
            startActivityForResult(intent, ActivityRequestCodes.REQUEST_VPN);
        } else {
            onActivityResult(ActivityRequestCodes.REQUEST_VPN, RESULT_OK, null);
        }
    }

    private void stopVPN() {
        Logger.d(TAG, "Stopping VPN service");
        if (bounded) {
            this.unbindService(mSc);
            bounded = false;
        }
        mVPN.stopVPN();
    }

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 3;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 4;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COURSE_LOCATION = 5;

    /**
     * Requests permissions if they are not granted.
     * @return Whether the app has all the required permissions.
     */
    private boolean checkPermissionsAndRequest() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COURSE_LOCATION);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        String permission = null;

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                permission = Manifest.permission.READ_CONTACTS;
                break;
            }
            case MY_PERMISSIONS_REQUEST_READ_PHONE_STATE: {
                permission = Manifest.permission.READ_PHONE_STATE;
                break;
            }
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                permission = Manifest.permission.READ_EXTERNAL_STORAGE;
                break;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                break;
            }
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                permission = Manifest.permission.ACCESS_FINE_LOCATION;
                break;
            }
            case MY_PERMISSIONS_REQUEST_ACCESS_COURSE_LOCATION: {
                permission = Manifest.permission.ACCESS_COARSE_LOCATION;
                break;
            }
        }

        if (permission == null) throw new RuntimeException("Should not be null.");

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // If an individual permission was granted, check once again.
            checkPermissionsAndRequestAndEnableViews();
        } else {
            // If an individual permission was not granted.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                // If the user did not select "never ask again", check once again.
                checkPermissionsAndRequest();
            } else {
                // In this case, the user has selected "never ask again" and declined a permission.
                // Since we require all permissions to be granted, and can no longer ask for this
                // permission, give the user access to the permissions screen to turn on all the
                // permissions manually.
                mainLayout.setVisibility(View.GONE);
                permissionDisabledView.setVisibility(View.VISIBLE);
                applicationPermissionDisabledView.setVisibility(View.VISIBLE);
                boolean usageStats = (!PermissionsHelper.validBuildVersionForAppUsageAccess() || PermissionsHelper.hasUsageAccessPermission(getApplicationContext()));
                usageStatsPermissionDisabledView.setVisibility(usageStats ? View.GONE : View.VISIBLE);
            }
        }
    }
}