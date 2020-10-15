package com.mass.feature;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BuildFeature {
    private Boolean withState;
    private static Embedding embeds;
    private static IdConverter idConverter;

    public BuildFeature(Boolean withState, IdConverter idConverter) {
        this.withState = withState;
        BuildFeature.idConverter = idConverter;
        if (withState){
            embeds = new Embedding();
        }
    }

    public int getUserId(int userIndex) {
        return idConverter.getUserId(userIndex);
    }

    public int getItemId(int itemIndex) {
        return idConverter.getItemId(itemIndex);
    }

    private JSONArray getUserEmbedding(int userId) {
    //    int userId = getUserId(userIndex);
        return embeds.getEmbedding("user", userId);
    }

    private JSONArray getItemEmbedding(int itemId) {
    //    int itemId = getItemId(itemIndex);
        return embeds.getEmbedding("item", itemId);
    }

    public List<Float> getEmbedding(int user, List<Integer> items) {
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

    public List<Integer> getSeq(List<Integer> items) {
        List<Integer> seq = new ArrayList<>();
        for (int i: items) {
            int itemId = getItemId(i);
            seq.add(itemId);
        }
        return seq;
    }

    public List<Integer> convertItems(List<Integer> items) {
        return idConverter.convertItems(items);
    }

    public void close() throws IOException {
        if (withState) {
            embeds.close();
        }
        idConverter.close();
    }
}
