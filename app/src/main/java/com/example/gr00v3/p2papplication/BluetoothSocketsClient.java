package com.example.gr00v3.p2papplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Set;

import static android.R.id.list;


public class BluetoothSocketsClient {
    // Debugging
    private static final String TAG = "BluetoothSocketsClient";
    private static final boolean D = true;

    private final static int REQUEST_ENABLE_BT = 1;

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.randomUUID();
    // Member fields
    private final Activity mParentActivity;
    private final Context mContext;
    private final BluetoothAdapter mAdapter;
    private final BroadcastReceiver mReceiver;      //Receiver that filters and handles system messages

    //External devices
    ArrayList<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param parentActivity  The parent activity
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothSocketsClient(final Activity parentActivity, Handler handler) {
        mParentActivity = parentActivity;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mContext = parentActivity.getApplicationContext();
        //mHandler = handler;

        //Initialize broadcast receiver
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                //Check if the action declares a found device
                if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devices.add(device);
                }
                else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {

                }
                else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {

                }
                //Check if bluetooth has been turned off
                else if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    if(mAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                        //Query user to turn on bluetooth again
                        Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        parentActivity.startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
                    }
                }
            }
        };

        //Register receiver with scan filters

        mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

    }

    public void DiscoverPairedDevices() {
        // Starting the device discovery
        if (D) Log.d(TAG, "Starting Discovery");
        Set<BluetoothDevice> devicesDiscovered = mAdapter.getBondedDevices();
        if (D) Log.d(TAG, "Discovery done");

        // Listing paired devices
        if (D) Log.d(TAG, "Already paired devices:");
        for (BluetoothDevice device : devicesDiscovered) {
            if (D) Log.d(TAG, "Found alreday paired device: " + device.getName() + " Address: " + device.getAddress());
            devices.add(device);
        }

    }

}