package com.mass.util;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
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

    public static String convertEmbedding(List<Double> embeds) {
        String repeat = StringUtils.repeat("\"x\",", embeds.size());
        String columns = "{\"columns\": [" + repeat.substring(0, repeat.length() - 1) + "], ";
        StringBuilder sb = new StringBuilder("\"data\": [[");
        for (double num : embeds) {
            sb.append(String.format("%f,", num));
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]]}");
        return columns + sb.toString();
    }

    public static void main(String[] args) {
        Double[] aa = {1.2, 3.5, 5.666};
        List<Double> bb = Arrays.asList(aa);
        System.out.println(convertEmbedding(bb));
    }
}
