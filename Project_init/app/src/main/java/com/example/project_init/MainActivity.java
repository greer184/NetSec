package com.example.project_init;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.AdapterView;
import android.widget.Button;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class MainActivity extends Activity {
    Button b1;
    private BluetoothAdapter blueAdapt;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(blueAdapt == null){
            Toast.makeText(getApplicationContext(), "Unable to use Bluetooth", Toast.LENGTH_LONG).show();
        } else {

            // Turn on Bluetooth if off
            if (!blueAdapt.isEnabled()) {
                Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnOn, 0);
                Toast.makeText(getApplicationContext(),
                        "Bluetooth enabled", Toast.LENGTH_LONG).show();
            }

        }

        // Set button to allow Bluetooth connection
        b1 = (Button) findViewById(R.id.buttonD);
    }

    // Bluetooth code for discovering and pairing Bluetooth devices
    public void discovery(View v){

        // Setup adapter
        blueAdapt = BluetoothAdapter.getDefaultAdapter();

        // Stop any attempts at discovery
        if (blueAdapt.isDiscovering()){
            blueAdapt.cancelDiscovery();
        }

        // If there are already paired devices, get information from here
        Set<BluetoothDevice> paired = blueAdapt.getBondedDevices();

        if (paired.size() > 0) {
            for (BluetoothDevice device : paired) {
                String stuff = device.getName() + "\n" + device.getAddress();
                Toast.makeText(getApplicationContext(), stuff, Toast.LENGTH_LONG).show();
            }
        } else {

            // Start fresh discovery
            blueAdapt.startDiscovery();

            // Register for broadcasts when a device is discovered.
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // End discovery process
        if (blueAdapt != null) {
            blueAdapt.cancelDiscovery();
        }

        // Clear listeners
        this.unregisterReceiver(mReceiver);
    }

    private AdapterView.OnItemClickListener deviceListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            // Cancel discovery because we are connecting
            blueAdapt.cancelDiscovery();

            // Get the device MAC address from the view
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    // The BroadcastReceiver that listens for discovered devices
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    String stuff = device.getName() + "\n" + device.getAddress();
                    Toast.makeText(getApplicationContext(), stuff, Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    public class BluetoothFileTransfer {
        private static final String TAG = "MY_APP_DEBUG_TAG";
        private Handler mHandler; // handler that gets info from Bluetooth service

        // Defines several constants used when transmitting messages between the
        // service and the UI.
        private interface MessageConstants {
            public static final int MESSAGE_READ = 0;
            public static final int MESSAGE_WRITE = 1;
        }

        private class ConnectedThread extends Thread {
            private final BluetoothSocket mmSocket;
            private final InputStream mmInStream;
            private final OutputStream mmOutStream;
            private byte[] mmBuffer; // mmBuffer store for the stream

            public ConnectedThread(BluetoothSocket socket) {
                mmSocket = socket;
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                // Get the input and output streams
                try {
                    tmpIn = socket.getInputStream();
                } catch (IOException e) {

                }
                try {
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) {

                }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;
            }

            public void run() {
                mmBuffer = new byte[1024];
                int numBytes; // bytes returned from read()

                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    try {
                        // Read from the InputStream.
                        numBytes = mmInStream.read(mmBuffer);

                        // Send the obtained bytes to the UI activity.
                        Message readMsg = mHandler.obtainMessage(com.example.project_init.BluetoothFileTransfer.MessageConstants.MESSAGE_READ,
                                numBytes, -1, mmBuffer);
                        readMsg.sendToTarget();

                    } catch (IOException e) {
                        break;
                    }
                }
            }

            // Call this from the main activity to send data to the remote device.
            public void write(byte[] bytes) {
                try {
                    mmOutStream.write(bytes);

                    // Share the sent message with the UI activity.
                    Message writtenMsg = mHandler.obtainMessage(com.example.project_init.BluetoothFileTransfer.MessageConstants.MESSAGE_WRITE,
                            -1, -1, mmBuffer);
                    writtenMsg.sendToTarget();
                } catch (IOException e) {

                }
            }

            // Call this method from the main activity to shut down the connection.
            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) {

                }
            }
        }

        private class ServerThread extends Thread {
            private final BluetoothServerSocket mmServerSocket;

            public ServerThread() {
                // Use a temporary object that is later assigned to mmServerSocket
                // because mmServerSocket is final.
                BluetoothServerSocket tmp = null;
                try {
                    // MY_UUID is the app's UUID string, also used by the client code.
                    tmp = blueAdapt.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                } catch (IOException e) {

                }
                mmServerSocket = tmp;
            }

            public void run() {
                BluetoothSocket socket = null;
                // Keep listening until exception occurs or a socket is returned.
                while (true) {
                    try {
                        socket = mmServerSocket.accept();
                    } catch (IOException e) {
                        break;
                    }

                    if (socket != null) {
                        // A connection was accepted. Perform work associated with
                        // the connection in a separate thread.
                        BluetoothFileTransfer.ConnectedThread(new );
                        cancel();
                        break;
                    }
                }
            }

            // Closes the connect socket and causes the thread to finish.
            public void cancel() {
                try {
                    mmServerSocket.close();
                } catch (IOException e) {

                }
            }
        }

        private class ClientThread extends Thread {
            private final BluetoothSocket mmSocket;
            private final BluetoothDevice mmDevice;

            public ClientThread(BluetoothDevice device) {
                // Use a temporary object that is later assigned to mmSocket
                // because mmSocket is final.
                BluetoothSocket tmp = null;
                mmDevice = device;

                try {
                    // Get a BluetoothSocket to connect with the given BluetoothDevice.
                    // MY_UUID is the app's UUID string, also used in the server code.
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e) {

                }
                mmSocket = tmp;
            }

            public void run() {

                // Cancel discovery because it otherwise slows down the connection.
                blueAdapt.cancelDiscovery();

                try {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    mmSocket.connect();
                } catch (IOException connectException) {

                    // Unable to connect; close the socket and return.
                    try {
                        mmSocket.close();
                    } catch (IOException closeException) {

                    }
                    return;
                }

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                BluetoothFileTransfer.ConnectedThread(mmSocket);
            }

            // Closes the client socket and causes the thread to finish.
            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) {

                }
            }
        }
    }
}
