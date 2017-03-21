package com.example.gr00v3.p2papplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.Set;


public class BluetoothSocketsClient {
    // Debugging
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.randomUUID();
    // Member fields
    private final Context mContext;
    private final BluetoothAdapter mAdapter;

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

    public void DiscoverDevices() {
        // Starting the device discovery
        if (D) Log.d(TAG, "Starting Discovery");
        mAdapter.startDiscovery();
        if (D) Log.d(TAG, "Discovery done");
        // Listing paired devices
        if (D) Log.d(TAG, "Devices paired:");
        Set<BluetoothDevice> devices = mAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            if (D) Log.d(TAG, "Found device: " + device.getName() + " Address: " + device.getAddress());
        }

    }

}