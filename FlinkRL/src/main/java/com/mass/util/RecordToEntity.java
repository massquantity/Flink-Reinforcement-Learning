package com.mass.util;

import com.mass.entity.RecordEntity;
import org.json.JSONObject;

public class RecordToEntity {
    public static RecordEntity getRecord(String s, JSONObject userJSON, JSONObject itemJSON, int numUsers, int numItems) {
        String[] values = s.split(",");
        RecordEntity record = new RecordEntity();
        record.setUserId(userJSON.has(values[0]) ? userJSON.getInt(values[0]) : numUsers);
        record.setItemId(itemJSON.has(values[1]) ? itemJSON.getInt(values[1]) : numItems);
        record.setTime(Long.parseLong(values[2]));
        return record;
    }
}
