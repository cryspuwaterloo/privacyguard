/*
 * Thread for writing response to the virtual interface
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

import ca.uwaterloo.crysp.privacyguard.Application.Network.FakeVPN.MyVpnService;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by y59song on 02/06/14.
 */
public class TunWriteThread extends Thread {
    private final FileOutputStream localOut;
    private LinkedBlockingQueue<byte[]> writeQueue = new LinkedBlockingQueue<>();

    public TunWriteThread(FileDescriptor fd, MyVpnService vpnService) {
        localOut = new FileOutputStream(fd);
    }

    public void run() {
        try {
            while (!isInterrupted()) {
                byte[] temp = writeQueue.take();
                try {
                    localOut.write(temp);
                } catch (Exception e) {
                    e.printStackTrace();
                    clean();
                    return;
                }
            }
            clean();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            clean();
        }
    }

    public void write(byte[] data) {
        writeQueue.offer(data);
    }

    private void clean() {
        try {
            localOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        writeQueue.clear();
    }
}
