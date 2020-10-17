package com.mass.map;

import com.mass.entity.RecordEntity;
import com.mass.util.RecordToEntity;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;

public class RecordMap extends RichMapFunction<String, RecordEntity> {
    private static InputStream userStream;
    private static InputStream itemStream;
    private static JSONObject userJSON;
    private static JSONObject itemJSON;

    @Override
    public void open(Configuration parameters) {
        userStream = RecordMap.class.getResourceAsStream("/features/user_map.json");
        itemStream = RecordMap.class.getResourceAsStream("/features/item_map.json");
        try {
            userJSON = new JSONObject(new JSONTokener(userStream));
            itemJSON = new JSONObject(new JSONTokener(itemStream));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public RecordEntity map(String value) {
        RecordEntity record = RecordToEntity.getRecord(value, userJSON, itemJSON);
        return record;
    }

    @Override
    public void close() throws Exception {
        userStream.close();
        itemStream.close();
    }
}

