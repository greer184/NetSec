package com.example.project_init;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Toast;

public class BluetoothClientActivity extends Activity {

    private boolean ACTIVE = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_client);
        registerReceiver(discoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter != null && adapter.isDiscovering()){
            adapter.cancelDiscovery();
        }
        adapter.startDiscovery();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(discoveryResult);
        } catch(Exception e) {
            e.printStackTrace();
        }
        if(socket != null){
            try{
                inStream.close();
                outStream.close();
                socket.close();

                ACTIVE = false;
            }catch(Exception e){

            }
        }
    }

    private BluetoothSocket socket;
    private OutputStreamWriter outStream;
    private InputStream inStream;
    private BluetoothDevice remoteDevice;
    private BroadcastReceiver discoveryResult = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterReceiver(this);
            remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            new Thread(reader).start();
        }
    };

    private Runnable reader = new Runnable() {

        @Override
        public void run() {
            try {
                UUID uuid = UUID.fromString("1c6b6701-2ee1-49e8-bf2f-119366d741a5");
                socket = remoteDevice.createRfcommSocketToServiceRecord(uuid);
                socket.connect();

                outStream = new OutputStreamWriter(socket.getOutputStream());
                inStream = socket.getInputStream();

                new Thread(writer).start();

                int bufferSize = 1024;
                int bytesRead = -1;
                byte[] buffer = new byte[bufferSize];

                while(ACTIVE){
                    final StringBuilder sb = new StringBuilder();
                    bytesRead = inStream.read(buffer);
                    if (bytesRead != -1) {
                        String result = "";
                        while ((bytesRead == bufferSize) && (buffer[bufferSize-1] != 0)){
                            result = result + new String(buffer, 0, bytesRead - 1);
                            bytesRead = inStream.read(buffer);
                        }
                        result = result + new String(buffer, 0, bytesRead - 1);
                        sb.append(result);
                    }

                    //Show message on UIThread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), sb.toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private Runnable writer = new Runnable() {

        @Override
        public void run() {
            int index = 0;
            while (ACTIVE) {
                try {
                    index = index + 1;
                    outStream.write("Message From Client" + index + "\n");
                    outStream.flush();
                    Thread.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };
}
