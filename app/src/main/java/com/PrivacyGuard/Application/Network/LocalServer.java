package com.PrivacyGuard.Application.Network;

import android.util.Log;

import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.Network.FakeVPN.MyVpnService;
import com.PrivacyGuard.Application.Network.Forwarder.LocalServerForwarder;
import com.PrivacyGuard.Application.Network.SSL.SSLSocketBuilder;

import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.plugin.proxy.SiteData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by frank on 2014-06-03.
 */
public class LocalServer extends Thread {
    public static final int SSLPort = 443;
    private static final boolean DEBUG = true;
    private static final String TAG = LocalServer.class.getSimpleName();
    public static int port = 12345;
    //private ServerSocketChannel serverSocketChannel;
    private ServerSocket serverSocket;
    private MyVpnService vpnService;
    private Set<String> sslPinning = new HashSet<String>();

    public LocalServer(MyVpnService vpnService) {
        //if(serverSocketChannel == null || !serverSocketChannel.isOpen())
            try {
                listen();
            } catch (IOException e) {
                if(DEBUG) Log.d(TAG, "Listen error");
                e.printStackTrace();
            }
        this.vpnService = vpnService;
    }

    private void listen() throws IOException {
        //serverSocketChannel = ServerSocketChannel.open();
        //serverSocketChannel.socket().setReuseAddress(true);
        //serverSocketChannel.socket().bind(null);
        //port = serverSocketChannel.socket().getLocalPort();
        serverSocket = new ServerSocket();
        serverSocket.bind(null);
        port = serverSocket.getLocalPort();
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                Logger.d(TAG, "Accepting");
                //SocketChannel socketChannel = serverSocketChannel.accept();
                //Socket socket = socketChannel.socket();
                Socket socket = serverSocket.accept();
                vpnService.protect(socket);
                Logger.d(TAG, "Receiving : " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                new Thread(new LocalServerHandler(socket)).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "Stop Listening");
    }

    private class LocalServerHandler implements Runnable {
        private final String TAG = LocalServerHandler.class.getSimpleName();
        private Socket client;
        public LocalServerHandler(Socket client) {
            this.client = client;
        }
        @Override
        public void run() {
            try {
                ConnectionDescriptor descriptor = vpnService.getClientAppResolver().getClientDescriptorByPort(client.getPort());
                //SocketChannel targetChannel = SocketChannel.open();
                //Socket target = targetChannel.socket();
                Socket target = new Socket();
                target.bind(null);
                vpnService.protect(target);
                //boolean result = targetChannel.connect(new InetSocketAddress(descriptor.getRemoteAddress(), descriptor.getRemotePort()));
                target.connect(new InetSocketAddress(descriptor.getRemoteAddress(), descriptor.getRemotePort()));

                if(descriptor != null && descriptor.getRemotePort() == SSLPort) {

                    if (!sslPinning.contains(descriptor.getRemoteAddress())) {
                        SiteData remoteData = vpnService.getHostNameResolver().getSecureHost(client, descriptor, true);
                        Logger.d(TAG, "Begin Local Handshake : " + remoteData.tcpAddress + " " + remoteData.name);
                        SSLSocket ssl_client = SSLSocketBuilder.negotiateSSL(client, remoteData, false, vpnService.getSSlSocketFactoryFactory());
                        SSLSession session = ssl_client.getSession();
                        Logger.d(TAG, "After Local Handshake : " + remoteData.tcpAddress + " " + remoteData.name + " " + session + " is valid : " + session.isValid());
                        if (session.isValid()) {
                            Socket ssl_target = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(target, descriptor.getRemoteAddress(), descriptor.getRemotePort(), true);
                            SSLSession tmp_session = ((SSLSocket) ssl_target).getSession();
                            Logger.d(TAG, "Remote Handshake : " + tmp_session + " is valid : " + tmp_session.isValid());
                            if (tmp_session.isValid()) {
                                client = ssl_client;
                                target = ssl_target;
                            } else {
                                sslPinning.add(descriptor.getRemoteAddress());
                                ssl_client.close();
                                ssl_target.close();
                                client.close();
                                target.close();
                                return;
                            }
                        } else {
                            sslPinning.add(descriptor.getRemoteAddress());
                            ssl_client.close();
                            client.close();
                            target.close();
                            return;
                        }
                    }
                    else {
                            Logger.d(TAG, "Skipping TLS interception for " + descriptor.getRemoteAddress() + ":" + descriptor.getRemotePort() + " due to suspected pinning");
                        }
                }
                LocalServerForwarder.connect(client, target, vpnService);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
