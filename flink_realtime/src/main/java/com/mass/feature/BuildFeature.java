package com.mass.feature;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BuildFeature {

    public static final String[] staticFeat = {
            "click_environment", "click_deviceGroup", "click_os",
            "click_country", "click_region", "click_referrer_type"};
    public static final String[] dynamicFeat = {"category_id"};
    public static final int histNum = 10;
    public static final int itemEmbedSize = 32;
    public static final int featEmbedSize = 16;
    public static final int totalFeatSize =
            (itemEmbedSize + featEmbedSize * dynamicFeat.length) * histNum + featEmbedSize * staticFeat.length;
    private InputStream userStream;
    private InputStream itemStream;
    private InputStream featStream;
    private JSONObject userJSON;
    private JSONObject itemJSON;
    private JSONObject featJSON;
    private Embedding embeds;
    public int n_users;
    public int n_items;

    public BuildFeature() {
        embeds = new Embedding();
    }

    public void openFile() {
        userStream = BuildFeature.class.getResourceAsStream("/features/user_map.json");
        itemStream = BuildFeature.class.getResourceAsStream("/features/item_map.json");
        featStream = BuildFeature.class.getResourceAsStream("/features/feat_map.json");
        try {
            userJSON = new JSONObject(new JSONTokener(userStream));
            itemJSON = new JSONObject(new JSONTokener(itemStream));
            featJSON = new JSONObject(new JSONTokener(featStream));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        embeds.openFile();
        n_users = userJSON.length();
        n_items = itemJSON.length();
    }

    public int staticFeatIndex(String feat, int userIndex) {
        String user = String.valueOf(userIndex);
        int key = userJSON.has(user) ? userJSON.getInt(user) : n_users;
        JSONObject featJO = new JSONObject(featJSON.getString(feat));
        return featJO.getInt(String.valueOf(key));
    }

    public int dynamicFeatIndex(String feat, int itemIndex) {
        String item = String.valueOf(itemIndex);
        int key = itemJSON.has(item) ? itemJSON.getInt(item) : n_items;
        JSONObject featJO = new JSONObject(featJSON.getString(feat));
        return featJO.getInt(String.valueOf(key));
    }

    public JSONArray getStaticEmbedding(String feat, int userIndex) {
        int index = staticFeatIndex(feat, userIndex);
        return embeds.getEmbedding(feat, index);
    }

    public JSONArray getDynamicEmbedding(String feat, int itemIndex) {
        int index = dynamicFeatIndex(feat, itemIndex);
        return embeds.getEmbedding(feat, index);
    }

    public JSONArray getItemEmbedding(int itemIndex) {
        String item = String.valueOf(itemIndex);
        int index = itemJSON.has(item) ? itemJSON.getInt(item) : n_items;
        return embeds.getEmbedding("item", index);
    }

    public List<Double> getFeatures(int user, List<Integer> items) {
        List<Double> features = new ArrayList<>();
        for (int i : items) {
            JSONArray itemArray = getItemEmbedding(i);
            for (int j = 0; j < itemArray.length(); j++) {
                features.add(itemArray.getDouble(j));
            }

            for (String feat : dynamicFeat) {
                JSONArray dynamicArray = getDynamicEmbedding(feat, i);
                for (int k = 0; k < dynamicArray.length(); k++) {
                    features.add(dynamicArray.getDouble(k));
                }
            }
        }

        for (String feat : staticFeat) {
            JSONArray staticArray = getStaticEmbedding(feat, user);
            for (int j = 0; j < staticArray.length(); j++) {
                features.add(staticArray.getDouble(j));
            }
        }
        return features;
    }


    public void close() throws IOException {
        userStream.close();
        itemStream.close();
        featStream.close();
    }
}
