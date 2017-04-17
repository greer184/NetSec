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
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_up_blue_tooth);

        Intent intent = getIntent();

        sender = (Button) findViewById(R.id.sender);
        receiver = (Button) findViewById(R.id.receiver);
    }

    // New Client/Server code
    public void server(View v){
        Intent intent = new Intent(this, BluetoothServerActivity.class);
        startActivity(intent);
    }

    public void client(View v){
        Intent intent = new Intent(this, BluetoothClientActivity.class);
        startActivity(intent);
    }

    // Old discovery code, this could be useful for a display later down the road
    // Right now, leave for testing purposes
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

}
