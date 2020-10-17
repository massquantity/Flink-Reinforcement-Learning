package com.mass.feature;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class IdConverter {
    private InputStream userStream;
    private InputStream itemStream;
    private JSONObject userJSON;
    private JSONObject itemJSON;
    private int numUsers;
    private int numItems;

    public IdConverter() {
        userStream = IdConverter.class.getResourceAsStream("/features/user_map.json");
        itemStream = IdConverter.class.getResourceAsStream("/features/item_map.json");
        try {
            userJSON = new JSONObject(new JSONTokener(userStream));
            itemJSON = new JSONObject(new JSONTokener(itemStream));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        numUsers = userJSON.length();
        numItems = itemJSON.length();
    }

    public int getUserId(int userIndex) {
        String user = String.valueOf(userIndex);
        return userJSON.has(user) ? userJSON.getInt(user) : numUsers;
    }

    public int getItemId(int itemIndex) {
        String item = String.valueOf(itemIndex);
        return itemJSON.has(item) ? itemJSON.getInt(item) : numItems;
    }

    public List<Integer> convertItems(List<Integer> items) {
        List<Integer> converted = new ArrayList<>();
        for (int i : items) {
            converted.add(getItemId(i));
        }
        return converted;
    }

    public void close() throws IOException {
        userStream.close();
        itemStream.close();
    }
}
