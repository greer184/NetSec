package com.example.project_init;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class SetUpWifiActivity extends AppCompatActivity {

    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_up_wifi);
    }

    public void sendFiles(View view){
        Intent pastIntent = getIntent();
        path = pastIntent.getExtras().getString("Filename");
        Intent intent = new Intent(this, WifiDirectClientActivity.class);
        intent.putExtra("Filename", path);
        startActivity(intent);

    }

    public void receiveFiles(View view){
        //Intent pastIntent = getIntent();
        //path = pastIntent.getExtras().getString("Filename");
        Intent intent = new Intent(this, WifiDirectServerActivity.class);
        //intent.putExtra("Filename", path);
        startActivity(intent);

    }
}
