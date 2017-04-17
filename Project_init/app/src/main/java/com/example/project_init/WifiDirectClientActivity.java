package com.example.project_init;


import java.io.File;
import java.util.ArrayList;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;



public class WifiDirectClientActivity extends Activity {

    public final int fileRequestID = 98;
    public final int port = 7950;


    private WifiP2pManager wifiManager;
    private Channel wifichannel;
    private BroadcastReceiver wifiClientReceiver;

    private IntentFilter wifiClientReceiverIntentFilter;

    private boolean connectedAndReadyToSendFile;

    private boolean transferActive;

    private Intent clientServiceIntent;
    private WifiP2pDevice targetDevice;
    private WifiP2pInfo wifiInfo;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifidirect_client_activity);
        //getActionBar().setDisplayHomeAsUpEnabled(true);

        wifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);

        //wifichannel = wifiManager.initialize(this, getMainLooper(), null);
        wifiClientReceiver = new WifiDirectBroadcastReceiverClient(wifiManager, wifichannel, this);

        wifiClientReceiverIntentFilter = new IntentFilter();

        wifiClientReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        wifiClientReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        wifiClientReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        wifiClientReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        connectedAndReadyToSendFile = false;


        clientServiceIntent = null;
        targetDevice = null;
        wifiInfo = null;

        registerReceiver(wifiClientReceiver, wifiClientReceiverIntentFilter);

        setClientFileTransferStatus("Client is currently idle");

        //setTargetFileStatus("testing");
    }

    public void searchForPeers(View view) {

        //Discover peers, no call back method given
        wifiManager.discoverPeers(wifichannel, null);

    }

    public void setTransferStatus(boolean status)
    {
        connectedAndReadyToSendFile = status;
    }

    public void setNetworkToReadyState(boolean status, WifiP2pInfo info, WifiP2pDevice device)
    {
        wifiInfo = info;
        targetDevice = device;
        connectedAndReadyToSendFile = status;
    }

    public void setClientWifiStatus(String message)
    {
        TextView connectionStatusText = (TextView) findViewById(R.id.client_wifi_status_text);
        connectionStatusText.setText(message);
    }

    public void setClientFileTransferStatus(String message)
    {
        //TextView fileTransferStatusText = (TextView) findViewById(R.id.file_transfer_status);
        //fileTransferStatusText.setText(message);
    }

    public void setClientStatus(String message)
    {
        TextView clientStatusText = (TextView) findViewById(R.id.client_status_text);
        clientStatusText.setText(message);
    }

    public void displayPeers(final WifiP2pDeviceList peers)
    {
        //Dialog to show errors/status
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("WiFi Direct File Transfer");


        ListView peerView = (ListView) findViewById(R.id.peers_listview);


        ArrayList<String> peersStringArrayList = new ArrayList<String>();

        //Fill array list with strings of peer names
        for(WifiP2pDevice wd : peers.getDeviceList())
        {
            peersStringArrayList.add(wd.deviceName);
        }


        peerView.setClickable(true);

        //Make adapter to connect peer data to list view
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, peersStringArrayList.toArray());

        //Show peer data in listview
        peerView.setAdapter(arrayAdapter);


        peerView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View view, int arg2,long arg3) {

                //Get string from textview
                TextView tv = (TextView) view;

                WifiP2pDevice device = null;

                //Search all known peers for matching name
                for(WifiP2pDevice wd : peers.getDeviceList())
                {
                    if(wd.deviceName.equals(tv.getText()))
                        device = wd;
                }

                if(device != null)
                {
                    //Connect to selected peer
                    connectToPeer(device);

                }
                else
                {
                    dialog.setMessage("Failed");
                    dialog.show();

                }
            }

        });

    }

    public void connectToPeer(final WifiP2pDevice wifiPeer)
    {
        this.targetDevice = wifiPeer;

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = wifiPeer.deviceAddress;
        wifiManager.connect(wifichannel, config, new WifiP2pManager.ActionListener(){
            public void onSuccess() {

                setClientStatus("Connection to " + targetDevice.deviceName + " sucessful");
            }

            public void onFailure(int reason) {
                setClientStatus("Connection to " + targetDevice.deviceName + " failed");

            }
        });

    }

}