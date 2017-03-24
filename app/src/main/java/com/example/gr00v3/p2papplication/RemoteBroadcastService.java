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

import java.util.concurrent.ExecutionException;

import static android.R.attr.radius;
import static android.R.attr.value;
import static android.R.attr.x;
import static android.media.CamcorderProfile.get;

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


    public RemoteBroadcastService(MapsActivity activity) {
        this.parentActivity = activity;

        //Initialize BLE client
        //bleClient = new BLEClient(parentActivity.getApplicationContext(), parentActivity);

        //Initialize BT Sockets Client
        bluetoothSocketsClient = new BluetoothSocketsClient(parentActivity, this, new Handler());
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

        parentActivity.reDrawMarkers();
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
                    JSONObject value = MsgJson.getJSONObject("value");
                    radius = value.getDouble("radius");
                    poiType = value.getString("poiType");
                    lng = value.getDouble("lng");
                    lat = value.getDouble("lat");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JSONArray outArray = new JSONArray();

                //Iterate over all JSON-objects in JSONArray and filter, using query parameters
                //TODO: Get POIs from SQLite
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
                    newPoiArray = MsgJson.getJSONObject("value").getJSONArray("poiArray");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                updateInternalPois(newPoiArray);
                break;
            case "KEYREQUEST":
                break;
            case "KEYRESPONSE":
                break;
        }

    }

    //Write a JSON-object over the bluetooth-connection.
    public void writeBT(JSONObject msg, MessageType msgType, BluetoothSocketsClient.ConnectionType connType) {
        JSONObject out = new JSONObject();
        try {
            out.put("type", msgType.name());
            out.put("value", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("RemoteBroadcastService", "Sending message over BT: " + out.toString());
        bluetoothSocketsClient.write(out.toString(), connType);
    }

    public static enum MessageType {
        POIREQUEST,
        POIRESPONSE,
        KEYREQUEST,
        KEYRESPONSE
    }

}

