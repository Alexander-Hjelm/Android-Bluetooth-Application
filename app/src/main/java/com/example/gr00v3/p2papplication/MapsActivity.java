package com.example.gr00v3.p2papplication;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import org.json.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private final static int REQUEST_ENABLE_BT = 1;

    private GoogleMap mMap;
    private RemoteBroadcastService remoteBroadcastService;

    //UI elements
    private Button scanButton;
    private Button advertiseButton;
    private TextView debugTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        remoteBroadcastService = new RemoteBroadcastService(this);

        //Buttons
        scanButton = (Button) findViewById(R.id.scan_button);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                remoteBroadcastService.scanBLE();
            }
        });

        advertiseButton = (Button) findViewById(R.id.ad_button);
        advertiseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                remoteBroadcastService.advertiseBLE();
            }
        });

        debugTextView = (TextView) findViewById(R.id.debug_text);
        debugOnScreen("MAIN", "App Started...");
    }

    // Ensures Bluetooth is available on the device and it is enabled. If not,
    // displays a dialog requesting user permission to enable Bluetooth.
    @Override
    protected void onResume() {
        super.onResume();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        // Add a marker in Stockholm and move the camera
        LatLng stockholm = new LatLng(90.3, 18.1);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(stockholm));
        mMap.animateCamera( CameraUpdateFactory.zoomTo( 4.0f ));

    }

    @Override
    public void onMapLongClick(LatLng point) {
        // case: internal, other units, google maps API

        // Get pois from google maps API
        JSONArray newPoiArray = remoteBroadcastService.retrievePoisFromGoogleMaps(point);
        remoteBroadcastService.updateInternalPois(newPoiArray);


        mMap.addMarker(new MarkerOptions()
                .position(point)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.downvote))
                .title("You pressed here"));
    }

    public void reDrawMarkers() {
        //Clear and redraw all markers
        mMap.clear();

        for (int i = 0; i < remoteBroadcastService.getPoiArray().length(); i++) {
            double lat = 0;
            double lng = 0;
            String name = "";
            try {
                JSONObject obj = remoteBroadcastService.getPoiArray().getJSONObject(i);
                JSONObject geometry = obj.getJSONObject("geometry");
                JSONObject location = geometry.getJSONObject("location");
                lat = location.getDouble("lat");
                lng = location.getDouble("lng");
                name = obj.getString("name");
            }
            catch (JSONException e) {
                Log.e("Error", Log.getStackTraceString(e));
            }
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat,lng))
                    .title(name));
        }
    }

    public void debugOnScreen(String tag, String msg) {
        debugTextView.setText(tag + ": " + msg);
    }


    //Sources
    //https://developer.android.com/reference/android/widget/Button.html
    //http://stackoverflow.com/questions/14694119/how-to-add-buttons-at-top-of-map-fragment-api-v2-layout
}
