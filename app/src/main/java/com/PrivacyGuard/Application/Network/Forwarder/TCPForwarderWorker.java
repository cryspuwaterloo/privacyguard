package com.PrivacyGuard.Application.Network.Forwarder;

import com.PrivacyGuard.Application.Network.Forwarder.TCPForwarder;
import com.PrivacyGuard.Application.Network.LocalServer;
import com.PrivacyGuard.Application.PrivacyGuard;
import com.PrivacyGuard.Application.Logger;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by y59song on 03/04/14.
 *
 * Acts as an intermediate between TCPForwarder and LocalServer
 */
public class TCPForwarderWorker extends Thread {
    private final String TAG = "TCPForwarderWorker";
    private final int limit = 1368;
    private SocketChannel socketChannel;
    private Selector selector;
    private TCPForwarder forwarder;
    private ByteBuffer msg = ByteBuffer.allocate(limit);
    private LinkedBlockingQueue<byte[]> requests = new LinkedBlockingQueue<>();
    private Sender sender;

    public TCPForwarderWorker(InetAddress srcAddress, int src_port, InetAddress dstAddress, int dst_port, TCPForwarder forwarder) {
        this.forwarder = forwarder;
        try {
            socketChannel = SocketChannel.open();
            Socket socket = socketChannel.socket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), src_port));
            try {
                socketChannel.connect(new InetSocketAddress(LocalServer.port));
                while (!socketChannel.finishConnect()) ;
            } catch (ConnectException e) {
                e.printStackTrace();
                return;
            }
            socketChannel.configureBlocking(false);
            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isValid() {
        return selector != null;
    }

    public void send(byte[] request) {
        requests.offer(request);
    }

    @Override
    // reads responses from socket connected to LocalServer and passes them on to TCPForwarder
    public void run() {
        sender = new Sender();
        sender.start();
        while (!isInterrupted() && selector.isOpen()) {
            try {
                selector.select(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (!isInterrupted() && iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (!key.isValid()) continue;
                else if (key.isReadable()) {
                    try {
                        msg.clear();
                        int length = socketChannel.read(msg);
                        if (length <= 0 || isInterrupted()) {
                            close();
                            return;
                        }
                        msg.flip();
                        byte[] temp = new byte[length];
                        msg.get(temp);
                        PrivacyGuard.tcpForwarderWorkerRead += length;
                        forwarder.forwardResponse(temp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        close();
    }

    public void close() {
        try {
            if (selector != null) selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (sender != null && sender.isAlive()) {
            sender.interrupt();
        }
        try {
            if (socketChannel.isConnected()) {
                socketChannel.socket().close();
                socketChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // reads payloads queued by TCPForwarder and puts them into socket connected to LocalServer
    public class Sender extends Thread {
        public void run() {
            try {
                byte[] temp;
                while (!isInterrupted() && !socketChannel.socket().isClosed()) {
                    temp = requests.take();
                    ByteBuffer tempBuf = ByteBuffer.wrap(temp);
                    while (true) {
                        int written = socketChannel.write(tempBuf);
                        Logger.d(TAG, written + " bytes forwarded to LocalServer");
                        PrivacyGuard.tcpForwarderWorkerWrite += written;
                        if (tempBuf.hasRemaining()) {
                            Thread.sleep(10);
                        } else break;
                    }
                }
            } catch (InterruptedException e) {
                // happens when connection gets terminated by TCPForwarder
                //e.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}