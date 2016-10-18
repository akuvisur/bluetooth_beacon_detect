package com.aware.plugin.bluetooth_beacon_detect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aware.utils.IContextCard;

import java.util.Timer;

public class ContextCard implements IContextCard {

    public static final String TAG = "ContextCard";

    TextView dist;
    TextView addr;

    BroadcastReceiver br;

    Double distance = 99.0;
    String address = "address";

    //Constructor used to instantiate this card
    public ContextCard() {
        Timer timer = new Timer();
        /*
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateUI();
            }
        }, 0, 1000); //update the UI every 1 second
        */
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Plugin.BROADCAST_ACTION)) {
                    Log.d(TAG, "broadcast received");
                    if (intent.getDoubleExtra("Distance",99.0) < 1) {
                        distance = intent.getDoubleExtra(Provider.BluetoothBeacon_Data.DOUBLE_DISTANCE,99.0);
                        address = intent.getStringExtra(Provider.BluetoothBeacon_Data.MAC_ADDRESS);
                        updateUI();
                    }
                }
            }
        };

    }

    public static boolean receiverRegistered = false;

    @Override
    public View getContextCard(Context context) {
        //Load card information to memory
        LayoutInflater sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View card = sInflater.inflate(R.layout.card, null);

        //Initialize UI elements from the card
        dist = (TextView) card.findViewById(R.id.distance);
        addr = (TextView) card.findViewById(R.id.address);

        if (!receiverRegistered) {
            IntentFilter i = new IntentFilter();
            i.addAction(Plugin.BROADCAST_ACTION);
            context.registerReceiver(br, i);
        }
        //Return the card to AWARE/apps
        return card;
    }

    private void updateUI() {
        Log.d(TAG, "update ui");
        dist.setText(distance.toString().substring(0,3) + " meters");
        addr.setText(address);
    }
}
