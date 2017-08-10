/*
 * Thread for handling and dispatching all ip packets (only tcp and udp)
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

package ca.uwaterloo.crysp.privacyguard.Application.Network.FakeVPN;

import ca.uwaterloo.crysp.privacyguard.Application.Network.Forwarder.ForwarderPools;
import ca.uwaterloo.crysp.privacyguard.Application.Network.Protocol.IP.IPDatagram;
import ca.uwaterloo.crysp.privacyguard.Application.Logger;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by y59song on 06/06/14.
 */
public class TunReadThread extends Thread {
    private final FileInputStream localIn;
    private final FileChannel localInChannel;
    private final int LIMIT = 2048;
    private final ForwarderPools forwarderPools;
    private final Dispatcher dispatcher;
    private LinkedBlockingQueue<IPDatagram> readQueue = new LinkedBlockingQueue<>();
    private static final String TAG = TunReadThread.class.getSimpleName();
    private static final boolean DEBUG = false;

    public TunReadThread(FileDescriptor fd, MyVpnService vpnService) {
        localIn = new FileInputStream(fd);
        localInChannel = localIn.getChannel();
        this.forwarderPools = vpnService.getForwarderPools();
        dispatcher = new Dispatcher();
    }

    public void run() {
        try {
            ByteBuffer packet = ByteBuffer.allocate(LIMIT);
            IPDatagram ip;
            dispatcher.start();
            while (!isInterrupted()) {
                packet.clear();
                if (localInChannel.read(packet) > 0) {
                    packet.flip();
                    if ((ip = IPDatagram.create(packet)) != null) {
                        if (DEBUG) Logger.d(TAG, "receiving: " + ip.headerToString());
                        readQueue.offer(ip);
                    }
                } else {
                    // shouldn't happen given that read() is blocking
                    Thread.sleep(100);
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        clean();
    }

    private void clean() {
        dispatcher.interrupt();
        try {
            localIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Dispatcher extends Thread {
        public void run() {
            try {
                while (!isInterrupted()) {
                    IPDatagram temp = readQueue.take();
                    int port = temp.payLoad().getSrcPort();
                    forwarderPools.get(port, temp.header().protocol()).forwardRequest(temp);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
