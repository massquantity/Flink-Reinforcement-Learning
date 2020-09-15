package com.mass.feature;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class Embedding implements Serializable {
    private InputStream embedStream;
    private JSONObject embedJSON;

    public void openFile() {
        embedStream = Embedding.class.getResourceAsStream("/features/embeddings.json");
        try {
            embedJSON = new JSONObject(new JSONTokener(embedStream));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public JSONArray getEmbedding(String feat, int index) {
        JSONArray embeds = embedJSON.getJSONArray(feat);
        return embeds.getJSONArray(index);
    }

    public void close() throws IOException {
        embedStream.close();
    }
}
