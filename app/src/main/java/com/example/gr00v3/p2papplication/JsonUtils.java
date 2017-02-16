package com.example.gr00v3.p2papplication;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.R.attr.tag;

/**
 * Created by Gr00v3 on 02/04/2017.
 */

public class JsonUtils {

    public static JSONArray concatArrays(JSONArray arr1, JSONArray arr2) {
        JSONArray result = new JSONArray();
        try {
            for (int i = 0; i < arr1.length(); i++) {
                result.put(arr1.get(i));
            }
            for (int i = 0; i < arr2.length(); i++) {
                result.put(arr2.get(i));
            }
        }
        catch (JSONException e) {
            Log.e("Error", Log.getStackTraceString(e));
        }
        return result;
    }

    public static JSONArray mergeArrays(JSONArray arr1, JSONArray arr2) {
        try {
            for (int i = 0; i < arr1.length(); i++) {
                JSONObject jsonObject = arr1.getJSONObject(i);
                Boolean alreadyFound = false;
                for (int j = 0; j < arr2.length(); j++) {
                    String jsonStr1 = jsonObject.toString().substring(0, 573);
                    String jsonStr2 = arr2.getJSONObject(j).toString().substring(0, 573);

                    if (jsonStr1.equals(jsonStr2)) {
                        alreadyFound = true;
                    }

                }
                if (!alreadyFound) {
                    arr2.put(jsonObject);
                }
            }
        }
        catch (JSONException e) {
            Log.e("Error", Log.getStackTraceString(e));
        }
        return arr2;
    }

    public static Boolean compareObjectsByElement(JSONObject obj1, JSONObject obj2, String tag) {
        String[] tagArray = tag.split("\\.");
        JSONObject compObj1 = new JSONObject();
        JSONObject compObj2 = new JSONObject();
        String compStr1 = "";
        String compStr2 = "";
        try {
            for (int i = 0; i < tagArray.length - 2; i++) {
                obj1 = (JSONObject) obj1.get(tagArray[i]);
                obj2 = (JSONObject) obj2.get(tagArray[i]);
            }
            compStr1 = obj1.get(tagArray[tagArray.length - 1]).toString();
            compStr2 = obj2.get(tagArray[tagArray.length - 1]).toString();
        }
        catch (JSONException e) {
            Log.e("Error", Log.getStackTraceString(e));
        }

        if (compStr1.equals(compStr2)) {
            return true;
        }
        return false;
    }
}
