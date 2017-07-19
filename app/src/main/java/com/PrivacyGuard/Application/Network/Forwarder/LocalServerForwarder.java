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
import com.PrivacyGuard.Application.Network.FilterThread;
import com.PrivacyGuard.Application.PrivacyGuard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class LocalServerForwarder extends Thread {

    private static final String TAG = LocalServerForwarder.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static int LIMIT = 1368;

    private boolean outgoing = false;
    private MyVpnService vpnService;
    private InputStream in;
    private OutputStream out;
    private String destIP, srcIP;
    private int destPort, srcPort;
    private String appName, packageName;

    public LocalServerForwarder(Socket inSocket, Socket outSocket, boolean isOutgoing, MyVpnService vpnService, String appName, String packageName) {
        try {
            this.in = inSocket.getInputStream();
            this.out = outSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.outgoing = isOutgoing;
        this.destIP = outSocket.getInetAddress().getHostAddress();
        this.destPort = outSocket.getPort();
        if (this.destPort == 443) destIP += " (SSL)";
        this.srcIP = inSocket.getInetAddress().getHostAddress();
        this.srcPort = inSocket.getPort();
        this.vpnService = vpnService;
        setDaemon(true);
        this.appName = appName;
        this.packageName = packageName;
    }

    public static void connect(Socket clientSocket, Socket serverSocket, MyVpnService vpnService, String appName, String packageName) throws Exception {
        if (clientSocket != null && serverSocket != null && clientSocket.isConnected() && serverSocket.isConnected()) {
            clientSocket.setSoTimeout(0);
            serverSocket.setSoTimeout(0);
            LocalServerForwarder clientServer = new LocalServerForwarder(clientSocket, serverSocket, true, vpnService, appName, packageName);
            LocalServerForwarder serverClient = new LocalServerForwarder(serverSocket, clientSocket, false, vpnService, appName, packageName);
            clientServer.start();
            serverClient.start();

            if (DEBUG) Logger.d(TAG, "Start forwarding for " + clientSocket.getInetAddress().getHostAddress()+ ":" + clientSocket.getPort() + "<->" + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getPort());
            while (clientServer.isAlive() && serverClient.isAlive()) {
                try {
                    Thread.sleep(10);
                    } catch (InterruptedException e) {
                }
            }
            if (DEBUG) Logger.d(TAG, "Stop forwarding " + clientSocket.getInetAddress().getHostAddress()+ ":" + clientSocket.getPort() + "<->" + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getPort());
            clientSocket.close();
            serverSocket.close();
            clientServer.join();
            serverClient.join();

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

        FilterThread filterObject = null;
        if (!PrivacyGuard.asynchronous) filterObject = new FilterThread(vpnService, appName, packageName, srcPort, destIP, destPort);

        try {
            byte[] buff = new byte[LIMIT];
            int got;
            while ((got = in.read(buff)) > -1) {
                if (PrivacyGuard.doFilter && outgoing) {
                    String msg = new String(buff, 0, got);
                    if (PrivacyGuard.asynchronous) {
                        vpnService.getFilterThread().offer(msg, appName, packageName, srcPort, destIP, destPort);
                    } else {
                        filterObject.filter(msg);
                    }
                }
                if (DEBUG) Logger.d(TAG, got + " bytes to be written to " + srcIP + ":" + srcPort + "->" + destIP + ":" + destPort);
                out.write(buff, 0, got);
                if (DEBUG) Logger.d(TAG, got + " bytes written to " + srcIP + ":" + srcPort + "->" + destIP + ":" + destPort);
                out.flush();
            }
            if (DEBUG) Logger.d(TAG, "terminating " + srcIP + ":" + srcPort + "->" + destIP + ":" + destPort);
        } catch (Exception ignore) {
            ignore.printStackTrace();
            if (DEBUG) Logger.d(TAG, "outgoing : " + outgoing);
            // can happen when app opens a connection and then terminates it right away so
            // this thread will start running only after a FIN has already been to the server
        }
    }

}
