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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.security.KeyChain;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.PrivacyGuard.Application.Database.AppSummary;
import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Application.Helpers.PreferenceHelper;
import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.Network.FakeVPN.MyVpnService;
import com.PrivacyGuard.Application.Network.FakeVPN.MyVpnService.MyVpnServiceBinder;
import com.PrivacyGuard.Application.PrivacyGuard;
import com.PrivacyGuard.Plugin.KeywordDetection;
import com.PrivacyGuard.Utilities.CertificateManager;
import com.PrivacyGuard.Utilities.FileChooser;
import com.PrivacyGuard.Utilities.FileUtils;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.security.cert.Certificate;
import javax.security.cert.CertificateEncodingException;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    private static final int REQUEST_VPN = 1;
    public static final int REQUEST_CERT = 2;

    private ListView listLeak;
    private MainListViewAdapter adapter;
    private DatabaseHandler mDbHandler; // [w3kim@uwaterloo.ca] : factored out as an instance var

    private View onIndicator;
    private View offIndicator;
    private View loadingIndicator;
    private FloatingActionButton vpnToggle;

    private boolean bounded = false;
    private boolean keyChainInstalled = false;
    ServiceConnection mSc;
    MyVpnService mVPN;

    //When the VPN has started running, remove the loading view so that the user can continue
    //interacting with the application.
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

        onIndicator = findViewById(R.id.on_indicator);
        offIndicator = findViewById(R.id.off_indicator);
        loadingIndicator = findViewById(R.id.loading_indicator);
        listLeak = (ListView)findViewById(R.id.leaksList);
        vpnToggle = (FloatingActionButton)findViewById(R.id.on_off_button);

        vpnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!MyVpnService.isRunning()) {
                    Logger.d(TAG, "Connect toggled ON");
                    if (!keyChainInstalled) {
                        installCertificate();
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

        mDbHandler = new DatabaseHandler(this);
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
        vpnToggle.setAlpha(status == Status.VPN_STARTING ? 0.3f : 1.0f);

        if (status == Status.VPN_STARTING) {
            loadingViewShownTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.settings:
                Intent i = new Intent(this, MyPreferencesActivity.class);
                startActivity(i);
                break;
        }
        return true;
    }

    public void populateLeakList() {
        // -----------------------------------------------------------------------
        // Database Fetch
        DatabaseHandler db = new DatabaseHandler(this);
        List<AppSummary> apps = db.getAllApps();
        db.close();

        if (apps == null) {
            return;
        }

        Comparator<AppSummary> comparator = PreferenceHelper.getAppLeakOrder(getApplicationContext());
        if (comparator != null) {
            Collections.sort(apps, comparator);
        }

        if (adapter == null) {
            adapter = new MainListViewAdapter(this, apps);
            listLeak.setAdapter(adapter);
            listLeak.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(MainActivity.this, AppSummaryActivity.class);

                    AppSummary app = (AppSummary) parent.getItemAtPosition(position);

                    intent.putExtra(PrivacyGuard.EXTRA_PACKAGE_NAME, app.packageName);
                    intent.putExtra(PrivacyGuard.EXTRA_APP_NAME, app.appName);
                    intent.putExtra(PrivacyGuard.EXTRA_IGNORE, app.ignore);

                    startActivity(intent);
                }
            });
        } else {
            adapter.updateData(apps);
        }
    }

    /**
     *
     */
    public void installCertificate() {
        boolean certInstalled = CertificateManager.isCACertificateInstalled(MyVpnService.CADir, MyVpnService.CAName, MyVpnService.KeyType, MyVpnService.Password.toCharArray());
        if (keyChainInstalled && certInstalled) {
            return;
        }
        if (!certInstalled) {
            CertificateManager.initiateFactory(MyVpnService.CADir, MyVpnService.CAName, MyVpnService.CertName, MyVpnService.KeyType, MyVpnService.Password.toCharArray());
        }
        Intent intent = KeyChain.createInstallIntent();
        try {
            Certificate cert = CertificateManager.getCACertificate(MyVpnService.CADir, MyVpnService.CAName);
            if (cert != null) {
                intent.putExtra(KeyChain.EXTRA_CERTIFICATE, cert.getEncoded());
                intent.putExtra(KeyChain.EXTRA_NAME, MyVpnService.CAName);
                startActivityForResult(intent, REQUEST_CERT);
            }
        } catch (CertificateEncodingException e) {
            Logger.e(TAG, "Certificate Encoding Error", e);
        }
    }

    /**
     * Gets called immediately before onResume() when activity is re-starting
     */
    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request == REQUEST_CERT) {
            keyChainInstalled = result == RESULT_OK;
            if (keyChainInstalled) {
                startVPN();
            }
        } else if (request == REQUEST_VPN) {
            if (result == RESULT_OK) {
                Logger.d(TAG, "Starting VPN service");

                showIndicator(Status.VPN_STARTING);
                mVPN.startVPN(this);
            }
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
            startActivityForResult(intent, REQUEST_VPN);
        } else {
            onActivityResult(REQUEST_VPN, RESULT_OK, null);
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
}