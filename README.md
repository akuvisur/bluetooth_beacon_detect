# Bluetooth Beacon Detect
AWARE Plugin for Bluetooth Beacon Detecting
Uses Android Beacon Library (altbeacon.github.io/android-beacon-library) to scan for five different types of bluetooth beacons.

# Database
Database tables contain two different sets of values; 
one table for all beacons found while scanning (bt_beacons) and 
one table for storing the nearest beacon of each scan (nearest_beacon)

Both share similar structure and contain the following information:
Timestamp
Device_id
MAC address for the beacon
Name and set of ID values (ID1, ID2, ID3) set in the beacon configuration
Distance (in meters, approximation) to the beacon
Whether the beacon is considered to be NEAR or not (< 1 meters)
RSSI value from the beacon (in decibels)
The number of nearby beacons found in the scan
Label for the entry, to e.g. identify certain locations or beacons

# Broadcasts
The plugin sends two types of broadcasts whenever new scan is finished:
"com.aware.plugin.bluetooth_beacon_detect" - contains information of all scanned beacons, one broadcast is sent for each found beacon
"com.aware.plugin.bluetooth_beacon_detect.nearest_beacon" - contains information for the nearest beacon in each scan