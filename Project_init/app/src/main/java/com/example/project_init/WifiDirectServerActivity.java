package com.example.project_init;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;

public class WifiDirectServerActivity extends Activity{

        public final int fileRequestID = 55;
        public final int port = 7950;

        private WifiP2pManager wifiManager;
        private WifiP2pManager.Channel wifichannel;
        private BroadcastReceiver wifiServerReceiver;

        private IntentFilter wifiServerReceiverIntentFilter;

        private String path = "/";
        private File downloadTarget;

        private Intent serverServiceIntent;

        private boolean serverThreadActive;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            Log.d("intoClient", "made into server activity");
            super.onCreate(savedInstanceState);
            setContentView(R.layout.wifidirect_server_activity);

            wifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            wifichannel = wifiManager.initialize(this, getMainLooper(), null);
            wifiServerReceiver = new WifiDirectBroadcastReceiverServer(wifiManager, wifichannel, this);

            wifiServerReceiverIntentFilter = new IntentFilter();
            wifiServerReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            wifiServerReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            wifiServerReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            wifiServerReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


            //set status to stopped
            TextView serverServiceStatus = (TextView) findViewById(R.id.server_status_text);
           //serverServiceStatus.setText(R.string.server_stopped);

            downloadTarget = new File(path);

            serverServiceIntent = null;
            serverThreadActive = false;

            setServerFileTransferStatus("No File being transfered");

            registerReceiver(wifiServerReceiver, wifiServerReceiverIntentFilter);


        wifiManager.createGroup(wifichannel,  new WifiP2pManager.ActionListener()  {
    	    public void onSuccess() {
    	    	setServerFileTransferStatus("Wifi-Direct connection creation successful");
    	    }
    	    public void onFailure(int reason) {
    	    	setServerFileTransferStatus("Wifi-Direct connection creation failed");
    	    }
    	});
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {

            if (resultCode == Activity.RESULT_OK && requestCode == fileRequestID) {

                File targetDir = (File) data.getExtras().get("file");

                if(targetDir.isDirectory())
                {
                    if(targetDir.canWrite())
                    {
                        downloadTarget = targetDir;

                        setServerFileTransferStatus("Download directory set to " + targetDir.getName());

                    }
                    else
                    {
                        setServerFileTransferStatus("You do not have permission to write to " + targetDir.getName());
                    }

                }
                else
                {
                    setServerFileTransferStatus("The selected file is not a directory. Please select a valid download directory.");
                }
            }
        }

        public void startServer(View view) {

            if(!serverThreadActive)
            {
                Log.d("????", "got to start server");
                serverServiceIntent = new Intent(this, WifiDirectServerService.class);
                serverServiceIntent.putExtra("saveLocation", downloadTarget);
                serverServiceIntent.putExtra("port", new Integer(port));
                serverServiceIntent.putExtra("serverResult", new ResultReceiver(null) {
                    @Override
                    protected void onReceiveResult(int resultCode, final Bundle resultData) {

                        if(resultCode == port )
                        {
                            if (resultData == null) {

                                serverThreadActive = false;


                                final TextView server_status_text = (TextView) findViewById(R.id.server_status_text);
                                server_status_text.post(new Runnable() {
                                    public void run() {
                                        //server_status_text.setText(R.string.server_stopped);
                                    }
                                });


                            }
                            else
                            {
                                final TextView server_file_status_text = (TextView) findViewById(R.id.server_file_transfer_status);

                                server_file_status_text.post(new Runnable() {
                                    public void run() {
                                        server_file_status_text.setText((String)resultData.get("message"));
                                    }
                                });
                            }
                        }

                    }
                });

                serverThreadActive = true;
                startService(serverServiceIntent);

                TextView serverServiceStatus = (TextView) findViewById(R.id.server_status_text);
                //serverServiceStatus.setText(R.string.server_running);

            }
            else
            {
                TextView serverServiceStatus = (TextView) findViewById(R.id.server_status_text);
                serverServiceStatus.setText("The server is already running");
            }
        }

        public void stopServer(View view) {

            if(serverServiceIntent != null)
            {
                stopService(serverServiceIntent);

            }

        }

        @Override
        protected void onResume() {
            super.onResume();
        }

        @Override
        protected void onPause() {
            super.onPause();
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();

            stopServer(null);


            try {
                unregisterReceiver(wifiServerReceiver);
            } catch (IllegalArgumentException e) {

            }
        }

        public void setServerWifiStatus(String message)
        {
            TextView server_wifi_status_text = (TextView) findViewById(R.id.server_wifi_status_text);
            server_wifi_status_text.setText(message);
        }

        public void setServerStatus(String message)
        {
            TextView server_status_text = (TextView) findViewById(R.id.server_status_text_2);
            server_status_text.setText(message);
        }


        public void setServerFileTransferStatus(String message)
        {
            TextView server_status_text = (TextView) findViewById(R.id.server_file_transfer_status);
            server_status_text.setText(message);
        }

}
