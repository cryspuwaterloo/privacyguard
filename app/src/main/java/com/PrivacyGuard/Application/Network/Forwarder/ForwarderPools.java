/*
 * Pool for all forwarders
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

import android.util.Pair;

import com.PrivacyGuard.Application.Network.FakeVPN.MyVpnService;
import com.PrivacyGuard.Application.Network.Protocol.IP.IPDatagram;
import com.PrivacyGuard.Application.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by frank on 2014-04-01.
 */
public class ForwarderPools {
    private HashMap<Pair<Integer, Byte>, AbsForwarder> portToForwarder;
    private MyVpnService vpnService;
    private static final String TAG = ForwarderPools.class.getSimpleName();
    private static final boolean DEBUG = false;

    public ForwarderPools(MyVpnService vpnService) {
        this.vpnService = vpnService;
        portToForwarder = new HashMap<>();
    }

    public AbsForwarder get(int port, byte protocol) {

        releaseExpiredForwarders();

        // Using only src port and protocol for key will fail if the same src port is
        // used for multiple connections to different hosts or dest ports, which is
        // legitimate for TCP. Does it actually happen on Android?
        Pair<Integer, Byte> key = new Pair<>(port, protocol);
        if (portToForwarder.containsKey(key)) { //&& !portToForwarder.get(key).isClosed()) {
            // if forwarder is closed, packet will be discarded; don't create a new forwarder
            // since packet is likely for old connection
            return portToForwarder.get(key);
        } else {
            AbsForwarder temp = getByProtocol(protocol, port);
            if (temp != null) {
                portToForwarder.put(key, temp);
            }
            return temp;
        }
    }

    void releaseExpiredForwarders() {
        Iterator it = portToForwarder.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            AbsForwarder fw = (AbsForwarder)pair.getValue();
            if (fw.hasExpired()) {
                fw.release();
                it.remove(); // avoids a ConcurrentModificationException
            }
        }
    }

    private AbsForwarder getByProtocol(byte protocol, int port) {
        switch (protocol) {
            case IPDatagram.TCP:
                if (DEBUG) Logger.d(TAG, "Creating TCP forwarder for src port " + port);
                return new TCPForwarder(vpnService, port);
            case IPDatagram.UDP:
                if (DEBUG) Logger.d(TAG, "Creating UDP forwarder for src port " + port);
                return new UDPForwarder(vpnService, port);
            default:
                if (DEBUG) Logger.d(TAG, "Unknown type of forwarder requested for protocol " + protocol);
                return null;
        }
    }

    public void release(UDPForwarder udpForwarder) {
        portToForwarder.values().removeAll(Collections.singleton(udpForwarder));
    }

    public void release(TCPForwarder tcpForwarder) {
        portToForwarder.values().removeAll(Collections.singleton(tcpForwarder));
    }
}
