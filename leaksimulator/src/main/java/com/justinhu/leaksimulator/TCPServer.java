package com.justinhu.leaksimulator;

/**
 * Created by justinhu on 2017-02-10.
 *
 * Copied from https://www.myandroidsolutions.com/2012/07/20/android-tcp-connection-tutorial/#.WJ4fMRIrKRs
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The class extends the Thread class so we can receive and send messages at the same time
 */
public class TCPServer extends Thread {

    private static final int SERVERPORT = 4444;
    private boolean running = false;

    public TCPServer() {

    }

    @Override
    public void run() {
        super.run();

        running = true;

        try {
            System.out.println("S: Connecting...");

            //create a server socket. A server socket waits for requests to come in over the network.
            ServerSocket serverSocket = new ServerSocket(SERVERPORT);

            while (running) {
            //create client socket... the method accept() listens for a connection to be made to this socket and accepts it.
            Socket client = serverSocket.accept();
            System.out.println("S: Receiving...");

            try {
                //read the message received from client
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                //in this while we wait to receive messages from client (it's an infinite loop)
                //this while it's like a listener for messages

                    String message = in.readLine();
                    if(message!=null)
                        System.out.println(message);


            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    client.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void terminate() {
        running = false;
    }

}
