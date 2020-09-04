package com.mass.feature;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;

public class Embedding {
    private InputStream embedStream;
    private JSONObject embedJSON;

    public void openFile() {
        embedStream = Embedding.class.getResourceAsStream("/features/embeddigns.json");
        try {
            embedJSON = new JSONObject(new JSONTokener(embedStream));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public JSONArray getEmbedding(String feat, int index) {
        JSONArray embeds = new JSONArray(embedJSON.getString(feat));
        return embeds.getJSONArray(index);
    }

    public void close() throws IOException {
        embedStream.close();
    }
}
