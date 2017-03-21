package com.example.gr00v3.p2papplication;

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


public class BluetoothSocketsClient {
    // Debugging
    private static final String TAG = "BluetoothSocketsClient";
    private static final boolean D = true;

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.randomUUID();
    // Member fields
    private final Context mContext;
    private final BluetoothAdapter mAdapter;

    //External devices
    ArrayList<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothSocketsClient(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mContext = context;
        //mHandler = handler;
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