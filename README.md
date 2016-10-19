# Bluetooth Beacon Detect
AWARE Plugin for Bluetooth Beacon Detecting
Uses Android Beacon Library (altbeacon.github.io/android-beacon-library) to scan for five different types of bluetooth beacons.

# Database
Database tables contain two different sets of values; 
* Table for all beacons found while scanning (bt_beacons) and 
* Table for storing the nearest beacon of each scan (nearest_beacon)

Both share similar structure and contain the following information:
* Timestamp
* Device_id
* MAC address for the beacon
* Name and set of ID values (ID1, ID2, ID3) set in the beacon configuration
* Distance (in meters, approximation) to the beacon
* Whether the beacon is considered to be NEAR or not (< 1 meters)
* RSSI value from the beacon (in decibels)
* The number of nearby beacons found in the scan
* Label for the entry, to *e.g.* identify certain locations or beacons

# Broadcasts
The plugin sends two types of broadcasts whenever new scan is finished:
* ACTION_AWARE_PLUGIN_BT_BEACON_ALL = "com.aware.plugin.bluetooth_beacon_detect" 
  - contains information of all scanned beacons, one broadcast is sent for each found beacon
* ACTION_AWARE_PLUGIN_BT_BEACON_NEAREST = "com.aware.plugin.bluetooth_beacon_detect.nearest_beacon" 
  - contains information for the nearest beacon in each scan

Additionally, the last nearest beacon can be requested from the plugin by *sending* a broadcast with ACTION set as BLUETOOTH_BEACON_EMIT_CONTEXT_REQUEST = "BLUETOOTH_BEACON_EMIT_CONTEXT_REQUEST".

# Settings
You can also modify the following settings of the plugin:
* FREQUENCY_PLUGIN_BLUETOOTH_BEACON_DETECT = "frequency_plugin_bluetooth_beacon_detect" - Frequency of scans in milliseconds (default 10000ms)
* TYPE_PLUGIN_BLUETOOTH_BEACON_DETECT = "type_plugin_bluetooth_beacon_detect" - The Beacon type (Beacon Layout) that the plugin detects, single selection from the following:
  - ALTBEACON (m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25)
  - EDDYSTONE TLM (x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15)
  - EDDYSTONE UID (s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19)
  - EDDYSTONE URL (s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v)
  - IBEACON	(m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24)
* LABEL_PLUGIN_BLUETOOTH_BEACON_DETECT = "label_plugin_bluetooth_beacon_detect" - An identifier label that can be used to *e.g.* tag specific locations  