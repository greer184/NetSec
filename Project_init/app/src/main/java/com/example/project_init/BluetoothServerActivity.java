package com.example.project_init;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class BluetoothServerActivity extends Activity {

    private static final int DISCOVERABLE_REQUEST_CODE = 0x1;
    private boolean CONTINUE_READ_WRITE = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_server);

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(discoverableIntent, DISCOVERABLE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        android.util.Log.e("TrackingFlow", "Creating thread to start listening...");
        new Thread(reader).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(socket != null){
            try{
                inStream.close();
                outStream.close();
                socket.close();
            }catch(Exception e){

            }
            CONTINUE_READ_WRITE = false;
        }
    }

    private BluetoothSocket socket;
    private InputStream inStream;
    private OutputStreamWriter outStream;
    private Runnable reader = new Runnable() {
        public void run() {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            UUID uuid = UUID.fromString("1c6b6701-2ee1-49e8-bf2f-119366d741a5");
            try {

                BluetoothServerSocket serverSocket = adapter.listenUsingRfcommWithServiceRecord("Server", uuid);
                socket = serverSocket.accept();

                inStream = socket.getInputStream();
                outStream = new OutputStreamWriter(socket.getOutputStream());
                new Thread(writer).start();

                int bufferSize = 1024;
                int bytesRead = -1;
                byte[] buffer = new byte[bufferSize];

                while(CONTINUE_READ_WRITE){
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
            while(CONTINUE_READ_WRITE){
                try {

                    index = index + 1;
                    outStream.write("Message From Server" + (index) + "\n");
                    outStream.flush();
                    Thread.sleep(2000);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };
}

