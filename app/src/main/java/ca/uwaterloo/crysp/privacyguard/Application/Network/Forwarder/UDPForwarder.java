/*
 * Implement a simple udp protocol
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

package ca.uwaterloo.crysp.privacyguard.Application.Network.Forwarder;

import ca.uwaterloo.crysp.privacyguard.Application.Logger;
import ca.uwaterloo.crysp.privacyguard.Application.Network.FakeVPN.MyVpnService;
import ca.uwaterloo.crysp.privacyguard.Application.Network.Protocol.IP.IPDatagram;
import ca.uwaterloo.crysp.privacyguard.Application.Network.Protocol.IP.IPPayLoad;
import ca.uwaterloo.crysp.privacyguard.Application.Network.Protocol.UDP.UDPDatagram;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Created by frank on 2014-03-29.
 */
public class UDPForwarder extends AbsForwarder { //} implements ICommunication {
    private static final String TAG = UDPForwarder.class.getSimpleName();
    private static final boolean DEBUG = false;
    private final int LIMIT = 32767;
    private final int WAIT_BEFORE_RELEASE_PERIOD = 60000;
    //private InetAddress dstAddress;
    //private int dstPort;
    private DatagramSocket socket;
    private ByteBuffer packet;
    private DatagramPacket response;
    private UDPForwarderWorker worker;
    private boolean first = true;
    protected long releaseTime;

    public UDPForwarder(MyVpnService vpnService, int port) {
        super(vpnService, port);
        packet = ByteBuffer.allocate(LIMIT);
        response = new DatagramPacket(packet.array(), LIMIT);
    }

    @Override
    public void forwardRequest(IPDatagram ipDatagram) {
        if (first) {
            setup(ipDatagram);
            first = false;
        }
        UDPDatagram udpDatagram = (UDPDatagram)ipDatagram.payLoad();

        //udpDatagram.debugInfo(dstAddress);
        if (DEBUG) Logger.d("UDPForwarder", "forwarding " + udpDatagram.debugString());

        send(udpDatagram, ipDatagram.header().getDstAddress(), ipDatagram.payLoad().getDstPort());

        releaseTime = System.currentTimeMillis() + WAIT_BEFORE_RELEASE_PERIOD;
    }

    @Override
    // Should never get called since we don't use LocalServer for UDP
    public void forwardResponse(byte[] response) {
        if (DEBUG) Logger.d("UDPForwarder", "Unsolicited packet received: " + response);
    }

    public boolean setup(IPDatagram firstRequest) {
        try {
            socket = new DatagramSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
        vpnService.protect(socket);
        worker = new UDPForwarderWorker(firstRequest, socket, this);
        worker.start();
        return true;
    }

    public void send(IPPayLoad payLoad, InetAddress dstAddress, int dstPort) {
        try {
            // UDP packets are currently not filtered and don't go through LocalServer
            socket.send(new DatagramPacket(payLoad.data(), payLoad.dataLength(), dstAddress, dstPort));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void release() {
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        worker = null;
        if(socket != null && !socket.isClosed()) {
            socket.close();
        }

        if (DEBUG) Logger.d(TAG, "Releasing UDP forwarder for port " + port);
    }

    @Override
    public boolean hasExpired() { return releaseTime < System.currentTimeMillis();}
}
