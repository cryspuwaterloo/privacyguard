package com.y59song.LocationGuard;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.y59song.Forwader.ForwarderPools;
import com.y59song.Network.IP.IPDatagram;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Enumeration;

/**
 * Created by frank on 2014-03-26.
 */
public class MyVpnService extends VpnService implements Runnable{
  private static final String TAG = "MyVpnService";
  private Thread mThread;

  //The virtual network interface, get and return packets to it
  private ParcelFileDescriptor mInterface;
  private FileInputStream localIn;
  private FileOutputStream localOut;

  //Pools
  private ForwarderPools forwarderPools;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if(mThread != null) mThread.interrupt();
    mThread = new Thread(this, getClass().getSimpleName());
    mThread.start();
    forwarderPools = new ForwarderPools(this);
    return 0;
  }

  @Override
  public void run() {
    configure();
    localIn = new FileInputStream(mInterface.getFileDescriptor());
    localOut = new FileOutputStream(mInterface.getFileDescriptor());
    ByteBuffer packet = ByteBuffer.allocate(2048);

    try {
      while (mInterface != null && mInterface.getFileDescriptor() != null && mInterface.getFileDescriptor().valid()) {
        //packet.clear();
        int length = localIn.read(packet.array());
        if(length > 0) {
          packet.limit(length);
          final IPDatagram ip = IPDatagram.create(packet);
          packet.clear();
          if(ip == null) continue;
          int port = ip.payLoad().getSrcPort();
          Log.d(TAG, "Port : " + ip.payLoad().getDstPort());
          forwarderPools.get(port, ip.header().protocol()).request(ip);
        } else Thread.sleep(100);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public synchronized void fetchResponse(byte[] response) {
    if(localOut == null || response == null) return;
    try {
      Log.d(TAG, "" + response.length);
      localOut.write(response);
      localOut.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public ForwarderPools getForwarderPools() {
    return forwarderPools;
  }

  private InetAddress getLocalAddress() {
    try {
      for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
        NetworkInterface netInterface = en.nextElement();
        for(Enumeration<InetAddress> enumIpAddr = netInterface.getInetAddresses(); enumIpAddr.hasMoreElements();) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          if(!inetAddress.isLoopbackAddress()) {
            return inetAddress;
          }
        }
      }
    } catch (SocketException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void configure() {
    Builder b = new Builder();
    b.addAddress("10.0.0.0", 28);
    //b.addAddress(getLocalAddress(), 28);
    b.addRoute("0.0.0.0", 0);
    //b.addRoute("8.8.8.8", 32);
    //b.addDnsServer("8.8.8.8");
    //b.addRoute("220.181.37.55", 32);
    //b.addRoute("173.194.43.0", 24);
    //b.addRoute("71.19.173.0", 24);
    b.setMtu(1500);
    mInterface = b.establish();
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "destroy");
    super.onDestroy();
    if(mInterface == null) return;
    try {
      mInterface.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
