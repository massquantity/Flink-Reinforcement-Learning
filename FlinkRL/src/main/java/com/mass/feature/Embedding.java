package com.mass.feature;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class Embedding {
    private static InputStream embedStream;
    private static JSONObject embedJSON;

    static {
        embedStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("features/embeddings.json");
        try {
            embedJSON = new JSONObject(new JSONTokener(embedStream));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static JSONArray getEmbedding(String feat, int index) {
        JSONArray embeds = embedJSON.getJSONArray(feat);
        return embeds.getJSONArray(index);
    }

    public static void close() throws IOException {
        embedStream.close();
    }
}
