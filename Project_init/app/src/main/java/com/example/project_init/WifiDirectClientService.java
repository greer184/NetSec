package com.example.project_init;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;

import android.app.IntentService;
import android.content.Intent;
import android.os.ResultReceiver;
import android.util.Log;

public class WifiDirectClientService extends IntentService {

    private boolean serviceEnabled;

    private int port;
    private File fileToSend;
    private ResultReceiver clientResult;
    private WifiP2pDevice targetDevice;
    private WifiP2pInfo wifiInfo;

    public WifiDirectClientService() {
        super("WifiDirectClientService");
        serviceEnabled = true;

    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d("????", "client send file service start");
        port = ((Integer) intent.getExtras().get("port")).intValue();
        fileToSend = (File) intent.getExtras().get("fileToSend");
        clientResult = (ResultReceiver) intent.getExtras().get("clientResult");
        targetDevice = (WifiP2pDevice) intent.getExtras().get("targetDevice");
        wifiInfo = (WifiP2pInfo) intent.getExtras().get("wifiInfo");

        if(!wifiInfo.isGroupOwner)
        {
            //targetDevice.
            signalActivity(wifiInfo.isGroupOwner + " Transfering file " + fileToSend.getName() + " to " + wifiInfo.groupOwnerAddress.toString()  + " on TCP Port: " + port );

            InetAddress targetIP = wifiInfo.groupOwnerAddress;

            Socket clientSocket = null;
            OutputStream os = null;
            Log.d("????", "send file start before try");
            try {
                Log.d("????", "send file start in try");
                clientSocket = new Socket(targetIP, port);
                os = clientSocket.getOutputStream();
                PrintWriter pw = new PrintWriter(os);


                InputStream is = clientSocket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                signalActivity("About to start handshake");

                byte[] buffer = new byte[4096];

                FileInputStream fis = new FileInputStream(fileToSend);
                BufferedInputStream bis = new BufferedInputStream(fis);
                // long BytesToSend = fileToSend.length();

                while(true)
                {

                    int bytesRead = bis.read(buffer, 0, buffer.length);

                    if(bytesRead == -1)
                    {
                        break;
                    }

                    //BytesToSend = BytesToSend - bytesRead;
                    os.write(buffer,0, bytesRead);
                    os.flush();
                }

                fis.close();
                bis.close();

                br.close();
                isr.close();
                is.close();

                pw.close();
                os.close();

                clientSocket.close();

                signalActivity("File Transfer Complete, sent file: " + fileToSend.getName());


            } catch (IOException e) {
                signalActivity(e.getMessage());
            }
            catch(Exception e)
            {
                signalActivity(e.getMessage());

            }

        }
        else
        {
            signalActivity("This device is a group owner, therefore the IP address of the " +
                    "target device cannot be determined. File transfer cannot continue");
        }


        clientResult.send(port, null);
    }

    public void signalActivity(String message)
    {
        Bundle b = new Bundle();
        b.putString("message", message);
        clientResult.send(port, b);
    }


    public void onDestroy()
    {
        serviceEnabled = false;

        //Signal that the service was stopped
        //serverResult.send(port, new Bundle());

        stopSelf();
    }

}