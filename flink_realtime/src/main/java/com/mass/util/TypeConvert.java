package com.mass.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class TypeConvert {
    public static String convertString(List<Integer> items) {
        StringBuilder sb = new StringBuilder("{\"columns\": [\"x\"], \"data\": [[[");
        for (Integer item : items) {
            sb.append(String.format("%d,", item));
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]]]}");
        return sb.toString();
    }

    public static JSONObject convertJSON(String message) {
        JSONArray resArray = new JSONArray(message);
        return resArray.getJSONObject(0);
    }
}
