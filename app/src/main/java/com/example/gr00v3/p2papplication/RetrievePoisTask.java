package com.example.gr00v3.p2papplication;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ResourceCursorTreeAdapter;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by Gr00v3 on 02/04/2017.
 */

class RetrievePoisTask extends AsyncTask<LatLng, Void, String> {

    private final double radius;				//radius in m
    private final String type;
    //List of supported types: https://developers.google.com/places/web-service/supported_types

    private final String apiKey = "AIzaSyAgtJjop4Q2hbatmf9gncbcdkolZHos30c";
    private final String USER_AGENT = "Mozilla/5.0";

    public RetrievePoisTask(double radius, String type) {
        super();
        this.radius = radius;
        this.type = type;
    }

    protected String doInBackground(LatLng... points) {
        //Instantiates an http-client and retrieves pois near point

        URL url;
        HttpURLConnection connection = null;
        StringBuffer response = null;
        for (LatLng point : points) {
            String urlStr = buildUrlString(point.latitude, point.longitude);
            try {
                url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                Log.e("Error", Log.getStackTraceString(e));
            }


            try {
                connection.setRequestMethod("GET");
            } catch (ProtocolException e) {
                Log.e("Error", Log.getStackTraceString(e));
            }
            connection.setRequestProperty("User-Agent", USER_AGENT);

            //Get response body

            BufferedReader in;
            response = new StringBuffer();
            try {
                in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                String getLine;


                while ((getLine = in.readLine()) != null) {
                    response.append(getLine);
                }
                in.close();
            } catch (IOException e) {
                Log.e("Error", Log.getStackTraceString(e));
            }
        }
        return response.toString();
    }

    private String buildUrlString(double latF, double longF) {
        String urlStr = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" +
                latF +
                "," +
                longF +
                "&radius=" +
                radius +
                "&type=" +
                type +
                //"&keyword=shalom" +
                "&key=" +
                apiKey;
        return urlStr;
    }

}
