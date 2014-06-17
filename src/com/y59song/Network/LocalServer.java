package com.y59song.Network;

import android.util.Log;
import com.y59song.Forwader.MySocketForwarder;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Utilities.SSLSocketBuilder;
import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.plugin.proxy.SiteData;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by frank on 2014-06-03.
 */
public class LocalServer extends Thread {
  private static final boolean DEBUG = false;
  private static final String TAG = LocalServer.class.getSimpleName();
  public static final int port = 12345;
  public static final int SSLPort = 443;

  private ServerSocketChannel serverSocketChannel;
  private MyVpnService vpnService;
  public LocalServer(MyVpnService vpnService) {
    if(serverSocketChannel == null || !serverSocketChannel.isOpen())
      try {
        listen();
      } catch (IOException e) {
        if(DEBUG) Log.d(TAG, "Listen error");
        e.printStackTrace();
      }
    this.vpnService = vpnService;
  }

  private void listen() throws IOException {
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(port));
  }

  private class ForwarderHandler implements Runnable {
    private Socket client;
    public ForwarderHandler(Socket client) {
      this.client = client;
    }
    @Override
    public void run() {
      try {
        ConnectionDescriptor descriptor = vpnService.getClientResolver().getClientDescriptorBySocket(client);
        SocketChannel targetChannel = SocketChannel.open();
        Socket target = targetChannel.socket();
        vpnService.protect(target);
        if(descriptor.getRemotePort() == SSLPort) {
          SiteData remoteData = vpnService.getResolver().getSecureHost(client, descriptor, true); // TODO
          Log.d(TAG, "Begin Handshake : " + remoteData.tcpAddress + " " + remoteData.hostName);
          client = SSLSocketBuilder.negotiateSSL(client, remoteData, false, vpnService.getSSlSocketFactoryFactory());
          ((SSLSocket)client).getSession();
          Log.d(TAG, "After Handshake");
          targetChannel.connect(new InetSocketAddress(descriptor.getRemoteAddress(), descriptor.getRemotePort()));
          target = ((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(target, descriptor.getRemoteAddress(), descriptor.getRemotePort(), true);
        } else {
          targetChannel.connect(new InetSocketAddress(descriptor.getRemoteAddress(), descriptor.getRemotePort()));
        }
        MySocketForwarder.connect("", client, target);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void run() {
    while(!isInterrupted()) {
      try {
        if(DEBUG) Log.d(TAG, "Accepting");
        SocketChannel socketChannel = serverSocketChannel.accept();
        Socket socket = socketChannel.socket();
        if(DEBUG) Log.d(TAG, "Receiving : " + socket.getInetAddress().getHostAddress());
        new Thread(new ForwarderHandler(socketChannel.socket())).start();
        if(DEBUG) Log.d(TAG, "Not blocked");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}