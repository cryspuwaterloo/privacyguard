package ca.uwaterloo.crysp.privacyguard.Application.Database;

/**
 * Created by rongjunyan on 2017-08-08.
 */

public class Traffic {
    private String appName;
    private String destIP;
    private boolean encrypted;
    private int size;
    private boolean outgoing;

    public Traffic(String appName, String destIP, boolean encrypted, int size, boolean outgoing){
        this.appName = appName;
        this.destIP = destIP;
        this.encrypted = encrypted;
        this.size = size;
        this.outgoing = outgoing;
    }

    public String getAppName(){
        return appName;
    }

    public String getDestIP(){
        return destIP;
    }

    public boolean isEncrypted(){
        return encrypted;
    }

    public int getSize(){
        return size;
    }

    public boolean isOutgoing(){
        return outgoing;
    }
}
