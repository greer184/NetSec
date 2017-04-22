package com.example.project_init;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
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
    private int newState;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    // Constructor for class
    public BluetoothFileTransfer() {
        blueAdapt = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
        newState = state;
    }

    // Return connection state
    public synchronized int getState(){
        return state;
    }

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
    public synchronized void connected(BluetoothSocket socket, String type){

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

        connectedThread = new ConnectedThread(socket, type);
        connectedThread.start();
    }

    // The connection failed
    private void connectionFailed() {

        // Start the service over to restart listening mode
        state = STATE_NONE;
        BluetoothFileTransfer.this.start();
    }

    // The connection was lost
    private void connectionLost(){

        // Start the service over to restart listening mode
        state = STATE_NONE;
        BluetoothFileTransfer.this.start();
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
                                connected(socket, "Server");

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
                temp = device.createRfcommSocketToServiceRecord(MY_UUID);
            }catch (Exception e){
                Log.e("????", "Connection to server failed");
            }

            clientSock = temp;
            state = STATE_CONNECTING;
        }

        public void run(){

            // Kill discovery, to speed things up
            blueAdapt.cancelDiscovery();

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
            connected(clientSock, "Client");

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
        private final String serviceType;

        // Constructor, type = sender, receiver
        public ConnectedThread(BluetoothSocket socket, String type){
            sock = socket;
            serviceType = type;
            InputStream inTemp = null;
            OutputStream outTemp = null;

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

        // This is the reader
        public void run() {

        }

        // This is the writer
        public void write() {

        }

        public void cancel() {
            try {
                sock.close();
            } catch (Exception e) {

            }
        }
    }
}
