package com.example.project_init;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

// Class that creates connections, performs transfers, etc.
public class BluetoothFileTransfer {

    // UUID for server connection
    private static final UUID MY_UUID =
            UUID.fromString("1c6b6701-2ee1-49e8-bf2f-119366d741a5");

    // Variables
    private final BluetoothAdapter blueAdapt;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;
    private byte[] read;
    private int length;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    // Constructor for class
    public BluetoothFileTransfer() {
        blueAdapt = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
        read = null;
    }

    // Return connection state
    public synchronized int getState(){
        return state;
    }

    // Return read information
    public byte[] getInformation() { return read; }

    // Clear read information
    public int clearInformation() { read = null; return length;}

    // Start the file transfer service
    public synchronized void start(){

        // Kill any unwanted threads
        if(connectThread != null){
            connectThread.cancel();
        }
        if(connectedThread != null){
            connectedThread.cancel();
        }

        // Start a thread to listen on a Bluetooth Socket server
        acceptThread = new AcceptThread();
        acceptThread.start();
        Log.e("????", "Server Thread created");
    }

    // Connect to device acting as server
    public synchronized void connect(BluetoothDevice device){

        // Kill any thread attempting to make connection
        if (state == STATE_CONNECTING){
            if (connectThread != null){
                connectThread.cancel();
                connectThread = null;
            }
        }

        // Kill any thread currently with a connection
        if (connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        // Start process to connect with new device
        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    // Transition to socket connection where communication can start
    public synchronized void connected(BluetoothSocket socket){

        // Kill any thread attempting to make connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Kill any thread currently with a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Kill the accept thread because we only want to connect to one device
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        Log.e("????", "Connection completed");
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    // Allows us to access write
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            r = connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    // The connection failed
    private void connectionFailed() {

        // Start the service over
        state = STATE_NONE;
        BluetoothFileTransfer.this.stop();
    }

    // The connection was lost
    private void connectionLost(){

        // Start the service over
        state = STATE_NONE;
        BluetoothFileTransfer.this.stop();
    }

    // Stop the connection, kill everything
    public synchronized void stop() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
        }

        state = STATE_NONE;
    }


    // This class sets up the server
    private class AcceptThread extends Thread{

        // Local server socket
        private final BluetoothServerSocket serverSock;

        // Local constructor
        public AcceptThread(){
            BluetoothServerSocket temp = null;
            try {
                temp = blueAdapt.listenUsingRfcommWithServiceRecord("Server", MY_UUID);
                Log.e("????", "Successful Listening");
            } catch (Exception e){

            }
            serverSock = temp;
            state = STATE_LISTEN;
        }

        // Hope that another device hears our cries for help
        public void run() {
            BluetoothSocket socket = null;
            while(state != STATE_CONNECTED){
                try{
                    socket = serverSock.accept();
                    Log.e("????", "A socket that connects the two devices");
                } catch(Exception e){
                    Log.e("????", "Socket's accept() method failed", e);
                    break;
                }

                // Transition to new state
                if (socket != null){
                    synchronized (BluetoothFileTransfer.this) {
                        switch(state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:

                                // Start connected thread
                                connected(socket);

                            case STATE_NONE:
                            case STATE_CONNECTED:

                                // Bad, close server
                                cancel();
                        }
                    }
                }
            }
        }

        public void cancel(){
            try {
                serverSock.close();
            } catch (Exception e){

            }
        }

    }

    // This thread sets up the client
    private class ConnectThread extends Thread{

        // Variables
        private final BluetoothSocket clientSock;
        private final BluetoothDevice server;

        // Constructor
        public ConnectThread(BluetoothDevice device){
            server = device;
            BluetoothSocket temp = null;

            // Try to connect with the server
            try {
                temp = server.createRfcommSocketToServiceRecord(MY_UUID);
            }catch (Exception e){
                Log.e("????", "Connection to server failed");
            }

            clientSock = temp;
            state = STATE_CONNECTING;
        }

        public void run(){

            // Attempt connection to bluetooth socket
            try {
                clientSock.connect();
            } catch (Exception e){
                cancel();
                connectionFailed();
                return;
            }

            // Reset connection thread
            synchronized (BluetoothFileTransfer.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(clientSock);

        }

        public void cancel(){
            try {
                clientSock.close();
            }catch (Exception e){

            }
        }
    }

    // General connection class
    private class ConnectedThread extends Thread {

        // Variables
        private final BluetoothSocket sock;
        private final InputStream inStream;
        private final OutputStream outStream;

        // Constructor, type = sender, receiver
        public ConnectedThread(BluetoothSocket socket){
            sock = socket;
            InputStream inTemp = null;
            OutputStream outTemp = null;
            Log.e("????", "Connection Success");

            try{
                inTemp = socket.getInputStream();
                outTemp = socket.getOutputStream();
            } catch (Exception e){
                Log.e("????", "sockets aren't working/created");
            }

            inStream = inTemp;
            outStream = outTemp;
            state = STATE_CONNECTED;

        }

        // This is the reader, where we are always reading
        public void run() {
            length = 4096;
            byte[] buffer = new byte[length];
            int bytes = -1;
            read = null;
            while (state == STATE_CONNECTED) {

                // only read if buffer is empty
                if (read == null) {
                    try {

                        // Read from the InputStream if there's something inside
                        if (inStream.available() > 0) {

                            // Collect bytes until nothing finished reading from buffer
                            //bytes = inStream.read(buffer);
                            //Log.e("????", bytes + "");

                            bytes = inStream.read(buffer);
                            read = Arrays.copyOfRange(buffer, 0, bytes);
                            length = read.length;
                        }


                    } catch (Exception e) {
                        connectionLost();
                    }
                }
            }
        }

        // This is the writer
        public void write(byte[] buffer) {
            try {
                outStream.write(buffer);
            } catch (Exception e) {
                connectionLost();
            }
        }

        public void cancel() {
            try {
                sock.close();
            } catch (Exception e) {

            }
        }
    }
}
