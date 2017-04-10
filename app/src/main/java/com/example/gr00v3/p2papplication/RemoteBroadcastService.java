package com.example.gr00v3.p2papplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ExecutionException;

import static android.R.attr.radius;
import static android.R.attr.value;
import static android.R.attr.x;
import static android.media.CamcorderProfile.get;
import static org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers.rsaEncryption;

/**
 * Created by Gr00v3 on 02/04/2017.
 */

public class RemoteBroadcastService  {

    // Debugging
    private static final String TAG = "BluetoothChat";

    MapsActivity parentActivity;
    //private BLEClient bleClient;
    private JSONArray poiArray = new JSONArray();

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    BluetoothSocketsClient bluetoothSocketsClient;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    //Encryption
    private RSAEncryption rsaEncryption;

    // Pubkey of BT receiver
    private PublicKey pubKeyThis;
    private PublicKey pubKeyReceiver;

    public RemoteBroadcastService(MapsActivity activity) {
        this.parentActivity = activity;

        //Initialize BLE client
        //bleClient = new BLEClient(parentActivity.getApplicationContext(), parentActivity);

        //Initialize encryption handler
        rsaEncryption = new RSAEncryption();

        //Initialize BT Sockets Client
        bluetoothSocketsClient = new BluetoothSocketsClient(parentActivity, this, new Handler());
        pubKeyThis = bluetoothSocketsClient.getMyPubKey();
    }

    public JSONArray getPoiArray() {
        return poiArray;
    }

    public JSONArray retrievePoisFromGoogleMaps(LatLng point, int radius, String type) {
        String jsonOut = "";
        try {
            LatLng[] pointsArray = {point};
            jsonOut = new RetrievePoisTask(radius, type).execute(pointsArray).get();

        }
        catch (InterruptedException | ExecutionException e) {
            Log.e("Error", Log.getStackTraceString(e));
        }
        JSONArray results = new JSONArray();
        try {

            JSONObject jsonObj = new JSONObject(jsonOut);
            results = jsonObj.getJSONArray("results");
        }
        catch (JSONException e) {
            Log.e("Error", Log.getStackTraceString(e));
        }
        return results;
    }

    public void updateInternalPois(JSONArray arrayIn) {

        try {

            JSONArray arrayOut = new JSONArray();

            //Build Reduced JSON objects, only include name, coordinates and types
            for (int i = 0; i < arrayIn.length(); i++) {
                JSONObject curr = (JSONObject) arrayIn.get(i);
                JSONObject out = new JSONObject();

                JSONObject geometry = new JSONObject();
                geometry.put("location", curr.getJSONObject("geometry").getJSONObject("location"));
                out.put("geometry", geometry);
                out.put("name", curr.get("name"));
                out.put("types", curr.getJSONArray("types"));

                arrayOut.put(out);
            }
            arrayIn = arrayOut;

            JSONArray diffArray = new JSONArray();

            //for each element, check if already added. If not, add to poiArray
            for (int i = 0; i < arrayIn.length(); i++) {
                Boolean alreadyExists = false;

                for (int j = 0; j < poiArray.length(); j++) {
                    //TODO: compare JSONObject in ArrayIn with objets in SQLLite
                    if (JsonUtils.compareObjectsByElement((JSONObject) arrayIn.get(i), (JSONObject) poiArray.get(j), "geometry.location.lat")
                            && JsonUtils.compareObjectsByElement((JSONObject) arrayIn.get(i), (JSONObject) poiArray.get(j), "geometry.location.lng")
                            && JsonUtils.compareObjectsByElement((JSONObject) arrayIn.get(i), (JSONObject) poiArray.get(j), "name")
                            ) {
                        alreadyExists = true;
                        break;
                    }
                }
                if (!alreadyExists) {
                    diffArray.put(arrayIn.get(i));
                }
            }

            //TODO: Add diffArray to SQLite
            poiArray = JsonUtils.concatArrays(poiArray, diffArray);
        }
        catch (JSONException e) {
            Log.e("Error", Log.getStackTraceString(e));
        }

        //If this is the first poi query, simply add all received pois
        if (poiArray.length() == 0) {
            poiArray = JsonUtils.concatArrays(new JSONArray(), arrayIn);
        }
    }

