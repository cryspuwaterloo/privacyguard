package com.PrivacyGuard.Application.Network;

import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.Network.FakeVPN.MyVpnService;
import com.PrivacyGuard.Application.Network.Forwarder.LocalServerForwarder;
import com.PrivacyGuard.Application.Network.SSL.SSLSocketBuilder;
import com.PrivacyGuard.Utilities.CertificateManager;
import com.PrivacyGuard.Application.Network.SSL.TLSWhiteList;

import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.plugin.proxy.SiteData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static com.PrivacyGuard.Application.Logger.getDiskFileDir;

/**
 * Created by frank on 2014-06-03.
 */
public class LocalServer extends Thread {
    public static final int SSLPort = 443;
    private static final boolean DEBUG = false;
    private static final String TAG = LocalServer.class.getSimpleName();
    public static int port = 12345;
    private ServerSocket serverSocket;
    private MyVpnService vpnService;
    private TLSWhiteList tlsWhiteList= new TLSWhiteList(getDiskFileDir(), "TLSInterceptFailures");

    public LocalServer(MyVpnService vpnService) {
            try {
                listen();
            } catch (IOException e) {
                if(DEBUG) Logger.d(TAG, "Listen error");
                e.printStackTrace();
            }
        this.vpnService = vpnService;
    }

    private void listen() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(null);
        port = serverSocket.getLocalPort();
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                if (DEBUG) Logger.d(TAG, "Accepting");
                Socket socket = serverSocket.accept();
                vpnService.protect(socket);
                if (DEBUG) Logger.d(TAG, "Receiving : " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                new Thread(new LocalServerHandler(socket)).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (DEBUG) Logger.d(TAG, "Stop Listening");
    }

    private class LocalServerHandler implements Runnable {
        private final String TAG = LocalServerHandler.class.getSimpleName();
        private static final String UNKNOWN = "unknown";
        private Socket client;
        public LocalServerHandler(Socket client) {
            this.client = client;
        }
        @Override
        public void run() {
            try {
                ConnectionDescriptor descriptor = vpnService.getClientAppResolver().getClientDescriptorBySocket(client);
                String appName, packageName;
                if (descriptor != null) {
                    appName = descriptor.getName();
                    packageName = descriptor.getNamespace();
                } else {
                    appName = UNKNOWN;
                    packageName = UNKNOWN;
                }

                if (DEBUG) Logger.d(TAG, "app name: " + appName + " / package name: " + packageName);
                Socket target = new Socket();
                target.bind(null);
                vpnService.protect(target);
                // TODO: why do this and the call above return different results?
                descriptor = vpnService.getClientAppResolver().getClientDescriptorByPort(client.getPort());
                target.connect(new InetSocketAddress(descriptor.getRemoteAddress(), descriptor.getRemotePort()));

                if(descriptor != null && descriptor.getRemotePort() == SSLPort) {

                    if (!tlsWhiteList.contains(descriptor.getRemoteAddress(), packageName)) {
                        SiteData remoteData = vpnService.getHostNameResolver().getSecureHost(client, descriptor, true);
                        if (DEBUG) Logger.d(TAG, "Begin Local Handshake : " + remoteData.tcpAddress + " " + remoteData.name);
                        SSLSocket ssl_client = SSLSocketBuilder.negotiateSSL(client, remoteData, false, CertificateManager.getSSLSocketFactoryFactory());
                        SSLSession session = ssl_client.getSession();
                        if (DEBUG) Logger.d(TAG, "After Local Handshake : " + remoteData.tcpAddress + " " + remoteData.name + " " + session + " is valid : " + session.isValid());
                        if (session.isValid()) {
                            // UH: this uses default SSLSocketFactory, which verifies hostname, does it also check for certificate expiration?
                            Socket ssl_target = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(target, descriptor.getRemoteAddress(), descriptor.getRemotePort(), true);
                            SSLSession tmp_session = ((SSLSocket) ssl_target).getSession();
                            if (DEBUG) Logger.d(TAG, "Remote Handshake : " + tmp_session + " is valid : " + tmp_session.isValid());
                            if (tmp_session.isValid()) {
                                client = ssl_client;
                                target = ssl_target;
                                tlsWhiteList.remove(descriptor.getRemoteAddress());

                            } else {
                                ssl_client.close();
                                ssl_target.close();
                                client.close();
                                target.close();
                                return;
                            }
                        } else {
                            tlsWhiteList.add(descriptor.getRemoteAddress());
                            ssl_client.close();
                            client.close();
                            target.close();
                            return;
                        }
                    } else {
                        if (DEBUG) Logger.d(TAG, "Skipping TLS interception for " + descriptor.getRemoteAddress() + ":" + descriptor.getRemotePort() + " due to whitelisting");
                    }
                }
                LocalServerForwarder.connect(client, target, vpnService, packageName, appName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
