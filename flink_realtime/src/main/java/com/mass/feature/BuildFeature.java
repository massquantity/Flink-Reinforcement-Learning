package com.mass.feature;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BuildFeature implements Serializable {
    private InputStream userStream;
    private InputStream itemStream;
    private JSONObject userJSON;
    private JSONObject itemJSON;
    private Embedding embeds;
    private int numUsers;
    private int numItems;

    public BuildFeature() {
        embeds = new Embedding();
    }

    public void openFile() {
        userStream = BuildFeature.class.getResourceAsStream("/features/user_map.json");
        itemStream = BuildFeature.class.getResourceAsStream("/features/item_map.json");
        try {
            userJSON = new JSONObject(new JSONTokener(userStream));
            itemJSON = new JSONObject(new JSONTokener(itemStream));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        embeds.openFile();
        numUsers = userJSON.length();
        numItems = itemJSON.length();
    }

    private JSONArray getUserEmbedding(int userIndex) {
        String user = String.valueOf(userIndex);
        int index = userJSON.has(user) ? userJSON.getInt(user) : numUsers;
        return embeds.getEmbedding("user", index);
    }

    private JSONArray getItemEmbedding(int itemIndex) {
        String item = String.valueOf(itemIndex);
        int index = itemJSON.has(item) ? itemJSON.getInt(item) : numItems;
        return embeds.getEmbedding("item", index);
    }

    public List<Float> getFeatures(int user, List<Integer> items) {
        List<Float> features = new ArrayList<>();
        JSONArray userArray = getUserEmbedding(user);
        for (int i = 0; i < userArray.length(); i++) {
            features.add(userArray.getFloat(i));
        }

        for (int i : items) {
            JSONArray itemArray = getItemEmbedding(i);
            for (int j = 0; j < itemArray.length(); j++) {
                features.add(itemArray.getFloat(j));
            }
        }
        return features;
    }

    public int getNumUsers() {
        return numUsers;
    }

    public int getNumItems() {
        return numItems;
    }


    public void close() throws IOException {
        userStream.close();
        itemStream.close();
        embeds.close();
    }
}
