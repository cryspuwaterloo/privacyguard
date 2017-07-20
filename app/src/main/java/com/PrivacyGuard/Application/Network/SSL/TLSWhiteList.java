package com.PrivacyGuard.Application.Network.SSL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;

import com.PrivacyGuard.Application.Logger;

public class TLSWhiteList {
    /* number of consecutive failed SSL interception attempts we need to see until we decide that a site uses
       SSL pinning */
    private static int MAX_FAIL = 2;

    /* likelihood used to remove a site from the set of suspected SSL pinning sites to give SSL
       interception another chance  */
    private static int REMOVE_RATE = 10;

    private static final boolean DEBUG = false;
    private static final String TAG = TLSWhiteList.class.getSimpleName();

    private File automaticFile;

    // list of destinations for which PrivacyGuard detects that setting up TLS fails, likely
    // due to certificate pinning
    private Map<String, Integer> automaticList;

    // list of apps for which TLS connection can set up but then there's a failure, likely
    // due to certificate pinning at the application layer
    private Set<String> manualList;
    private Random rand = new Random();

    public TLSWhiteList(File directory, String file){
        automaticList = new HashMap<String, Integer>();
        readfromfile(directory, file);

        manualList = new HashSet<String>();
        /// TODO: make this list user editable
        manualList.add("com.wire");
        manualList.add("com.amazon.mShop.android.shopping");
        manualList.add("org.mozilla.firefox");
        manualList.add("com.android.providers.downloads.ui");
    }

    private void readfromfile(File directory, String file) {
        automaticFile = new File(directory, file);
        try {
            if (automaticFile.exists()) {
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(automaticFile));
                automaticList = (HashMap<String, Integer>) inputStream.readObject();
                inputStream.close();
                if (DEBUG) {
                    for (Map.Entry entry : automaticList.entrySet()) {
                        Logger.d(TAG, " loading suspected SSL pinning site: " + entry.getKey() + " (" + entry.getValue() + ")");
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void writetofile(){
        try{
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(automaticFile));
            outputStream.writeObject(automaticList);
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean contains(String address, String packageName){

        if (manualList.contains(packageName))
            return true;

        if(automaticList.containsKey(address) && automaticList.get(address) >= MAX_FAIL) {
            if(rand.nextInt(100)+1 <= REMOVE_RATE){
                automaticList.remove(address);
                writetofile();
                if (DEBUG) Logger.d(TAG, "Randomly removed " + address + " from set of assumed SSL pinning sites");
                return false;
            }
            if (DEBUG) Logger.d(TAG, address + " is an assumed SSL pinning site");
            return true;
        }else{
            return false;
        }
    }

    public synchronized void add(String address){
        if (DEBUG) Logger.d(TAG, "Adding " + address + " to (candidate) SSL pinning sites (current counter: " + automaticList.get(address) + ")");
        if(automaticList.containsKey(address)){
            if (automaticList.get(address) >= MAX_FAIL)
                return;
            automaticList.put(address, automaticList.get(address) + 1);
        } else{
            automaticList.put(address, 1);
        }
        writetofile();
    }

    public synchronized void remove(String address){
        if(automaticList.containsKey(address)){
            automaticList.remove(address);
            writetofile();
            if (DEBUG) Logger.d(TAG, address + " has been removed from set of SSL pinning sites");
        }
    }
}
