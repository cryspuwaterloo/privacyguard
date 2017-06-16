package com.PrivacyGuard.Application.Network;

import android.util.Log;

import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.Network.FakeVPN.MyVpnService;
import com.PrivacyGuard.Application.Network.Forwarder.LocalServerForwarder;
import com.PrivacyGuard.Application.Network.SSL.SSLSocketBuilder;

import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.plugin.proxy.SiteData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static com.PrivacyGuard.Application.Logger.getDiskFileDir;

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
    private static File failedaddress = new File(getDiskFileDir(), "FailAddress");
    //private Set<String> sslPinning = new HashSet<String>();
    private Map<String, Integer> sslPinning = new HashMap<String, Integer>();
    private Integer MAXFAIL = 100;
    Random rand = new Random();


    public LocalServer(MyVpnService vpnService) {
        //if(serverSocketChannel == null || !serverSocketChannel.isOpen())
            try {
                listen();
            } catch (IOException e) {
                if(DEBUG) Log.d(TAG, "Listen error");
                e.printStackTrace();
            }
        this.vpnService = vpnService;
        readfromfile();
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


    private void readfromfile(){
        //Log.d(TAG, "testing start");
        try{
            if(failedaddress.exists()) {
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(failedaddress));
                //Log.d(TAG, "testing reading");
                sslPinning = (HashMap<String, Integer>) inputStream.readObject();
                inputStream.close();
                for (Map.Entry entry : sslPinning.entrySet()) {
                    //Log.d(TAG, "testing reading "+ entry.getKey() + ", " + entry.getValue());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    private void writetofile(){
        try{
            //Log.d(TAG, "testing writing");
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(failedaddress));
            outputStream.writeObject(sslPinning);
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean contains(String address){
        //Logger.d(TAG, "testing" + address);
        if(sslPinning.containsKey(address) && sslPinning.get(address) >= MAXFAIL) {
           if(rand.nextInt(100)+1 <= 5){
               sslPinning.remove(address);
               writetofile();
               return false;
           }
            //Logger.d(TAG, "Testing Skip because fail tp connect to " + address + " " + sslPinning.get(address) + " times.");

            return true;
        }else{
            return false;
        }
    }

    private void add(String address){
        if(sslPinning.containsKey(address)){
            sslPinning.put(address, sslPinning.get(address) + 1);
        } else{
            sslPinning.put(address, 1);
        }
        writetofile();

        //Logger.d(TAG, "Testing Fail tp connect to " + address + " " + sslPinning.get(address) + " times.");
    }

    private void remove(String address){
        if(sslPinning.containsKey(address)){
            sslPinning.remove(address);
            writetofile();
            //Logger.d(TAG, "Testing removed" + address);
        }
        //Logger.d(TAG, "Testing successed" + address);
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

                    if (!contains(descriptor.getRemoteAddress())) {
                        SiteData remoteData = vpnService.getHostNameResolver().getSecureHost(client, descriptor, true);
                        Logger.d(TAG, "Begin Local Handshake : " + remoteData.tcpAddress + " " + remoteData.name);
                        SSLSocket ssl_client = SSLSocketBuilder.negotiateSSL(client, remoteData, false, vpnService.getSSlSocketFactoryFactory());
                        SSLSession session = ssl_client.getSession();
                        Logger.d(TAG, "After Local Handshake : " + remoteData.tcpAddress + " " + remoteData.name + " " + session + " is valid : " + session.isValid());
                        if (session.isValid()) {
                            Socket ssl_target = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(target, descriptor.getRemoteAddress(), descriptor.getRemotePort(), true);
                            SSLSession tmp_session = ((SSLSocket) ssl_target).getSession();
                            Logger.d(TAG, "Remote Handshake : " + tmp_session + " is valid : " + tmp_session.isValid());
                            remove(descriptor.getRemoteAddress());
                            if (tmp_session.isValid()) {
                                client = ssl_client;
                                target = ssl_target;
                            } else {
                                add(descriptor.getRemoteAddress());
                                ssl_client.close();
                                ssl_target.close();
                                client.close();
                                target.close();
                                return;
                            }
                        } else {
                            add(descriptor.getRemoteAddress());
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
