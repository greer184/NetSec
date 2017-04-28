package com.example.project_init;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SetUpBlueToothActivity extends AppCompatActivity implements
        AdapterView.OnItemSelectedListener {

    Button sender, receiver, pair;
    private BluetoothAdapter blueAdapt;
    private String path;
    private String name;
    private List<String> Names;
    private List<String> MACs;
    private String deviceToConnect;
    private BluetoothFileTransfer service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_up_blue_tooth);

        Intent intent = getIntent();
        path = intent.getExtras().getString("Filename");
        name = intent.getExtras().getString("Name");

        blueAdapt = BluetoothAdapter.getDefaultAdapter();

        deviceToConnect = null;

        sender = (Button) findViewById(R.id.sender);
        receiver = (Button) findViewById(R.id.receiver);
        pair = (Button) findViewById(R.id.find);

        Names = new ArrayList<String>();
        MACs = new ArrayList<String>();

        // Build registers
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(myReceiver, filter);

    }

    public void onDestroy() {
        super.onDestroy();
        if (service != null) {
            service.stop();
        }
        unregisterReceiver(myReceiver);
    }

    // New Client/Server code
    public void server(View v) {

        // Setup as server
        service = new BluetoothFileTransfer();
        service.start();

        // Make device discoverable
        ensureDiscoverable();

        // Waited until state is connected
        while(true){
            if(service.getState() == BluetoothFileTransfer.STATE_CONNECTED){
                Log.d("????", "Connection Completed");
                break;
            }
        }

        // Generate key part
        DiffieHellman dh = new DiffieHellman();
        Log.d("????", "Diffie Hellman Started");

        // Receive other part from client
        byte[] keyPart = null;
        while(true){
            if(service.getInformation() != null){
                keyPart = service.getInformation();
                service.clearInformation();
                break;
            }
        }

        // Send key part
        Log.d("????", "Received KeyPart Information");
        byte[] keyz = dh.generatePublicKey();
        service.write(keyz);

        // Build decryption mechanism
        Cipher c = null;
        try {
            byte[] shared = dh.computeSharedKey(keyPart, 192);
            Key key = new SecretKeySpec(shared, 0, shared.length, "AES");
            c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, key);
        } catch (Exception e){
            Log.e("????", e.toString());
        }

        // Receive length of filename from client
        byte[] len = null;
        while(true){
            if(service.getInformation() != null){
                len = service.getInformation();
                service.clearInformation();
                break;
            }
        }
        int length = ByteBuffer.wrap(len).getInt();
        Log.d("????", "File Length: " + length + "");


        // Receive length of file from client
        byte[] nameLen = null;
        while(true){
            if(service.getInformation() != null){
                nameLen = service.getInformation();
                service.clearInformation();
                break;
            }
        }
        int nameLength = ByteBuffer.wrap(nameLen).getInt();
        Log.d("????", "Filename length: " + nameLength + "");

        // Receive name of file from client
        byte[] fileName = null;
        while(true){
            if(service.getInformation() != null){
                fileName = service.getInformation();
                service.clearInformation();
                break;
            }
        }
        String fName = null;
        try {
            fileName = Arrays.copyOfRange(fileName, 0, nameLength);
            fName = new String(fileName);
            Log.d("????", "File: " + fName);
        } catch(Exception e) {
            Log.e("????", "Issue retrieving filename");
            service.stop();
        }

        // Build file until file transfer is complete
        byte[] received = new byte[length];
        int start = 0;
        while(service.getState() == BluetoothFileTransfer.STATE_CONNECTED) {
            if (service.getInformation() != null) {
                byte[] n = service.getInformation();
                int actual = service.clearInformation();
                for (int i = 0; i < actual; i++){
                    if(start + i < received.length) {
                        received[start + i] = n[i];
                    }
                }
                start += actual;
                Log.d("????", "Bytes: " + start + "/" + received.length);

                if (start >= received.length){
                    break;
                }
            }
        }

        // Decode compiled file
        try {
            received = c.doFinal(received);
        }catch (Exception e){
            Log.e("????", "Decryption Failed");
            service.stop();
        }

        // Convert to File
        FileOutputStream fOut;
        String fPath = Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_DOWNLOADS).toString() + "/" + fName;
        try {

            // Write to file
            fOut = new FileOutputStream(new File(fPath));
            fOut.write(received);
            fOut.close();
            Toast.makeText(getApplicationContext(), "File Received: " + fName,
                    Toast.LENGTH_LONG).show();

        }  catch (Exception e){
            Log.e("????", "File save didn't work: " + e.toString());
            service.stop();
        }

        Log.d("????", "Complete");
    }

    public void client(View v) {

        // Get the BluetoothDevice object
        Log.d("????", "Ready to connect");
        BluetoothDevice device = blueAdapt.getRemoteDevice(deviceToConnect);

        // Attempt to connect
        service = new BluetoothFileTransfer();
        service.connect(device);

        // Wait for connection
        while(true){
            if(service.getState() == BluetoothFileTransfer.STATE_CONNECTED){
                Log.d("????", "Connection Completed");
                break;
            }
        }

        // Generate key part
        Log.d("????", "Diffie Hellman Started");
        DiffieHellman dh = new DiffieHellman();
        byte[] keyz = dh.generatePublicKey();
        service.write(keyz);

        // Receive other part from client
        byte[] keyPart = null;
        while(true){
            if(service.getInformation() != null){
                keyPart = service.getInformation();
                service.clearInformation();
                break;
            }
        }

        Log.d("????", "Diffie Hellman KeyPart Received");

        // Build encryption mechanism
        Cipher c = null;
        try {
            byte[] shared = dh.computeSharedKey(keyPart, 192);
            Key key = new SecretKeySpec(shared, 0, shared.length, "AES");
            c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key);
        } catch (Exception e){
            Log.e("????", "Issues with key generation");
            service.stop();
        }
        Log.d("????", "Symmetric Key Generated");


        // Convert file to byte array
        File f = new File(path);
        byte[] byteFile = new byte[(int) f.length()];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(f));
            buf.read(byteFile, 0, byteFile.length);
            buf.close();
        } catch (Exception e) {
            Log.e("????", "File not read correctly");
            service.stop();
        }

        try {
            byteFile = c.doFinal(byteFile);
        }catch (Exception e){
            Log.e("????", "Encryption failed");
            service.stop();
        }

        Log.d("????", "Encryption worked");

        // Convert file length and send away
        Log.d("????", "File length: " + byteFile.length + "");
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(byteFile.length);
        byte[] len = b.array();
        service.write(len);

        // Convert file path and send away
        try {
            byte[] fileName = name.getBytes("UTF-8");
            Log.d("????", "File: " + name);
            b = ByteBuffer.allocate(4);
            Log.d("????", "Filename Length: " + fileName.length + "");
            b.putInt(fileName.length);
            len = b.array();

            // Length of name
            service.write(len);
            Thread.sleep(50);

            // Filename
            service.write(fileName);
            Thread.sleep(50);

        } catch (Exception e){
            Log.e("????", "Filename transfer issue");
            service.stop();
        }

        // Send file to server
        Log.d("????", "Sending File");
        service.write(byteFile);

        Log.d("????", "Everything works on this side");

        // Close connection now we are done
        Toast.makeText(getApplicationContext(), "File Transfer Complete",
                Toast.LENGTH_LONG).show();
        service.stop();
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private BroadcastReceiver myReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {

                    // Get MAC Address and name of device
                    Log.d("????", "Found Device");
                    if (device.getName() != null) {
                        Names.add(device.getName());
                    } else {
                        Names.add("Unnamed Device");
                    }
                    MACs.add(device.getAddress());
                }

            }
        }
    };

    private void ensureDiscoverable() {
        if (blueAdapt.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    public void discovery(View v){

        // Set device to connect to be null
        deviceToConnect = null;

        // Start discovery process to be found by the server
        BluetoothAdapter blueAdapt = BluetoothAdapter.getDefaultAdapter();
        if(blueAdapt != null && blueAdapt.isDiscovering()){
            blueAdapt.cancelDiscovery();
        }

        // Start discovering devices
        blueAdapt.startDiscovery();

        // If there are already paired devices, get information from here
        Set<BluetoothDevice> paired = blueAdapt.getBondedDevices();
        if (paired.size() > 0) {
            for (BluetoothDevice device : paired) {
                if (device.getName() != null) {
                    Names.add(device.getName());
                } else {
                    Names.add("Unnamed Device");
                }
                MACs.add(device.getAddress());
                break;
            }
        }

        // Start Discovery for other devices
        Log.d("????", "Reached discovery");

        // Sleep for 12 Seconds
        try {
            Thread.sleep(12000);
        } catch (Exception e){

        }

        // Discovery is over
        blueAdapt.cancelDiscovery();
        Log.d("????", "Ended discovery");

        // Create a spinner to select MAC Address
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, Names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        deviceToConnect = MACs.get(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}

