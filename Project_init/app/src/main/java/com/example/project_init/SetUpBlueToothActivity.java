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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class SetUpBlueToothActivity extends AppCompatActivity {

    Button sender, receiver;
    private BluetoothAdapter blueAdapt;
    private String path;
    private String deviceToConnect;
    private BluetoothFileTransfer service;

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
        service = new BluetoothFileTransfer();
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


}

