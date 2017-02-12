package com.justinhu.leaksimulator;

/**
 * Created by justinhu on 2017-02-10.
 * Copied from https://www.myandroidsolutions.com/2012/07/20/android-tcp-connection-tutorial/#.WJ4fMRIrKRs
 */

import android.os.AsyncTask;
import android.util.Log;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient extends AsyncTask<String,Void,Void>{

    private String serverMessage;
    public static final String SERVERIP = "192.168.2.132"; //host machine
    public static final int SERVERPORT = 4444;
    private boolean mRun = false;

    PrintWriter out;
    Socket socket;

    public TCPClient() {


    }

    @Override
    protected Void doInBackground(String... params) {
        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(SERVERIP);

            Log.e("TCP Client", "C: Connecting...");

            //create a socket to make the connection with the server
            socket= new Socket(serverAddr, SERVERPORT);
            //send the message to the server

            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            if (!out.checkError()) {
                out.println(params[0]);
                out.flush();
            }
            out.close();

            Log.e("TCP Client", "C: Sent:"+params[0]);

            socket.close();

        } catch (Exception e) {

            Log.e("TCP", "C: Error", e);

        }
        return null;
    }


}
