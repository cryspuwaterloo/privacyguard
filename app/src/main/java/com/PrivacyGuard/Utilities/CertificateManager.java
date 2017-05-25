package com.PrivacyGuard.Utilities;

import android.content.Intent;
import android.security.KeyChain;

import com.PrivacyGuard.Application.Helpers.ActivityRequestCodes;
import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.Network.FakeVPN.MyVpnService;

import org.sandrop.webscarab.plugin.proxy.SSLSocketFactoryFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.util.Date;
import java.util.Enumeration;

import javax.security.cert.Certificate;
import javax.security.cert.CertificateEncodingException;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

/**
 * Created by Near on 15-01-06.
 */
public class CertificateManager {
    private static String TAG = "CertificateManager";
    static SSLSocketFactoryFactory mFactoryFactory;

    // generate CA certificate but return a ssl socket factory factory which use this certificate
    public static SSLSocketFactoryFactory initiateFactory(String dir, String caName, String certName, String KeyType, char[] password) {
        if (mFactoryFactory == null) {
            try {
                mFactoryFactory = new SSLSocketFactoryFactory(dir + "/" + caName, dir + "/" + certName, KeyType, password);
            } catch (GeneralSecurityException | IOException e) {
                Logger.e(TAG, "Error initiating SSLSocketFactoryFactory", e);
            }
        }
        return mFactoryFactory;
    }

    // do we already have a trusted credential for our fake root CA?
    public static boolean fakeRootCAIsTrusted() {
        try
        {
            KeyStore ks = KeyStore.getInstance("AndroidCAStore");
            if (ks != null)
            {
                ks.load(null, null);
                Enumeration aliases = ks.aliases();
                while (aliases.hasMoreElements())
                {
                    String alias = (String) aliases.nextElement();
                    java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) ks.getCertificate(alias);

                    if (cert.getIssuerDN().getName().contains(MyVpnService.CAName))
                    {
                        Logger.d(TAG, "Fake root CA is already trusted");
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (java.security.cert.CertificateException e) {
            e.printStackTrace();
        }
        Logger.d(TAG, "Fake root CA is not yet trusted");
        return false;
    }

    // have certificate for fake root CA become a trusted credential
    public static Intent trustfakeRootCA() {
        //boolean certInstalled = CertificateManager.isCACertificateInstalled(MyVpnService.CADir, MyVpnService.CAName, MyVpnService.KeyType, MyVpnService.Password.toCharArray());

        // XXX: check whether we have certificate in KeyChain, is it the same as trust manager

        //if (keyChainInstalled && certInstalled) {
        //    return;
        //}
        //if (!certInstalled) {
        //    CertificateManager.initiateFactory(MyVpnService.CADir, MyVpnService.CAName, MyVpnService.CertName, MyVpnService.KeyType, MyVpnService.Password.toCharArray());
        //}
        Logger.d(TAG, "Trusting fake root CA");
        Intent intent = KeyChain.createInstallIntent();
        try {
            Certificate cert = getCACertificate(MyVpnService.CADir, MyVpnService.CAName);
            if (cert != null) {
                intent.putExtra(KeyChain.EXTRA_CERTIFICATE, cert.getEncoded());
                intent.putExtra(KeyChain.EXTRA_NAME, MyVpnService.CAName);
                return intent;
            }
        } catch (CertificateEncodingException e) {
            Logger.e(TAG, "Certificate Encoding Error", e);
        }
        return null;
    }

    /*public static boolean isCACertificateInstalled(String dir, String caName, String type, char[] password) {
        File fileCA = new File(dir + "/" + caName);
        if (!fileCA.exists() || !fileCA.canRead()) {
            Logger.d(TAG, "CA file invalid");
            return false;
        }

        KeyStore keyStoreCA = null;
        try {
            keyStoreCA = KeyStore.getInstance(type, "BC");
        } catch (KeyStoreException e) {
            Logger.e(TAG, "KeyStore Error", e);
        } catch (NoSuchProviderException e) {
            Logger.e(TAG, "Provider Error", e);
        }

        if (keyStoreCA == null) {
            return false;
        }
        FileInputStream fileCert = null;
        try {
            fileCert = new FileInputStream(fileCA);
            keyStoreCA.load(fileCert, password);

            Enumeration ex = keyStoreCA.aliases();
            Date exportFilename = null;
            String caAliasValue = "";

            while (ex.hasMoreElements()) {
                String is = (String) ex.nextElement();
                Date lastStoredDate = keyStoreCA.getCreationDate(is);
                if (exportFilename == null || lastStoredDate.after(exportFilename)) {// to get latest cert?
                    exportFilename = lastStoredDate;
                    caAliasValue = is;
                }
            }

            return keyStoreCA.getKey(caAliasValue, password) != null;

        } catch (NoSuchAlgorithmException e) {
            Logger.e(TAG, "Algorithm Error", e);
        } catch (IOException e) {
            Logger.e(TAG, "IO Error", e);
        } catch (java.security.cert.CertificateException e) {
            Logger.e(TAG, "Certificate Error", e);
        } catch (UnrecoverableKeyException e) {
            Logger.e(TAG, "Key cannot be recovered", e);
        } catch (KeyStoreException e) {
            Logger.e(TAG, "Key Store not initialized", e);
        } finally {
            if (fileCert != null) {
                try {
                    fileCert.close();
                } catch (IOException e) {
                }
            }
        }


        return false;
    }*/

    // get the CA certificate by the path
    private static X509Certificate getCACertificate(String dir, String caName) {
        String CERT_FILE = dir + "/" + caName + "_export.crt";
        File certFile = new File(CERT_FILE);
        FileInputStream certIs = null;
        try {
            certIs = new FileInputStream(CERT_FILE);
            byte[] cert = new byte[(int) certFile.length()];
            certIs.read(cert);
            return X509Certificate.getInstance(cert);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } finally {
            if (certIs != null) {
                try {
                    certIs.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}
