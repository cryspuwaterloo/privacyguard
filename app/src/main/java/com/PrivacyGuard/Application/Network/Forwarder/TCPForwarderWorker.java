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
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by y59song on 03/04/14.
 *
 * Acts as an intermediate between TCPForwarder and LocalServer
 */
public class TCPForwarderWorker extends Thread {
    private static final String TAG = TCPForwarderWorker.class.getSimpleName();
    private static final boolean DEBUG = false;
    private final int limit = 1368;
    //private SocketChannel socketChannel;
    //private Selector selector;
    private Socket socket;
    private TCPForwarder forwarder;
    private ByteBuffer msg = ByteBuffer.allocate(limit);
    private LinkedBlockingQueue<byte[]> requests = new LinkedBlockingQueue<>();
    private Sender sender;
    private int src_port;

    public TCPForwarderWorker(Socket socket, TCPForwarder forwarder, int src_port) {
        this.forwarder = forwarder;
        this.socket = socket;
        this.src_port = src_port;
    }

    public boolean isValid() {
    //    return selector != null;
        return true;
    }

    //public void send(byte[] request) {
    //    requests.offer(request);
    //}

    @Override
    // reads responses from socket connected to LocalServer and passes them on to TCPForwarder
    public void run() {
        //sender = new Sender();
        //sender.start();

        try {
            byte[] buff = new byte[limit];
            int got;
            InputStream in = socket.getInputStream();
            while ((got = in.read(buff)) > -1) {
                if (DEBUG) Logger.d(TAG, got + " response bytes to be written to " + src_port);
                PrivacyGuard.tcpForwarderWorkerRead += got;
                byte[] temp = new byte[got];
                System.arraycopy(buff, 0, temp, 0, got);
                forwarder.forwardResponse(temp);
            }
        } catch (IOException e) {
            // socket got closed by TCPForwarder
            //e.printStackTrace();
        }
                //            }
        //while (!isInterrupted() && selector.isOpen()) {
        //   try {
        //      selector.select(0);   // blocks till there is some data to read
        // } catch (IOException e) {
        //       e.printStackTrace();
        //   }
        //   Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        //    while (!isInterrupted() && iterator.hasNext()) {
        //        SelectionKey key = iterator.next();
        //        iterator.remove();
        //while (key.isValid() && key.isReadable()) {
        //            try {
        //                msg.clear();
        //                Logger.d(TAG, "reading on port: " + src_port);
        //                int length = socketChannel.read(msg);
        //                if (length <= 0 || isInterrupted()) {
        //                    if (length == 0) Logger.d(TAG, "read length is zero for port: " + src_port);
        //                    close();
        //                    return;
        //                }
        //                Logger.d(TAG, "read " + length + " bytes on port: " + src_port);
        //                msg.flip();
        //                byte[] temp = new byte[length];
        //                msg.get(temp);
        //                PrivacyGuard.tcpForwarderWorkerRead += length;
        //                forwarder.forwardResponse(temp);
        //            } catch (IOException e) {
        //                e.printStackTrace();
        //            }
        //        }
        //    }
        //}
        //close();
    }

    public void close() {
       // try {
            //if (selector != null) selector.close();
        //} catch (IOException e) {
          //  e.printStackTrace();
        //}
        if (sender != null && sender.isAlive()) {
            sender.interrupt();
        }
        try {
           // if (socketChannel.isConnected()) {
           //     socketChannel.socket().close();
           //     socketChannel.close();
            socket.close();
                Logger.d(TAG, "closed socket for port " + src_port);
           // }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // reads payloads queued by TCPForwarder and puts them into socket connected to LocalServer
    public class Sender extends Thread {
        public void run() {
            try {
                byte[] temp;
                OutputStream stream = socket.getOutputStream();
                //while (!isInterrupted() && !socketChannel.socket().isClosed()) {
                while (!isInterrupted() && !socket.isClosed()) {
                        temp = requests.take();
                        //ByteBuffer tempBuf = ByteBuffer.wrap(temp);
                        //while (true) {
                            //int written = socketChannel.write(tempBuf);
                            stream.write(temp);
                            stream.flush();
                            Logger.d(TAG, temp.length + " bytes forwarded to LocalServer from port: " + src_port);
                            PrivacyGuard.tcpForwarderWorkerWrite += temp.length;
                            //if (tempBuf.hasRemaining()) {
                            //    Thread.sleep(10);
                            //} else break;
                        //}
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