    public void startBTServer() {
        bluetoothSocketsClient.DiscoverPairedDevices();
        bluetoothSocketsClient.DiscoverNewDevices();
        bluetoothSocketsClient.StartServerThread();
    }
    public void stopBTServer() {
        bluetoothSocketsClient.DiscoverPairedDevices();
        bluetoothSocketsClient.DiscoverNewDevices();
        bluetoothSocketsClient.ConnectToPairedDevices();
    }

    //Handling of incoming messages over BT
    public void handleMessage(String msg) {
        Log.d("RemoteBroadcastService", "Received message over BT: " + msg);
        Toast.makeText(parentActivity.getApplicationContext(), "RECEIVED MESSAGE: " + msg, Toast.LENGTH_LONG).show();
        JSONObject MsgJson = null;
        String type = "";
        try {
            MsgJson = new JSONObject(msg);
            type = MsgJson.getString("type");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        switch(type) {
            case "POIREQUEST":
                JSONObject outObj = new JSONObject();

                //Get query params
                double radius = 0;
                String poiType = "";
                double lng = 0;
                double lat = 0;
                try {
                    //JSONObject value = MsgJson.getJSONObject("value");

                    //Verify
                    String valueStr = MsgJson.getString("value");
                    String signStr = MsgJson.getString("signature");
                    if (!rsaEncryption.verify(valueStr, signStr, pubKeyReceiver)) {
                        Log.d("RemoteBroadcastService", "VERIFICATION FAILED");
                        return;
                    }

                    //Decrypt
                    String msgDecr = rsaEncryption.decrypt(MsgJson.getString("value"), rsaEncryption.getPrivKey());
                    Log.d("Encryption Stuff", "DECR; POIRQUEST: " + msgDecr);
                    JSONObject value = new JSONObject(msgDecr);



                    radius = value.getDouble("radius");
                    poiType = value.getString("poiType");
                    lng = value.getDouble("lng");
                    lat = value.getDouble("lat");
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                JSONArray outArray = doInternalQuery(radius, poiType, lat, lng);

                try {
                    outObj.put("poiArray", outArray);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                writeBT(outObj, MessageType.POIRESPONSE,
                        BluetoothSocketsClient.ConnectionType.SERVER);
                break;
            case "POIRESPONSE":
                JSONArray newPoiArray = new JSONArray();
                try {
                    //Verify
                    String valueStr = MsgJson.getString("value");
                    String signStr = MsgJson.getString("signature");
                    if (!rsaEncryption.verify(valueStr, signStr, pubKeyReceiver)) {
                        Log.d("RemoteBroadcastService", "VERIFICATION FAILED");
                        return;
                    }

                    //Decrypt
                    String msgDecr = rsaEncryption.decrypt(MsgJson.getString("value"), rsaEncryption.getPrivKey());
                    Log.d("Encryption Stuff", "DECR; POIREQUEST: " + msgDecr);
                    JSONObject value = new JSONObject(msgDecr);

                    newPoiArray = value.getJSONArray("poiArray");
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                updateInternalPois(newPoiArray);
                parentActivity.drawMarkers(newPoiArray);
                break;
            case "KEYREQUEST":
                //If receivers pubkey is not set, store and send key response
                if ( pubKeyReceiver == null ) {
                    JSONObject keyResponse = new JSONObject();
                    try {
                        pubKeyReceiver = RSAEncryption.buildPublicKeyFromString(MsgJson.getString("value"));
                        keyResponse.put("type", MessageType.KEYRESPONSE.name());
                        keyResponse.put("value", RSAEncryption.getKeyAsString(pubKeyThis));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    }
                    Log.d("RemoteBroadcastService", "SENDING KEYRESPONSE:" + keyResponse.toString());
                    bluetoothSocketsClient.write(keyResponse.toString(), BluetoothSocketsClient.ConnectionType.SERVER);
                }
                break;
            case "KEYRESPONSE":
                //If receivers pubkey is not set, store and do not send anything further
                if ( pubKeyReceiver == null ) {
                    try {
                        pubKeyReceiver = RSAEncryption.buildPublicKeyFromString(MsgJson.getString("value"));
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }

    }

    //Write a JSON-object over the bluetooth-connection.
    public void writeBT(JSONObject msg, MessageType msgType, BluetoothSocketsClient.ConnectionType connType) {

        //Send key request if key not already stored
        if (pubKeyReceiver == null) {
            JSONObject keyRequest = new JSONObject();
            try {
                keyRequest.put("type", RemoteBroadcastService.MessageType.KEYREQUEST.name());
                keyRequest.put("value", RSAEncryption.getKeyAsString(pubKeyThis));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            bluetoothSocketsClient.write(keyRequest.toString(), connType);

            //Withhold sending the message until pubKeyReceiver has been set, inside waitAndSendThread.
            WaitAndSendThread waitAndSendThread = new WaitAndSendThread(msg, msgType, connType);
            waitAndSendThread.start();
            return;
        }

        JSONObject out = new JSONObject();
        try {
            String value = rsaEncryption.encrypt(msg.toString(), pubKeyReceiver);

            out.put("type", msgType.name());
            //out.put("value", msg);
            out.put("value", value);

            // Add signature
            if (msgType == MessageType.POIREQUEST || msgType == MessageType.POIRESPONSE) {
                out.put("signature", rsaEncryption.sign(value, rsaEncryption.getPrivKey()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Log.d("RemoteBroadcastService", "Sending message over BT: " + out.toString());
        bluetoothSocketsClient.write(out.toString(), connType);
    }

    public JSONArray doInternalQuery(double radius, String poiType, double lat, double lng) {

        JSONArray outArray = new JSONArray();

        for (int i = 0; i < poiArray.length(); i++) {
            double latComp = 0;
            double lngComp = 0;
            JSONArray typesComp = new JSONArray();

            JSONObject poi = null;
            try {
                poi = poiArray.getJSONObject(i);

                latComp = poi.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
                lngComp = poi.getJSONObject("geometry").getJSONObject("location").getDouble("lng");
                typesComp = poi.getJSONArray("types");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Check distance
            final double RAD = 0.000008998719243599958;
            if (Math.sqrt(Math.pow(latComp - lat, 2) + Math.pow(lngComp - lng, 2)) / RAD > radius) {
                continue;
            }

            // Check type
            for (int j = 0; j < typesComp.length(); j++) {
                String poiTypeComp = null;
                try {
                    poiTypeComp = typesComp.getString(j);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (poiTypeComp.equals(poiType)) {
                    outArray.put(poi);
                }
            }
        }

        return outArray;

    }

    public static enum MessageType {
        POIREQUEST,
        POIRESPONSE,
        KEYREQUEST,
        KEYRESPONSE
    }

    public class WaitAndSendThread extends Thread {

        private JSONObject msg;
        private MessageType msgType;
        private BluetoothSocketsClient.ConnectionType connType;

        public WaitAndSendThread(JSONObject msg, MessageType msgType, BluetoothSocketsClient.ConnectionType connType) {
            super();
            this.msg = msg;
            this.msgType = msgType;
            this.connType = connType;
        }

        public void run() {
            while (pubKeyReceiver == null) {
                try {
                    this.sleep(10);
                } catch (InterruptedException e) {
                   e.printStackTrace();
                }
            }


            JSONObject out = new JSONObject();
            try {
                out.put("type", msgType.name());
                out.put("value", rsaEncryption.encrypt(msg.toString(), pubKeyReceiver));
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d("RemoteBroadcastService", "Sending message over BT: " + out.toString());
            bluetoothSocketsClient.write(out.toString(), connType);
        }
    }

}

//source: http://coding.westreicher.org/?p=23
//fetched (2017-03-28)
