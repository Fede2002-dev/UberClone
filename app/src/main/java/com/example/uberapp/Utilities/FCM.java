package com.example.uberapp.Utilities;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.example.uberapp.DriverHomeActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCM extends FirebaseMessagingService {
    DriverHomeActivity a;
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.e("t", s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);


        Log.e("TAG","mensaje recibido de: " + remoteMessage.getFrom());

        Log.e("msg", "mgs");

        if(remoteMessage.getData().size()>0){
            Log.e("msg", "el id es: " + remoteMessage.getData().get("id"));
            Log.e("msg", "el id es: " + remoteMessage.getData().get("user_pos"));

            if(DriverHomeActivity.A !=null){
                String destination = remoteMessage.getData().get("user_pos");
                String end_pos = remoteMessage.getData().get("end_pos");
                String key = remoteMessage.getData().get("key");
                String id = remoteMessage.getData().get("id");
                String start = remoteMessage.getData().get("start");
                String end = remoteMessage.getData().get("end");
                String distance = remoteMessage.getData().get("distance");
                String time = remoteMessage.getData().get("time");
                a = DriverHomeActivity.A;
                a.comm(id,key, destination,end_pos,start,end, distance, time);
            }

        }

    }
}
