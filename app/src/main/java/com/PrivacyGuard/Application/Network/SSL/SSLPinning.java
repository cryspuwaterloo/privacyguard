package com.PrivacyGuard.Application.Network.SSL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.PrivacyGuard.Application.Logger;

public class SSLPinning {
    /* number of consecutive failed SSL interception attempts we need to see until we decide that a site uses
       SSL pinning */
    private static int MAX_FAIL = 2;

    /* likelihood used to remove a site from the set of suspected SSL pinning sites to give SSL
       interception another chance  */
    private static int REMOVE_RATE = 5;

    private static final boolean DEBUG = false;
    private static final String TAG = SSLPinning.class.getSimpleName();

    private File failureFile;
    private Map<String, Integer> addresslist;
    private Random rand = new Random();

    public SSLPinning(File directory, String file){
        addresslist = new HashMap<String, Integer>();
        readfromfile(directory, file);
    }

    private void readfromfile(File directory, String file) {
        failureFile = new File(directory, file);
        try {
            if (failureFile.exists()) {
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(failureFile));
                addresslist = (HashMap<String, Integer>) inputStream.readObject();
                inputStream.close();
                if (DEBUG) {
                    for (Map.Entry entry : addresslist.entrySet()) {
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
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(failureFile));
            outputStream.writeObject(addresslist);
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean contains(String address){
        if(addresslist.containsKey(address) && addresslist.get(address) >= MAX_FAIL) {
            if(rand.nextInt(100)+1 <= REMOVE_RATE){
                addresslist.remove(address);
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
        if(addresslist.containsKey(address)){
            addresslist.put(address, addresslist.get(address) + 1);
        } else{
            addresslist.put(address, 1);
        }
        writetofile();
        if (DEBUG) Logger.d(TAG, "Adding " + address + " to (candidate) SSL pinning sites (current counter: " + addresslist.get(address) + ")");
    }

    public synchronized void remove(String address){
        if(addresslist.containsKey(address)){
            addresslist.remove(address);
            writetofile();
            if (DEBUG) Logger.d(TAG, address + " has been removed from set of SSL pinning sites");
        }
    }
}
