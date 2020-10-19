package com.mass.map;

import com.mass.entity.RecordEntity;
import com.mass.util.Property;
import com.mass.util.RecordToEntity;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class RecordMap extends RichMapFunction<String, RecordEntity> {
    private static InputStream userStream;
    private static InputStream itemStream;
    private static JSONObject userJSON;
    private static JSONObject itemJSON;
    private static int numUsers;
    private static int numItems;

    @Override
    public void open(Configuration parameters) {
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

    @Override
    public RecordEntity map(String value) {
        RecordEntity record = RecordToEntity.getRecord(value, userJSON, itemJSON, numUsers, numItems);
        return record;
    }

    @Override
    public void close() throws Exception {
        userStream.close();
        itemStream.close();
    }
}

