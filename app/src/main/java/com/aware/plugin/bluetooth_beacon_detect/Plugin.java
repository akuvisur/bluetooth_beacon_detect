package com.aware.plugin.bluetooth_beacon_detect;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Aware_Plugin;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;

public class Plugin extends Aware_Plugin implements BeaconConsumer {

    protected static String TAG;

    private static BeaconManager beaconManager;

    public static final String BLUETOOTH_BEACON_DETECT_PARAM_CHANGE = "BLUETOOTH_BEACON_DETECT_PARAM_CHANGE";
    private static BroadcastReceiver changeReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "AWARE::"+getResources().getString(R.string.app_name)+":RangingActivity";

        //Any active plugin/sensor shares its overall context using broadcasts
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                Log.d(TAG, "onContext broadcast");
                sendBroadcast(broadcastIntentAll);
                sendBroadcast(broadcastIntentNearest);
            }
        };

        //Add permissions you need (Support for Android M). By default, AWARE asks access to the #Manifest.permission.WRITE_EXTERNAL_STORAGE
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH_ADMIN);

        beaconManager = BeaconManager.getInstanceForApplication(this);

        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        beaconManager.setForegroundScanPeriod(10000L);
        beaconManager.setBackgroundScanPeriod(10000L);

        beaconManager.bind(this);

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.BluetoothBeacon_Data.CONTENT_URI }; //this syncs dummy BluetoothBeacon_Data to server

        changeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                changeParams();
            }
        };
        IntentFilter changeFilter = new IntentFilter();
        changeFilter.addAction(BLUETOOTH_BEACON_DETECT_PARAM_CHANGE);
        registerReceiver(changeReceiver, changeFilter);

        //Activate plugin -- do this ALWAYS as the last thing (this will restart your own plugin and apply the settings)
        Aware.startPlugin(this, "com.aware.plugin.bluetooth_beacon_detect");

    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean permissions_ok = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        if (permissions_ok) {
            //Check if the user has toggled the debug messages
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_PLUGIN_BLUETOOTH_BEACON_DETECT, true);

        } else {
            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissions);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
        unregisterReceiver(changeReceiver);
        Aware.setSetting(this, Settings.STATUS_PLUGIN_BLUETOOTH_BEACON_DETECT, false);

        //Stop plugin
        Aware.stopPlugin(this, "com.aware.plugin.bluetooth_beacon_detect");

        //Stop AWARE
        Aware.stopAWARE();
    }

    Intent broadcastIntentAll;
    Intent broadcastIntentNearest;

    public static final String BROADCAST_ACTION_ALL = "com.aware.plugin.bluetooth_beacon_detect";
    public static final String BROADCAST_ACTION_NEAREST = "com.aware.plugin.bluetooth_beacon_detect.nearest_beacon";

    public static String DEVICE_ID;
    @Override
    public void onBeaconServiceConnect() {
        DEVICE_ID = Aware.getSetting(this, Aware_Preferences.DEVICE_ID);
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                Log.d(TAG, "found " + beacons.size() + " beacons.");

                if (beacons.size() > 0) {
                    Double nearest_beacon_distance = 99.0;
                    Beacon nearest_beacon = null;
                    // broadcast and store data for all scanned beacons
                    for (Beacon b : beacons) {
                        if (b.getDistance() < nearest_beacon_distance) {
                            nearest_beacon_distance = b.getDistance();
                            nearest_beacon = b;
                        }
                        broadcastIntentAll = new Intent();
                        broadcastIntentAll.setAction(BROADCAST_ACTION_ALL);
                        broadcastIntentAll.putExtra(Provider.BluetoothBeacon_Data.MAC_ADDRESS, b.getBluetoothAddress());
                        broadcastIntentAll.putExtra(Provider.BluetoothBeacon_Data.NAME, b.getBluetoothName());
                        broadcastIntentAll.putExtra(Provider.BluetoothBeacon_Data.ID1, b.getId1().toString());
                        broadcastIntentAll.putExtra(Provider.BluetoothBeacon_Data.ID2, b.getId2().toString());
                        broadcastIntentAll.putExtra(Provider.BluetoothBeacon_Data.ID3, b.getId3().toString());
                        broadcastIntentAll.putExtra(Provider.BluetoothBeacon_Data.DOUBLE_DISTANCE, b.getDistance());
                        broadcastIntentAll.putExtra(Provider.BluetoothBeacon_Data.NEAR, b.getDistance() < 1);
                        broadcastIntentAll.putExtra(Provider.BluetoothBeacon_Data.DOUBLE_RSSI, b.getRssi());
                        broadcastIntentAll.putExtra(Provider.BluetoothBeacon_Data.NUM_BEACONS, Integer.valueOf(beacons.size()));
                        broadcastIntentAll.putExtra(Provider.BluetoothBeacon_Data.LABEL, "");
                        sendBroadcast(broadcastIntentAll);

                        ContentValues cv = new ContentValues();
                        cv.put(Provider.BluetoothBeacon_Data.TIMESTAMP, System.currentTimeMillis());
                        cv.put(Provider.BluetoothBeacon_Data.DEVICE_ID, DEVICE_ID);
                        cv.put(Provider.BluetoothBeacon_Data.MAC_ADDRESS, b.getBluetoothAddress());
                        cv.put(Provider.BluetoothBeacon_Data.NAME, b.getBluetoothName());
                        cv.put(Provider.BluetoothBeacon_Data.ID1, b.getId1().toString());
                        cv.put(Provider.BluetoothBeacon_Data.ID2, b.getId2().toString());
                        cv.put(Provider.BluetoothBeacon_Data.ID3, b.getId3().toString());
                        cv.put(Provider.BluetoothBeacon_Data.DOUBLE_DISTANCE, b.getDistance());
                        cv.put(Provider.BluetoothBeacon_Data.NEAR, b.getDistance() < 1);
                        cv.put(Provider.BluetoothBeacon_Data.DOUBLE_RSSI, b.getRssi());
                        cv.put(Provider.BluetoothBeacon_Data.NUM_BEACONS, Integer.valueOf(beacons.size()));
                        cv.put(Provider.BluetoothBeacon_Data.LABEL, "");
                        getContentResolver().insert(Provider.BluetoothBeacon_Data.CONTENT_URI, cv);
                    }
                    // store and broadcast nearest beacon information
                    if (nearest_beacon != null) {
                        broadcastIntentNearest = new Intent();
                        broadcastIntentNearest.setAction(BROADCAST_ACTION_NEAREST);
                        broadcastIntentNearest.putExtra(Provider.BluetoothBeacon_Data.MAC_ADDRESS, nearest_beacon.getBluetoothAddress());
                        broadcastIntentNearest.putExtra(Provider.BluetoothBeacon_Data.NAME, nearest_beacon.getBluetoothName());
                        broadcastIntentNearest.putExtra(Provider.BluetoothBeacon_Data.ID1, nearest_beacon.getId1().toString());
                        broadcastIntentNearest.putExtra(Provider.BluetoothBeacon_Data.ID2, nearest_beacon.getId2().toString());
                        broadcastIntentNearest.putExtra(Provider.BluetoothBeacon_Data.ID3, nearest_beacon.getId3().toString());
                        broadcastIntentNearest.putExtra(Provider.BluetoothBeacon_Data.DOUBLE_DISTANCE, nearest_beacon.getDistance());
                        broadcastIntentNearest.putExtra(Provider.BluetoothBeacon_Data.NEAR, nearest_beacon.getDistance() < 1);
                        broadcastIntentNearest.putExtra(Provider.BluetoothBeacon_Data.DOUBLE_RSSI, nearest_beacon.getRssi());
                        broadcastIntentNearest.putExtra(Provider.BluetoothBeacon_Data.NUM_BEACONS, beacons.size());
                        broadcastIntentNearest.putExtra(Provider.BluetoothBeacon_Data.LABEL, "");
                        sendBroadcast(broadcastIntentNearest);

                        ContentValues cv = new ContentValues();
                        cv.put(Provider.BluetoothBeacon_Data.TIMESTAMP, System.currentTimeMillis());
                        cv.put(Provider.BluetoothBeacon_Data.DEVICE_ID, DEVICE_ID);
                        cv.put(Provider.BluetoothBeacon_Data.MAC_ADDRESS, nearest_beacon.getBluetoothAddress());
                        cv.put(Provider.BluetoothBeacon_Data.NAME, nearest_beacon.getBluetoothName());
                        cv.put(Provider.BluetoothBeacon_Data.ID1, nearest_beacon.getId1().toString());
                        cv.put(Provider.BluetoothBeacon_Data.ID2, nearest_beacon.getId2().toString());
                        cv.put(Provider.BluetoothBeacon_Data.ID3, nearest_beacon.getId3().toString());
                        cv.put(Provider.BluetoothBeacon_Data.DOUBLE_DISTANCE, nearest_beacon.getDistance());
                        cv.put(Provider.BluetoothBeacon_Data.NEAR, nearest_beacon.getDistance() < 1);
                        cv.put(Provider.BluetoothBeacon_Data.DOUBLE_RSSI, nearest_beacon.getRssi());
                        cv.put(Provider.BluetoothBeacon_Data.NUM_BEACONS, beacons.size());
                        cv.put(Provider.BluetoothBeacon_Data.LABEL, "");
                        getContentResolver().insert(Provider.NearestBeacon_Data.CONTENT_URI, cv);
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }

    public void changeParams() {
        beaconManager.unbind(this);
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Aware.getSetting(this, Settings.TYPE_PLUGIN_BLUETOOTH_BEACON_DETECT)));

        beaconManager.setForegroundScanPeriod(Long.parseLong(Aware.getSetting(this, Settings.FREQUENCY_PLUGIN_BLUETOOTH_BEACON_DETECT)));
        beaconManager.setBackgroundScanPeriod(Long.parseLong(Aware.getSetting(this, Settings.FREQUENCY_PLUGIN_BLUETOOTH_BEACON_DETECT)));

        beaconManager.bind(this);
    }

}
