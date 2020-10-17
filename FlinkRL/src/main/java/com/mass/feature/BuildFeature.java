package com.mass.feature;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BuildFeature {

    public static int getUserId(int userIndex) {
        return IdConverter.getUserId(userIndex);
    }

    public static int getItemId(int itemIndex) {
        return IdConverter.getItemId(itemIndex);
    }

    private static JSONArray getUserEmbedding(int userId) {
    //    int userId = getUserId(userIndex);
        return Embedding.getEmbedding("user", userId);
    }

    private static JSONArray getItemEmbedding(int itemId) {
    //    int itemId = getItemId(itemIndex);
        return Embedding.getEmbedding("item", itemId);
    }

    public static List<Float> getEmbedding(int user, List<Integer> items) {
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

    public static List<Integer> getSeq(List<Integer> items) {
        List<Integer> seq = new ArrayList<>();
        for (int i: items) {
            int itemId = getItemId(i);
            seq.add(itemId);
        }
        return seq;
    }

    public static List<Integer> convertItems(List<Integer> items) {
        return IdConverter.convertItems(items);
    }

    public static void close(Boolean withState) throws IOException {
        if (withState) {
            Embedding.close();
        }
        IdConverter.close();
    }
}
