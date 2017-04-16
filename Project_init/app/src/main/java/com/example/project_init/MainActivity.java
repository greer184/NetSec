package com.example.project_init;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
    Button b1;
    private BluetoothAdapter blueAdapt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set button to begin connection
        b1 = (Button) findViewById(R.id.button);

    }

    // Verify whether or not bluetooth is enabled
    public void on(View v){

        // Setup adapter
        blueAdapt = BluetoothAdapter.getDefaultAdapter();

        // Initial Bluetooth check
        if(blueAdapt == null){
            Toast.makeText(getApplicationContext(),"Unable to use Bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (!blueAdapt.isEnabled()) {
                Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnOn, 0);
                Toast.makeText(getApplicationContext(), "Bluetooth enabled" ,Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth already on" , Toast.LENGTH_LONG).show();
            }
        }
    }

}
