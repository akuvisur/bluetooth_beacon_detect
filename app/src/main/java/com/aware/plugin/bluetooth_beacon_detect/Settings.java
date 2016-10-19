package com.aware.plugin.bluetooth_beacon_detect;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "PluginSettings";

    public static final String TYPE_ALTBEACON = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
    public static final String TYPE_EDDYSTONE_TLM = "x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15";
    public static final String TYPE_EDDYSTONE_UID = "s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19";
    public static final String TYPE_EDDYSTONE_URL = "s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v";
    public static final String TYPE_IBEACON = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    //Plugin settings in XML @xml/preferences
    public static final String STATUS_PLUGIN_BLUETOOTH_BEACON_DETECT = "status_plugin_bluetooth_beacon_detect";
    public static final String FREQUENCY_PLUGIN_BLUETOOTH_BEACON_DETECT = "frequency_plugin_bluetooth_beacon_detect";
    public static final String TYPE_PLUGIN_BLUETOOTH_BEACON_DETECT = "type_plugin_bluetooth_beacon_detect";
    public static final String LABEL_PLUGIN_BLUETOOTH_BEACON_DETECT = "label_plugin_bluetooth_beacon_detect";

    //Plugin settings UI elements
    private static CheckBoxPreference status;
    private static ListPreference types;
    private static EditTextPreference frequency;
    private static EditTextPreference label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_BLUETOOTH_BEACON_DETECT);
        if( Aware.getSetting(this, STATUS_PLUGIN_BLUETOOTH_BEACON_DETECT).length() == 0 ) {
            Aware.setSetting( this, STATUS_PLUGIN_BLUETOOTH_BEACON_DETECT, true ); //by default, the setting is true on install
        }
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_BLUETOOTH_BEACON_DETECT).equals("true"));

        frequency = (EditTextPreference) findPreference(FREQUENCY_PLUGIN_BLUETOOTH_BEACON_DETECT);
        if (Aware.getSetting(this, FREQUENCY_PLUGIN_BLUETOOTH_BEACON_DETECT).length() == 0)
            Aware.setSetting(this, FREQUENCY_PLUGIN_BLUETOOTH_BEACON_DETECT, 10000L);
        frequency.setSummary("Every " + Aware.getSetting(this, FREQUENCY_PLUGIN_BLUETOOTH_BEACON_DETECT) + " millisecond(s)");

        types = (ListPreference) findPreference(TYPE_PLUGIN_BLUETOOTH_BEACON_DETECT);
        if (Aware.getSetting(this, TYPE_PLUGIN_BLUETOOTH_BEACON_DETECT).length() == 0)
            Aware.setSetting(this, TYPE_PLUGIN_BLUETOOTH_BEACON_DETECT, TYPE_IBEACON);
        types.setSummary(Aware.getSetting(this, TYPE_PLUGIN_BLUETOOTH_BEACON_DETECT));

        label = (EditTextPreference) findPreference(LABEL_PLUGIN_BLUETOOTH_BEACON_DETECT);
        if (Aware.getSetting(this, LABEL_PLUGIN_BLUETOOTH_BEACON_DETECT).length() == 0)
            Aware.setSetting(this, LABEL_PLUGIN_BLUETOOTH_BEACON_DETECT, "");
        label.setSummary(Aware.getSetting(this, LABEL_PLUGIN_BLUETOOTH_BEACON_DETECT));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference setting = findPreference(key);
        if( setting.getKey().equals(STATUS_PLUGIN_BLUETOOTH_BEACON_DETECT) ) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            status.setChecked(sharedPreferences.getBoolean(key, false));
        }

        if( setting.getKey().equals(FREQUENCY_PLUGIN_BLUETOOTH_BEACON_DETECT) ) {
            Aware.setSetting(this, key, Long.valueOf(sharedPreferences.getString(key, "10000")));
            frequency.setSummary(Aware.getSetting(getApplicationContext(), FREQUENCY_PLUGIN_BLUETOOTH_BEACON_DETECT) + " milliseconds");
        }

        if( setting.getKey().equals(TYPE_PLUGIN_BLUETOOTH_BEACON_DETECT) ) {
            Log.d(TAG, "Type: " + sharedPreferences.getString(key, "default"));
            Aware.setSetting(this, key, sharedPreferences.getString(key, TYPE_IBEACON));
            types.setSummary(Aware.getSetting(getApplicationContext(), TYPE_PLUGIN_BLUETOOTH_BEACON_DETECT));
        }

        if( setting.getKey().equals(LABEL_PLUGIN_BLUETOOTH_BEACON_DETECT) ) {
            Aware.setSetting(this, key, sharedPreferences.getString(key, ""));
            label.setSummary(sharedPreferences.getString(key, ""));
        }

        if (Aware.getSetting(this, STATUS_PLUGIN_BLUETOOTH_BEACON_DETECT).equals("true")) {
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.bluetooth_beacon_detect");
        } else {
            Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.bluetooth_beacon_detect");
        }
        Intent paramChangeIntent = new Intent(Plugin.BLUETOOTH_BEACON_DETECT_PARAM_CHANGE);
        sendBroadcast(paramChangeIntent);
    }

}
