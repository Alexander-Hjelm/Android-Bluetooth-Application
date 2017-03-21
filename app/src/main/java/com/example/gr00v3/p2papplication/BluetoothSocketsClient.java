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
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Set;

import static android.R.id.list;
import static android.os.Build.VERSION_CODES.M;


public class BluetoothSocketsClient {
    // Debugging
    private static final String TAG = "BluetoothSocketsClient";
    private static final boolean D = true;

    private final static int REQUEST_ENABLE_BT = 0;
    private final static int SUCCESS_CONNECT = 1;

    //Message constants
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_TOAST = 4;


    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.randomUUID();
    // Member fields
    private final Activity mParentActivity;
    private final Context mContext;
    private final BluetoothAdapter mAdapter;
    private final BroadcastReceiver mReceiver;      //Receiver that filters and handles system messages

    private final Handler mHandler;

    //External devices
    ArrayList<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>();
    ArrayList<BluetoothDevice> newDevices = new ArrayList<BluetoothDevice>();

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
                    if (D) Log.d(TAG, "Found new device: " + device.getName() + " Address: " + device.getAddress());
                    newDevices.add(device);
                }
                else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {

                }
                else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                    //Check if any of the new devices are already paired
                    if(newDevices.size() > 0) {
                        for (int i = 0; i < newDevices.size(); i++) {
                            for (int j = 0; j < pairedDevices.size(); j++) {
                                if(newDevices.get(i).equals(pairedDevices.get(j))) {
                                    newDevices.remove(j);
                                }
                                break;
                            }
                        }
                    }
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

        //TODO: Unregister receiver onPause()

        //Initialize handler
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case SUCCESS_CONNECT:
                        // Do something once we hae successfully connected to a device
                        ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket) msg.obj);
                        Toast.makeText(mContext, "Successfully connected to a Bluetooth device", Toast.LENGTH_SHORT).show();

                        //Write to socket
                        if (D) Log.d(TAG, "Writing to socket");
                        connectedThread.write("HI FANDANGO".getBytes());

                        break;
                    case MESSAGE_READ:
                        // Do something once we have received a message
                        if (D) Log.d(TAG, "Reading message from socket");
                        byte[] readFromBuffer = (byte[])msg.obj;    //String is in msg.obj
                        String msgString = readFromBuffer.toString();
                        Toast.makeText(mContext, "RECEIVED MESSAGE: " + msgString, Toast.LENGTH_SHORT   );
                        break;
                }
            }
        };

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
            pairedDevices.add(device);
        }

    }

    public void DiscoverNewDevices() {
        // Starting the device discovery
        if (D) Log.d(TAG, "Starting Discovery");
        mAdapter.cancelDiscovery();
        mAdapter.startDiscovery();
        if (D) Log.d(TAG, "Discovery done");
        //New devices are handled when mReceiver gets an ACTION_FOUND Intent
    }

    public void ConnectToPairedDevices() {
        for (BluetoothDevice device: pairedDevices) {
            if (D) Log.d(TAG, "Connecting to device: " + device.getName() + "@" + device.getAddress());
            ConnectThread connectThread = new ConnectThread(device);
            connectThread.start();
        }
    }

    //Thread class from google api
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            // Send message to handler
            mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();      //In handler: get socket with msg.obj
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    mmBuffer = new byte[1024];  //Clear buffer
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(
                            MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();

                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = mHandler.obtainMessage(
                        MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

}