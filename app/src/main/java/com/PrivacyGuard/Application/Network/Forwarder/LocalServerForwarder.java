/*
 * Modify the SocketForwarder of SandroproxyLib
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

package com.PrivacyGuard.Application.Network.Forwarder;


import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.Network.FakeVPN.MyVpnService;
import com.PrivacyGuard.Application.PrivacyGuard;
import com.PrivacyGuard.Plugin.IPlugin;
import com.PrivacyGuard.Plugin.LeakReport;
import com.PrivacyGuard.Utilities.ByteArray;
import com.PrivacyGuard.Utilities.ByteArrayPool;

import org.sandrop.webscarab.model.ConnectionDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;


public class LocalServerForwarder extends Thread {

    private static final String TIME_STAMP_FORMAT = "MM-dd HH:mm:ss.SSS";
    private static final String UNKNOWN = "unknown";
    private static String TAG = LocalServerForwarder.class.getSimpleName();
    private static boolean EVALUATE = false;
    private static boolean DEBUG = true;
    private static boolean PROTECT = true;
    private static int LIMIT = 1368;
    private static ByteArrayPool byteArrayPool = new ByteArrayPool(10, LIMIT);
    private boolean outgoing = false;
    private ArrayList<IPlugin> plugins;
    private MyVpnService vpnService;
    private Socket inSocket;
    private InputStream in;
    private OutputStream out;
    private String destIP;
    private SimpleDateFormat df = new SimpleDateFormat(TIME_STAMP_FORMAT, Locale.CANADA);
    private LinkedBlockingQueue<ByteArray> toFilter = new LinkedBlockingQueue<>();
    private SocketChannel inChannel, outChannel;

    public LocalServerForwarder(Socket inSocket, Socket outSocket, boolean isOutgoing, MyVpnService vpnService) {
        this.inSocket = inSocket;
        try {
            this.in = inSocket.getInputStream();
            this.out = outSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.outgoing = isOutgoing;
        this.destIP = outSocket.getInetAddress().getHostAddress();
        if (outSocket.getPort() == 443) destIP += " (SSL)";
        this.vpnService = vpnService;
        if (outgoing) this.plugins = vpnService.getNewPlugins();
        setDaemon(true);
    }

    public LocalServerForwarder(SocketChannel in, SocketChannel out, boolean isOutgoing, MyVpnService vpnService) {
        this.inChannel = in;
        this.outChannel = out;
        this.outgoing = isOutgoing;
        this.destIP = out.socket().getInetAddress().getHostAddress();
        if (out.socket().getPort() == 443) destIP += " (SSL)";
        this.vpnService = vpnService;
        if (outgoing) this.plugins = vpnService.getNewPlugins();
        setDaemon(true);
    }

    public static void connect(Socket clientSocket, Socket serverSocket, MyVpnService vpnService) throws Exception {
        if (clientSocket != null && serverSocket != null && clientSocket.isConnected() && serverSocket.isConnected()) {
            clientSocket.setSoTimeout(0);
            serverSocket.setSoTimeout(0);
            LocalServerForwarder clientServer = new LocalServerForwarder(clientSocket, serverSocket, true, vpnService);
            LocalServerForwarder serverClient = new LocalServerForwarder(serverSocket, clientSocket, false, vpnService);
            clientServer.start();
            serverClient.start();

            if (DEBUG) Logger.d(TAG, "Start forwarding for " + clientSocket.getInetAddress().getHostAddress()+ ":" + clientSocket.getPort() + "->" + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getPort());
            while (clientServer.isAlive())
                clientServer.join();
            while (serverClient.isAlive())
                serverClient.join();
            if (DEBUG) Logger.d(TAG, "Stop forwarding " + clientSocket.getInetAddress().getHostAddress()+ ":" + clientSocket.getPort() + "->" + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getPort());
            clientSocket.close();
            serverSocket.close();
        } else {
            if (DEBUG) Logger.d(TAG, "skipping socket forwarding because of invalid sockets");
            if (clientSocket != null && clientSocket.isConnected()) {
                clientSocket.close();
            }
            if (serverSocket != null && serverSocket.isConnected()) {
                serverSocket.close();
            }
        }
    }

    public void run() {
        FilterThread filterThread = null;
        if (outgoing) {
            filterThread = new FilterThread();
            if (PrivacyGuard.doFilter && PrivacyGuard.asynchronous) filterThread.start();
        }
        try {
            byte[] buff = new byte[LIMIT];
            int got;
            while ((got = in.read(buff)) > -1) {
                if (PrivacyGuard.doFilter && outgoing) {
                    if (PrivacyGuard.asynchronous) {
                        if (!filterThread.isAlive()) filterThread.start();
                        toFilter.offer(byteArrayPool.getByteArray(buff, got));
                    } else {
                        if (filterThread.isAlive()) filterThread.interrupt();
                        filterThread.filter(new String(buff, 0, got));
                    }
                }
                out.write(buff, 0, got);
                out.flush();
            }
        } catch (Exception ignore) {
            ignore.printStackTrace();
            Logger.d(TAG, "outgoing : " + outgoing);
            // can happen when app opens a connection and then terminates it right away so
            // this thread will start running only after a FIN has already been to the server
        }
        if (outgoing && PrivacyGuard.asynchronous && filterThread.isAlive()) filterThread.interrupt();
    }

    public class FilterThread extends Thread {
        public void filter(String msg) {
            if (PrivacyGuard.doFilter && outgoing) {
                String appName = null;
                String packageName = null;
                ConnectionDescriptor des = vpnService.getClientAppResolver().getClientDescriptorBySocket(inSocket);
                if (des != null) {
                    appName = des.getName();
                    packageName = des.getNamespace();
                } else {
                    appName = UNKNOWN;
                    packageName = UNKNOWN;
                }

                Logger.logTraffic(packageName, appName, destIP, msg);

                for (IPlugin plugin : plugins) {
                    LeakReport leak = plugin.handleRequest(msg);
                    if (leak != null) {
                        leak.appName = appName;
                        leak.packageName = packageName;
                        // w3kim@uwaterloo.ca : added an additional parameter to notify
                        vpnService.notify(msg, leak);
                        if (DEBUG) Logger.v(TAG, appName + " is leaking " + leak.category.name());
                        Logger.logLeak(leak.category.name());
                    }
                }
            }
        }

        public void run() {
            try {
                while (!interrupted()) {
                    ByteArray temp = toFilter.take();
                    //Log.d("toFilter", "" + toFilter.size());
                    String msg = new String(temp.data(), 0, temp.length());
                    byteArrayPool.release(temp);
                    filter(msg);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
