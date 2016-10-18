package com.aware.plugin.bluetooth_beacon_detect;

import android.Manifest;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
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
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    protected static String TAG;
    private BeaconManager beaconManager;

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "AWARE::"+getResources().getString(R.string.app_name)+":RangingActivity";

        //Any active plugin/sensor shares its overall context using broadcasts
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
            }
        };

        //Add permissions you need (Support for Android M). By default, AWARE asks access to the #Manifest.permission.WRITE_EXTERNAL_STORAGE
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);

        beaconManager.setForegroundScanPeriod(10000L);
        beaconManager.setBackgroundScanPeriod(10000L);
        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.All_Data.CONTENT_URI }; //this syncs dummy All_Data to server

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
            Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, true);

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
        Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, false);

        //Stop plugin
        Aware.stopPlugin(this, "com.aware.plugin.bluetooth_beacon_detect");

        //Stop AWARE
        Aware.stopAWARE();
    }

    Intent broadcastIntent;
    public static final String BROADCAST_ACTION = "com.aware.plugin.bluetooth_beacon_detect";
    @Override
    public void onBeaconServiceConnect() {
        //Log.d(TAG, "plop");
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                //Log.d(TAG, "found " + beacons.size() + " beacons.");
                if (beacons.size() > 0) {
                    for (Beacon b : beacons) {
                        broadcastIntent = new Intent();
                        broadcastIntent.setAction(BROADCAST_ACTION);
                        broadcastIntent.putExtra(Provider.All_Data.MAC_ADDRESS, b.getBluetoothAddress());
                        broadcastIntent.putExtra(Provider.All_Data.NAME, b.getBluetoothName());
                        broadcastIntent.putExtra(Provider.All_Data.ID1, b.getId1().toString());
                        broadcastIntent.putExtra(Provider.All_Data.ID2, b.getId2().toString());
                        broadcastIntent.putExtra(Provider.All_Data.ID3, b.getId3().toString());
                        broadcastIntent.putExtra(Provider.All_Data.DISTANCE, b.getDistance());
                        broadcastIntent.putExtra(Provider.All_Data.NEAR, b.getDistance() < 1);
                        broadcastIntent.putExtra(Provider.All_Data.RSSI, b.getRssi());
                        sendBroadcast(broadcastIntent);

                        ContentValues cv = new ContentValues();
                        cv.put(Provider.All_Data.MAC_ADDRESS, b.getBluetoothAddress());
                        cv.put(Provider.All_Data.NAME, b.getBluetoothName());
                        cv.put(Provider.All_Data.ID1, b.getId1().toString());
                        cv.put(Provider.All_Data.ID2, b.getId2().toString());
                        cv.put(Provider.All_Data.ID3, b.getId3().toString());
                        cv.put(Provider.All_Data.DISTANCE, b.getDistance());
                        cv.put(Provider.All_Data.NEAR, b.getDistance() < 1);
                        cv.put(Provider.All_Data.RSSI, b.getRssi());

                        Log.i(TAG, "inserting");
                        Provider p = new Provider();
                        p.insert(Provider.CONTENT_URI, cv);
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }

}
