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
    private static final int WRITE_PERMISSION = 2;

    private File storage;
    private File[] myFiles;
    private List<String> myFilenames;
    private List<String> displayNames;
    private Uri fileResource;

    private BluetoothAdapter blueAdapt;
    Button bBlue;
    Button bWifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Permission for reading from external storage(for Marshmallow and up)
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

        // Permission for writing from external storage(for Marshmallow and up)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_PERMISSION);
            }
        } else {
            Log.e("????", "granted");
        }

        // Get files from directory
        storage = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        myFiles = storage.listFiles();

        Log.e("????", storage.canRead() + "/" + storage.getPath());
        myFilenames = new ArrayList<String>();
        displayNames = new ArrayList<String>();
        myFilenames.add("None");
        displayNames.add("None Selected");
        for (int i = 0; i < myFiles.length; i++){
            myFilenames.add(myFiles[i].getAbsolutePath());
            displayNames.add(myFiles[i].getName());
        }

        // Create spinner which to select file
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, displayNames);
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
        String selection = displayNames.get(pos);
        String path = myFilenames.get(pos);
        Toast.makeText(getApplicationContext(), "Selected: " + selection, Toast.LENGTH_LONG).show();

        // Get a content URI
        if (path.equals("None")) {
        } else {
            File chosen = new File(path);
            try {
                fileResource = Uri.fromFile(chosen);
                Log.e("????", "is working!!!");
            } catch (Exception e) {
                Log.e("????", "not working???");
            }
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

            case WRITE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                }
                return;
            }

        }
    }

}
