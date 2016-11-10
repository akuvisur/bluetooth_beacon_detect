package com.aware.plugin.bluetooth_beacon_detect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aware.Aware;
import com.aware.utils.IContextCard;

import java.util.Timer;
import java.util.TimerTask;

// TODO add button for adding a friendly name

public class ContextCard implements IContextCard {

    public static final String TAG = "ContextCard";

    TextView name;
    TextView distance_field;
    TextView mac_address;
    TextView num_neacons;
    TextView update_field;

    BroadcastReceiver br;

    Double b_distance = 99.0;
    String b_mac_address = "MAC address";
    String b_name = "name";
    Integer b_num_beacons = -1;

    Long last_update = System.currentTimeMillis();

    final Handler uiUpdater = new Handler();

    //Constructor used to instantiate this card
    public ContextCard() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateTimer();
            }
        }, 0, 1000);
    }

    public static boolean receiverRegistered = false;

    @Override
    public View getContextCard(Context context) {
        //Load card information to memory
        LayoutInflater sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View card = sInflater.inflate(R.layout.card, null);

        //Initialize UI elements from the card
        name = (TextView) card.findViewById(R.id.name);
        distance_field = (TextView) card.findViewById(R.id.distance);
        mac_address = (TextView) card.findViewById(R.id.mac_address);
        num_neacons = (TextView) card.findViewById(R.id.num_beacons);
        update_field = (TextView) card.findViewById(R.id.update_field);

        // if card not initiated yet
        if (!receiverRegistered) {
            br = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(Plugin.ACTION_AWARE_PLUGIN_BT_BEACON_NEAREST)) {
                        Log.d(TAG, "broadcast received");
                        b_distance = intent.getDoubleExtra(Provider.NearestBeacon_Data.DOUBLE_DISTANCE,99.0);
                        b_mac_address = intent.getStringExtra(Provider.NearestBeacon_Data.MAC_ADDRESS);
                        b_name = intent.getStringExtra(Provider.NearestBeacon_Data.NAME);
                        b_num_beacons = intent.getIntExtra(Provider.NearestBeacon_Data.NUM_BEACONS, -1);
                        updateUI();

                    }
                }
            };
            IntentFilter i = new IntentFilter();
            i.addAction(Plugin.ACTION_AWARE_PLUGIN_BT_BEACON_NEAREST);
            context.registerReceiver(br, i);
        }
        //Log.d(TAG, "requesting new context");
        context.sendBroadcast(new Intent("BLUETOOTH_BEACON_EMIT_CONTEXT_REQUEST"));
        //Return the card to AWARE/apps
        return card;
    }

    private void updateUI() {
        //Log.d(TAG, "update ui");
        distance_field.setText(b_distance.toString().substring(0,3) + " meters");
        mac_address.setText("MAC: " + b_mac_address);
        name.setText(b_name);
        num_neacons.setText("Total of " + b_num_beacons + " nearby beacons");
        last_update = System.currentTimeMillis();
    }

    private void updateTimer() {
        uiUpdater.post(new Runnable() {
            @Override
            public void run() {
                update_field.setText("Last update: " + (System.currentTimeMillis()-last_update)/1000 + " seconds ago");
            }
        });

    }
}
