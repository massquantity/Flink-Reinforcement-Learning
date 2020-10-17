package com.mass.feature;

import com.mass.util.Property;
import com.typesafe.config.ConfigException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class IdConverter {
    private static InputStream userStream;
    private static InputStream itemStream;
    private static JSONObject userJSON;
    private static JSONObject itemJSON;
    private static int numUsers;
    private static int numItems;

    static {
        try {
            userStream = new FileInputStream(Property.getStrValue("user_map"));
            itemStream = new FileInputStream(Property.getStrValue("item_map"));
            userJSON = new JSONObject(new JSONTokener(userStream));
            itemJSON = new JSONObject(new JSONTokener(itemStream));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        numUsers = userJSON.length();
        numItems = itemJSON.length();
    }

    public static int getUserId(int userIndex) {
        String user = String.valueOf(userIndex);
        return userJSON.has(user) ? userJSON.getInt(user) : numUsers;
    }

    public static int getItemId(int itemIndex) {
        String item = String.valueOf(itemIndex);
        return itemJSON.has(item) ? itemJSON.getInt(item) : numItems;
    }

    public static List<Integer> convertItems(List<Integer> items) {
        List<Integer> converted = new ArrayList<>();
        for (int i : items) {
            converted.add(getItemId(i));
        }
        return converted;
    }

    public static void close() throws IOException {
        userStream.close();
        itemStream.close();
    }
}
