package com.example.gr00v3.p2papplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import android.content.pm.PackageManager;
import android.os.Build;
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
import android.widget.Toast;

import static android.R.attr.filter;
import static android.os.Build.VERSION_CODES.M;

/**
 * Created by Gr00v3 on 02/10/2017.
 */

public class BLEClient {

    private final static int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private Context mContext;
    private Activity parentActivity;
    private BluetoothGatt mGatt;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothLeScanner mLEScanner;
    private static final long SCAN_PERIOD = 10000;

    private UUID uuid = UUID.randomUUID();
    //private String uuid = "d5c54f8e-c7b2-430c-8044-8b7be9fb23a1";
    //private String uuid = "d5e78474-fce9-4d8c-ac0f-8941480f9285";


    private AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            ((MapsActivity) parentActivity).debugOnScreen("BLE", "Advertising onStartSuccess");
        }

        @Override
        public void onStartFailure(int errorCode) {
            //The error code 1 indicates that advertisement data size exceeds 31 bytes which is the specified limit. Try shorter device name (4 REPORT)
            // Error codes: https://developer.android.com/reference/android/bluetooth/le/AdvertiseCallback.html
            ((MapsActivity) parentActivity).debugOnScreen("BLE", "Advertising onStartFailure, error code: " + errorCode);
            super.onStartFailure(errorCode);
        }
    };

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            ((MapsActivity) parentActivity).debugOnScreen("BLE", "onScanResult, discovered: " + btDevice.getName());
            connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    parentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());
                            connectToDevice(device);
                        }
                    });
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

            //Reinitialize bluetooth adapter, THIS DOES CURRENTLY NOT WORK AT ALL <--------------------------------------------------------------------------
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }

        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();

        //Initialize scan settings
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        filters = new ArrayList<ScanFilter>();



        /////////////////////// EXPERIMENTAL

        mBluetoothAdapter.setName(android.os.Build.MODEL);

        BluetoothGattServer gattServer = bluetoothManager.openGattServer(mContext, new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
            }
        });

        BluetoothGattService service = new BluetoothGattService(uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        //characteristic.setValue("TESTINATED");
        service.addCharacteristic(characteristic);
        gattServer.addService(service);

        ///////////////////////////////
    }

    public void startAdvertising() {
        BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        //Advertisement settings, ADVERTISE_MODE_LOW_POWER for low power
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_MODE_BALANCED )
                .setConnectable( false )
                .build();

        ParcelUuid pUuid = new ParcelUuid( uuid );

        //Advertisement data
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName( false )
                .addServiceUuid( pUuid )
                .addServiceData( pUuid, "Data".getBytes( Charset.forName( "UTF-8" ) ) )
                .build();

        advertiser.startAdvertising( settings, data, advertisingCallback );
    }

    public void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mLEScanner.stopScan(mScanCallback);

                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    // GOOD TUT: https://code.tutsplus.com/tutorials/how-to-advertise-android-as-a-bluetooth-le-peripheral--cms-25426
    //http://www.truiton.com/2015/04/android-bluetooth-low-energy-ble-example/


    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(parentActivity, false, gattCallback);
            String name = device.getName();
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            // STATES: https://developer.android.com/reference/android/bluetooth/BluetoothProfile.html
            // TODO: here, onConnectionStateChange only calls with status = 133, newState = STATE_DISCONNECTED. Probably the Gatt server is not set up properly. See
            // TODO: EXPERIMENTAL in constructor.
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());



            /////////////EXPERIMENTAL/////////////////
            for (BluetoothGattService service : gatt.getServices()) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    ((MapsActivity) parentActivity).debugOnScreen("BLE", "Characteristic value: " + characteristic.getValue().toString());
                }
            }

            ((MapsActivity) parentActivity).debugOnScreen("BLE", "number of services: " + gatt.getServices().size());





            //////////////////////////////////////////

            //gatt.readCharacteristic(services.get(1).getCharacteristics().get
             //       (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };
}


