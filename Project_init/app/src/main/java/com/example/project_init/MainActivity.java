package com.example.project_init;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.Button;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Set;

public class MainActivity extends Activity {
    private BluetoothAdapter blueAdapt;
    Button bBlue;
    Button bWifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bBlue = (Button) findViewById(R.id.buttonBlue);
        bWifi = (Button) findViewById(R.id.buttonWifi);

    }

    //set up page for bluetooth connection
    public void selectBlueTooth(View view) {

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

            Intent intent = new Intent(this, SetUpBlueToothActivity.class);
            startActivity(intent);

        }


    }

    public void selectWifi(View view) {
        Intent intent = new Intent(this, SetUpWifiActivity.class);
        startActivity(intent);
    }

}
