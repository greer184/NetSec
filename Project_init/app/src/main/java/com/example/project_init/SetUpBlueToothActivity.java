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
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.Key;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SetUpBlueToothActivity extends AppCompatActivity {

    Button sender, receiver;
    private BluetoothAdapter blueAdapt;
    private String path;
    private String deviceToConnect;
    private BluetoothFileTransfer service;
    private byte[] bytes;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_up_blue_tooth);

        Intent intent = getIntent();
        path = intent.getExtras().getString("Filename");

        setup();

        sender = (Button) findViewById(R.id.sender);
        receiver = (Button) findViewById(R.id.receiver);
    }

    public void onDestroy() {
        super.onDestroy();
        if (service != null) {
            service.stop();
        }
    }

    // New Client/Server code
    public void server(View v) {

        // Need to ask user for desired name of file
        ensureDiscoverable();

        // Setup as server
        service.start();

        // Waited until state is connected
        while(true){
            if(service.getState() == BluetoothFileTransfer.STATE_CONNECTED){
                break;
            }
        }

        // Generate key part
        DiffieHellman dh = new DiffieHellman();
        service.write(dh.generatePublicKey());

        // Receive other part from client
        while(bytes == null){}
        byte[] keyPart = bytes;
        bytes = null;

        // Build decryption mechanism
        Cipher c = null;
        try {
            Key key = new SecretKeySpec(dh.computeSharedKey(keyPart), "AES");
            c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, key);
        } catch (Exception e){
            Log.e("????", "Issues with key generation");
        }

        // Receive length of file from client
        while(bytes == null){}
        byte[] len = bytes;
        bytes = null;
        int length = ByteBuffer.wrap(len).getInt();

        // Build file until file transfer is complete
        byte[] received = new byte[length];
        while(service.getState() == BluetoothFileTransfer.STATE_CONNECTED) {
            int start = 0;
            if (bytes != null) {
                byte[] n = bytes;
                for (int i = 0; i < 1024; i++){
                    if(start + i < received.length) {
                        received[start + i] = n[i];
                    } else {
                        break;
                    }
                }
                bytes = null;
                start += 1024;
                if (start >= received.length){
                    break;
                }
            }
        }

        // Decode chunks, then recompile file
        try {
            received = c.doFinal(received);
        }catch (Exception e){
            Log.e("????", "Decryption Failed");
        }

        // Convert to File
        File f = new File(path);
        try {
            FileOutputStream fOut = new FileOutputStream(path);
            fOut.write(received);
            fOut.close();
        }  catch (Exception e){
            Log.e("????", "File save didn't work");
        }
    }

    public void client(View v) {

        // Find the server device
        registerReceiver(discoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        // Start discovery process to be found by the server
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter != null && adapter.isDiscovering()){
            adapter.cancelDiscovery();
        }
        adapter.startDiscovery();

        // Get the BluetoothDevice object
        BluetoothDevice device = blueAdapt.getRemoteDevice(deviceToConnect);

        // Attempt to connect
        service.connect(device);

        // Wait for connection
        while(true){
            if(service.getState() == BluetoothFileTransfer.STATE_CONNECTED){
                break;
            }
        }

        // Generate key part
        DiffieHellman dh = new DiffieHellman();
        service.write(dh.generatePublicKey());

        // Receive other part from client
        while(bytes == null){}
        byte[] keyPart = bytes;
        bytes = null;

        // Build encryption mechanism
        Cipher c = null;
        try {
            Key key = new SecretKeySpec(dh.computeSharedKey(keyPart), "AES");
            c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key);
        } catch (Exception e){
            Log.e("????", "Issues with key generation");
        }

        // Convert file to byte array
        File f = new File(path);
        byte[] byteFile = new byte[(int) f.length()];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(f));
            buf.read(byteFile, 0, byteFile.length);
            buf.close();
        } catch (Exception e) {
            Log.e("????", "File not read correctly");
        }

        try {
            byteFile = c.doFinal(byteFile);
        }catch (Exception e){
            Log.e("????", "Encryption failed");
        }

        // Convert file length and sent away
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(byteFile.length);
        byte[] len = b.array();
        service.write(len);

        // Send in 1024 byte chunks
        int start = 0;
        boolean finished = false;
        while(service.getState() == BluetoothFileTransfer.STATE_CONNECTED) {
            byte[] toSend = new byte[1024];
            for (int i = 0; i < 1024; i++){
                if (start + i < byteFile.length) {
                    toSend[i] = byteFile[start + i];
                } else {
                    break;
                }
            }

            // Send chunk to server
            service.write(toSend);
            start += 1024;

            // If finished, exit loop
            if (start >= byteFile.length){
                break;
            }

        }

        // Close connection now we are done
        service.stop();
    }

    private void ensureDiscoverable() {
        if (blueAdapt.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    public void setup(){
        blueAdapt = BluetoothAdapter.getDefaultAdapter();
        service = new BluetoothFileTransfer(blueHandler);
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver discoveryResult = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // MAC Address of the device
                deviceToConnect = device.getAddress();
            }
        }
    };

    private final Handler blueHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    break;
                case MESSAGE_WRITE:
                    break;
                case MESSAGE_READ:
                    bytes = (byte[]) msg.obj;
                    break;
                case MESSAGE_DEVICE_NAME:
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("Toast"),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


}

