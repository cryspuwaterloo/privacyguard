package ca.uwaterloo.crysp.privacyguard.Application.Network.Resolver;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import ca.uwaterloo.crysp.privacyguard.Application.Network.FakeVPN.MyVpnService;
import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.plugin.proxy.SiteData;
import org.sandroproxy.utils.DNSProxy;
import org.sandroproxy.utils.PreferenceUtils;

import javax.net.ssl.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.uwaterloo.crysp.privacyguard.Application.Logger;

/**
 * Created by y59song on 05/06/14.
 */
public class MyNetworkHostNameResolver {
  private MyVpnService vpnService;
  private Context mContext;
  private String mHostName;
  private boolean mListenerStarted = false;
  private Map<Integer, SiteData> siteData;
  private Map<String, SiteData> ipPortSiteData;
  private List<SiteData> unresolvedSiteData;
  private HostNameResolver hostNameResolver;

  public static String DEFAULT_SITE_NAME = "privacyguard.untrusted";
  private static String TAG = MyNetworkHostNameResolver.class.getSimpleName();
  private static boolean LOGD = false;

  private native String getOriginalDest(Socket socket);

  static
  {
    System.loadLibrary("socketdest");
  }

  public MyNetworkHostNameResolver(MyVpnService vpnService){
    mContext = vpnService;
    this.vpnService = vpnService;
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    String hostName = pref.getString(PreferenceUtils.proxyTransparentHostNameKey, null);
    if (hostName != null && hostName.length() > 0){
      mHostName = hostName;
    }else{
      startListenerForEvents();
    }
  }

  public void cleanUp(){
    if (mListenerStarted){
      stopListenerForEvents();
    }
  }


  private class HostNameResolver implements Runnable{
    public boolean running = false;

    public void stop() {
      running = false;
    }

    @Override
    public void run() {
      running = true;
      while(running) {
        if (unresolvedSiteData.size() > 0){
          final SiteData siteDataCurrent = unresolvedSiteData.remove(0);
          TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
              public X509Certificate[] getAcceptedIssuers() {
                return null;
              }
              public void checkClientTrusted(X509Certificate[] certs, String authType) {
              }
              public void checkServerTrusted(X509Certificate[] certs, String authType) {
                try{
                  if (certs != null && certs.length > 0 && certs[0].getSubjectDN() != null){
                    // getting subject common name
                    String cnValue = certs[0].getSubjectDN().getName();
                    String[] cnValues = cnValue.split(",");
                    for (String  val : cnValues) {
                      String[] parts = val.split("=");
                      if (parts != null && parts.length == 2 && parts[0].equalsIgnoreCase("cn") && parts[1] != null && parts[1].length() > 0){
                        siteDataCurrent.name = parts[1].trim();
                        if (LOGD) Logger.d(TAG, "Adding hostname to dictionary " + siteDataCurrent.name + " port:" + siteDataCurrent.sourcePort);
                        siteDataCurrent.certs = certs;
                        siteData.put(siteDataCurrent.sourcePort, siteDataCurrent);
                        ipPortSiteData.put(siteDataCurrent.tcpAddress + ":" + siteDataCurrent.destPort, siteDataCurrent);
                        break;
                      }
                    }
                  }
                }catch(Exception e){
                  if (LOGD) Logger.d(TAG, e.getMessage());
                }
              }
            }
          };
          try {
            if (!ipPortSiteData.containsKey(siteDataCurrent.tcpAddress + ":" + siteDataCurrent.destPort)){
              String hostName = siteDataCurrent.hostName != null ? siteDataCurrent.hostName : siteDataCurrent.tcpAddress;
              SocketChannel socketChannel = SocketChannel.open();
              Socket socket = socketChannel.socket();
              vpnService.protect(socket);
              socketChannel.connect(new InetSocketAddress(hostName, siteDataCurrent.destPort));
              SSLContext sslContext = SSLContext.getInstance("TLS");
              sslContext.init(null, trustAllCerts, new SecureRandom());
              SSLSocketFactory factory = sslContext.getSocketFactory();
              SSLSocket sslsocket = (SSLSocket)factory.createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
              sslsocket.setUseClientMode(true);
              sslsocket.getSession();
              sslsocket.close();
              socket.close();
            }else{
              SiteData siteDataCached = ipPortSiteData.get(siteDataCurrent.tcpAddress + ":" + siteDataCurrent.destPort);
              if (LOGD) Logger.d(TAG, "Already have candidate for " + siteDataCached.name + ". No need to fetch " + siteDataCurrent.tcpAddress + " on port:" + siteDataCurrent.destPort);
              siteData.put(siteDataCurrent.sourcePort, siteDataCached);
            }
          } catch (Exception e) {
            if (LOGD) e.printStackTrace();
          }
        }else{
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  private SiteData parseData(Socket socket, ConnectionDescriptor descriptor){
    SiteData newSiteData = new SiteData();
    newSiteData.tcpAddress = descriptor.getRemoteAddress();
    newSiteData.destPort = descriptor.getRemotePort();
    newSiteData.hostName = DNSProxy.getHostNameFromIp(newSiteData.tcpAddress);
    newSiteData.sourcePort = socket.getPort();
    newSiteData.name = "";
    return newSiteData;
  }

  private void getCertificateData(SiteData newSiteData){
    if (!siteData.containsKey(newSiteData.sourcePort)){
      if (LOGD) Logger.d(TAG, "Add hostname to resolve :" +
        newSiteData.tcpAddress + " source port " +
        newSiteData.sourcePort + " uid " +
        newSiteData.appUID);
      unresolvedSiteData.add(newSiteData);
    }
  }

  private void startListenerForEvents(){
    try{
      siteData = new HashMap<Integer, SiteData>();
      ipPortSiteData = new HashMap<String, SiteData>();
      unresolvedSiteData = new ArrayList<SiteData>();
      hostNameResolver = new HostNameResolver();
      new Thread(hostNameResolver, "hostNameResolver").start();
      mListenerStarted = true;
    }catch (Exception ex){
      ex.printStackTrace();
    }
  }

  private void stopListenerForEvents(){
    if (hostNameResolver != null){
      hostNameResolver.stop();
    }
    mListenerStarted = false;
  }

  public SiteData getSecureHost(Socket socket, ConnectionDescriptor descriptor, boolean _getCertificateData) {
    SiteData secureHost = null;
    int port =  socket.getPort();
    int localport =  socket.getLocalPort();
    if (LOGD) Logger.d(TAG, "Search site for port " + port + " local:" + localport);
    SiteData secureHostInit = parseData(socket, descriptor);
    if (!_getCertificateData){
      return secureHostInit;
    }
    getCertificateData(secureHostInit);
    if (siteData.size() == 0 || !siteData.containsKey(port)){
      try {
        for(int i=0; i < 500; i++){
          Thread.sleep(100);
          if (siteData.containsKey(port)){
            secureHost = siteData.get(port);
            break;
          }
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    if (secureHost == null && siteData.containsKey(port)){
      secureHost = siteData.get(port);
    }
    if (secureHost == null && mHostName != null && mHostName.length() > 0){
      secureHost =  new SiteData();
      secureHost.name = mHostName;
    }
    if (secureHost == null){
      if (LOGD) Logger.d(TAG, "Nothing found for site for port " + port);
      return secureHostInit;
    }else{
      if (LOGD) Logger.d(TAG, "Having site for port " + port + " "
        +  secureHost.name + " addr: "
        + secureHost.tcpAddress
        + " port " + secureHost.destPort);
    }
    return secureHost;
  }

}
