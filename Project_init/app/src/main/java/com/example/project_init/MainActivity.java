package com.example.project_init;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener{
    private static final int READ_PERMISSION = 1;

    private File storage;
    private File[] myFiles;
    private List<String> myFilenames;
    private Uri fileResource;

    private BluetoothAdapter blueAdapt;
    Button bBlue;
    Button bWifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_PERMISSION);
            }
        } else {
            Log.e("????", "granted");
        }

        // Get files from directory
        storage = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString());
        myFiles = storage.listFiles();

        Log.e("????", storage.canRead() + "/" + storage.getPath());
        myFilenames = new ArrayList<String>();
        for (int i = 0; i < myFiles.length; i++){
            myFilenames.add(myFiles[i].getAbsolutePath());
        }

        // Create spinner which to select file
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, myFilenames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        // Buttons to select
        bBlue = (Button) findViewById(R.id.buttonBlue);
        bWifi = (Button) findViewById(R.id.buttonWifi);

    }

    //set up page for bluetooth connection
    public void selectBlueTooth(View view) {

        // Get default adapter so Bluetooth works
        blueAdapt = BluetoothAdapter.getDefaultAdapter();

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

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {

        // Get file from spinner
        String selection = (String) parent.getItemAtPosition(pos);
        File chosen = new File(selection);
        Toast.makeText(getApplicationContext(), "Selected: " + selection, Toast.LENGTH_LONG).show();

        // Use the FileProvider to get a content URI
        try {
            Uri.fromFile(chosen);
        } catch (Exception e) {
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case READ_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                }
                return;
            }
        }
    }

}
