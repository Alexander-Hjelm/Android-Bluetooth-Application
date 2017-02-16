package com.example.gr00v3.p2papplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import android.os.Handler;

import static android.R.attr.filter;

/**
 * Created by Gr00v3 on 02/10/2017.
 */

public class BLEClient {

    private final static int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private Context mContext;
    private Activity parentActivity;

    private String uuid = "d5c54f8e-c7b2-430c-8044-8b7be9fb23a1";
    //private String uuid = "d5e78474-fce9-4d8c-ac0f-8941480f9285";



    private AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.e( "BLE", "Advertising onStartSuccess");
        }

        @Override
        public void onStartFailure(int errorCode) {
            //The error code 1 indicates that advertisement data size exceeds 31 bytes which is the specified limit. Try shorter device name (4 REPORT)
            // Error codes: https://developer.android.com/reference/android/bluetooth/le/AdvertiseCallback.html
            Log.e( "BLE", "Advertising onStartFailure: " + errorCode );
            super.onStartFailure(errorCode);
        }
    };

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if( result == null
                    || result.getDevice() == null
                    || TextUtils.isEmpty(result.getDevice().getName()) )
                return;

            StringBuilder builder = new StringBuilder( result.getDevice().getName() );

            builder.append("\n").append(new String(result.getScanRecord().getServiceData(result.getScanRecord().getServiceUuids().get(0)), Charset.forName("UTF-8")));
            Log.d("BLE", "Discovery onScanResult");
            Log.d("BLE", builder.toString());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d("BLE", "Discovery onBatchScanResults");
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e( "BLE", "Discovery onScanFailed: " + errorCode );
            super.onScanFailed(errorCode);
        }

    };

    public BLEClient(Context mContext, Activity parentActivity) {
        this.mContext = mContext;
        this.parentActivity = parentActivity;

        // Initialize Handler
        mHandler = new Handler();

        // Initialize Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            parentActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void startAdvertising() {
        BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        //Advertisement settings, ADVERTISE_MODE_LOW_POWER for low power
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_MODE_BALANCED )
                .setConnectable( false )
                .build();

        ParcelUuid pUuid = new ParcelUuid( UUID.fromString(this.uuid) );

        //Advertisement data
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName( true )
                .addServiceUuid( pUuid )
                .addServiceData( pUuid, "Data".getBytes( Charset.forName( "UTF-8" ) ) )
                .build();

        advertiser.startAdvertising( settings, data, advertisingCallback );

    }

    public void startScan() {
        //Scan for other Bluetooth LE Advertisements

        final BluetoothLeScanner mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        //Scan filters
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid( new ParcelUuid(UUID.fromString( mContext.getString(R.string.ble_uuid ) ) ) )
                .build();

        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add( filter );

        //Scan settings
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY )
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        //Start runnable that stops the BLE Scan after 10s
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothLeScanner.stopScan(mScanCallback);
                Log.d("BLE", "Scan timeout");
            }
        }, 10000);

    }
    // GOOD TUT: https://code.tutsplus.com/tutorials/how-to-advertise-android-as-a-bluetooth-le-peripheral--cms-25426
}


