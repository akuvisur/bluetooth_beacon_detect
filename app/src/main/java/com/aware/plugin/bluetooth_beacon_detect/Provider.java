package com.aware.plugin.bluetooth_beacon_detect;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

public class Provider extends ContentProvider {

    public static String AUTHORITY = "com.aware.plugin.bluetooth_beacon_detect.provider.bt_beacons"; //change to package.provider.your_plugin_name
    public static final int DATABASE_VERSION = 3; //increase this if you make changes to the database structure, i.e., rename columns, etc.

    public static Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final String DATABASE_NAME = "plugin_bluetooth_beacon_detect.db"; //the database filename, use plugin_xxx for plugins.

    //Add here your database table names, as many as you need
    public static final String DB_TBL_BT_BEACONS = "bt_beacons";

    //For each table, add two indexes: DIR and ITEM. The index needs to always increment. Next one is 3, and so on.
    private static final int TABLE_ONE_DIR = 1;
    private static final int TABLE_ONE_ITEM = 2;

    //Put tables names in this array so AWARE knows what you have on the database
    public static final String[] DATABASE_TABLES = {
            DB_TBL_BT_BEACONS
    };

    //These are columns that we need to sync data, don't change this!
    public interface AWAREColumns extends BaseColumns {
        String _ID = "_id";
        String TIMESTAMP = "timestamp";
        String DEVICE_ID = "device_id";
    }

    /**
     * Create one of these per database table
     * In this example, we are adding example columns
     */
    public static final class BluetoothBeacon_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(Provider.CONTENT_URI, DB_TBL_BT_BEACONS);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.plugin.bluetooth_beacon_detect.bt_beacons"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.plugin.bluetooth_beacon_detect.bt_beacons"; //modify me

        //Note: integers and strings don't need a type prefix_
        public static final String MAC_ADDRESS = "mac_address";
        public static final String NAME = "name";
        public static final String ID1 = "id1";
        public static final String ID2 = "id2";
        public static final String ID3 = "id3";
        public static final String DOUBLE_DISTANCE = "distance";
        public static final String NEAR = "near";
        public static final String DOUBLE_RSSI = "rssi";
        public static final String LABEL = "label";

    }

    //Define each database table fields
    private static final String DB_TBL_ALL_FIELDS =
        BluetoothBeacon_Data._ID + " integer primary key autoincrement," +
        BluetoothBeacon_Data.TIMESTAMP + " real default 0," +
        BluetoothBeacon_Data.DEVICE_ID + " text default ''," +
        BluetoothBeacon_Data.MAC_ADDRESS + " text default ''," +
        BluetoothBeacon_Data.NAME + " text default ''," +
        BluetoothBeacon_Data.ID1 + " text default ''," +
        BluetoothBeacon_Data.ID2 + " text default ''," +
        BluetoothBeacon_Data.ID3 + " text default ''," +
        BluetoothBeacon_Data.DOUBLE_DISTANCE + " real default 0," +
        BluetoothBeacon_Data.NEAR + " boolean default FALSE," +
        BluetoothBeacon_Data.DOUBLE_RSSI + " real default 0," +
        BluetoothBeacon_Data.LABEL + " text default ''";

    /**
     * Share the fields with AWARE so we can replicate the table schema on the server
     */
    public static final String[] TABLES_FIELDS = {
            DB_TBL_ALL_FIELDS
    };

    //Helper variables for ContentProvider - don't change me
    private static UriMatcher sUriMatcher;
    private static DatabaseHelper databaseHelper;
    private static SQLiteDatabase database;

    //For each table, create a hashmap needed for database queries
    private static HashMap<String, String> tableOneHash;

    /**
     * Initialise database: create the database file, update if needed, etc. DO NOT CHANGE ME
     * @return
     */
    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        }
        if (database == null || !database.isOpen()) {
            database = databaseHelper.getWritableDatabase();
        }
        return (database != null && databaseHelper != null);
    }

    @Override
    public boolean onCreate() {
        //This is a hack to allow providers to be reusable in any application/plugin by making the authority dynamic using the package name of the parent app
        AUTHORITY = getContext().getPackageName() + ".provider.bt_beacons"; //make sure xxx matches the first string in this class

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        //For each table, add indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], TABLE_ONE_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", TABLE_ONE_ITEM);

        //Create each table hashmap so Android knows how to insert data to the database. Put ALL table fields.
        tableOneHash = new HashMap<>();
        tableOneHash.put(BluetoothBeacon_Data._ID, BluetoothBeacon_Data._ID);
        tableOneHash.put(BluetoothBeacon_Data.TIMESTAMP, BluetoothBeacon_Data.TIMESTAMP);
        tableOneHash.put(BluetoothBeacon_Data.DEVICE_ID, BluetoothBeacon_Data.DEVICE_ID);
        tableOneHash.put(BluetoothBeacon_Data.MAC_ADDRESS, BluetoothBeacon_Data.MAC_ADDRESS);
        tableOneHash.put(BluetoothBeacon_Data.NAME, BluetoothBeacon_Data.NAME);
        tableOneHash.put(BluetoothBeacon_Data.ID1, BluetoothBeacon_Data.ID1);
        tableOneHash.put(BluetoothBeacon_Data.ID2, BluetoothBeacon_Data.ID2);
        tableOneHash.put(BluetoothBeacon_Data.ID3, BluetoothBeacon_Data.ID3);
        tableOneHash.put(BluetoothBeacon_Data.DOUBLE_DISTANCE, BluetoothBeacon_Data.DOUBLE_DISTANCE);
        tableOneHash.put(BluetoothBeacon_Data.NEAR, BluetoothBeacon_Data.NEAR);
        tableOneHash.put(BluetoothBeacon_Data.DOUBLE_RSSI, BluetoothBeacon_Data.DOUBLE_RSSI);
        tableOneHash.put(BluetoothBeacon_Data.LABEL, BluetoothBeacon_Data.LABEL);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (!initializeDB()) {
            Log.w("", "Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {

            //Add all tables' DIR entries, with the right table index
            case TABLE_ONE_DIR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(tableOneHash); //the hashmap of the table
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        //Don't change me
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            //Add each table indexes DIR and ITEM
            case TABLE_ONE_DIR:
                return BluetoothBeacon_Data.CONTENT_TYPE;
            case TABLE_ONE_ITEM:
                return BluetoothBeacon_Data.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues new_values) {
        if (!initializeDB()) {
            Log.w("", "Database unavailable...");
            return null;
        }

        ContentValues values = (new_values != null) ? new ContentValues(new_values) : new ContentValues();
        long _id;

        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case TABLE_ONE_DIR:
                _id = database.insert(DATABASE_TABLES[0], BluetoothBeacon_Data.DEVICE_ID, values);
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(BluetoothBeacon_Data.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                throw new SQLException("Failed to insert row into " + uri);

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (!initializeDB()) {
            Log.w("", "Database unavailable...");
            return 0;
        }

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case TABLE_ONE_DIR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (!initializeDB()) {
            Log.w("", "Database unavailable...");
            return 0;
        }

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case TABLE_ONE_DIR:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;

            default:
                database.close();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
