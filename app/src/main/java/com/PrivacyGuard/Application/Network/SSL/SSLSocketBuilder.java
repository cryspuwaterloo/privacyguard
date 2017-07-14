package com.PrivacyGuard.Application.Network.SSL;

import com.PrivacyGuard.Application.Logger;

import org.sandrop.webscarab.plugin.proxy.SSLSocketFactoryFactory;
import org.sandrop.webscarab.plugin.proxy.SiteData;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SSLSocketBuilder {
    private static final String TAG = SSLSocketBuilder.class.getSimpleName();
    private static boolean DEBUG = false;
    private static String[] wiresharkSupportedCiphers = new String[]
            {
                    "TLS_RSA_WITH_NULL_MD5",
                    "TLS_RSA_WITH_NULL_SHA",
                    "TLS_RSA_EXPORT_WITH_RC4_40_MD5",
                    "TLS_RSA_WITH_RC4_128_MD5",
                    "TLS_RSA_WITH_RC4_128_SHA",
                    "TLS_RSA_EXPORT_WITH_RC2_CBC_40_MD5",
                    "TLS_RSA_WITH_IDEA_CBC_SHA",
                    "TLS_RSA_EXPORT_WITH_DES40_CBC_SHA",
                    "TLS_RSA_WITH_DES_CBC_SHA",
                    "TLS_RSA_WITH_3DES_EDE_CBC_SHA",
                    "TLS_RSA_WITH_AES_128_CBC_SHA", // 47
                    "TLS_RSA_WITH_AES_256_CBC_SHA", // 53
                    "TLS_RSA_WITH_NULL_SHA256", // 59
                    "TLS_RSA_WITH_AES_128_CBC_SHA256", // 60
                    "TLS_RSA_WITH_AES_256_CBC_SHA256", // 61
                    "TLS_RSA_EXPORT1024_WITH_DES_CBC_SHA", //98
            };

    private static List<String> listWiresharkSupportedCiphers = Arrays.asList(wiresharkSupportedCiphers);
    private static String[] selectedCiphers = null;

    public static String[] selectCiphers(String[] supportedCiphers){
        if (selectedCiphers == null){
            List<String> listSelectedCiphers = new ArrayList<String>();
            for (String supportedCipher : supportedCiphers) {
                if (listWiresharkSupportedCiphers.contains(supportedCipher)){
                    listSelectedCiphers.add(supportedCipher);
                }
            }
            if (listSelectedCiphers.size() == 0){
                String msg = "!!!Error Cipher list is empty";
                Logger.e(TAG, msg);
            }
            Collections.reverse(listSelectedCiphers);
            selectedCiphers = new String[listSelectedCiphers.size()];
            for (int i = 0; i < selectedCiphers.length; i++) {
                String selectedCipher = listSelectedCiphers.get(i);
                selectedCiphers[i] = selectedCipher;
            }
            return selectedCiphers;
        }else{
            return selectedCiphers;
        }
    }

    public static SSLSocket negotiateSSL(
            Socket sock, SiteData hostData,boolean useOnlyWiresharkDissCiphers,
            SSLSocketFactoryFactory sslSocketFactoryFactory)
            throws Exception {
        String certEntry = hostData.tcpAddress != null ? hostData.tcpAddress + "_" + hostData.destPort: hostData.name;
        if(DEBUG) Logger.d(TAG, certEntry);
        SSLSocketFactory factory = sslSocketFactoryFactory.getSocketFactory(hostData);
        if (factory == null)
            throw new RuntimeException(
                    "SSL Intercept not available - no keystores available");
        SSLSocket sslsock;
        try {
            int sockPort = sock.getPort();
            String hostName = hostData.tcpAddress != null ? hostData.tcpAddress : hostData.name;
            sslsock = (SSLSocket) factory.createSocket(sock, hostName, sockPort, true);
            if (useOnlyWiresharkDissCiphers){
                String[] ciphers = selectCiphers(sslsock.getSupportedCipherSuites());
                sslsock.setEnabledCipherSuites(ciphers);
            }
            sslsock.setUseClientMode(false);
            return sslsock;
        } catch (Exception e) {
            throw e;
        }
    }
}
