package com.mass.util;

import com.mass.entity.RecordEntity;
import org.json.JSONObject;

public class RecordToEntity {
    public static RecordEntity getRecord(String s, JSONObject userJSON, JSONObject itemJSON) {
        String[] values = s.split(",");
        RecordEntity record = new RecordEntity();
        record.setUserId(userJSON.getInt(values[0]));
        record.setItemId(itemJSON.getInt(values[1]));
        record.setTime(Long.parseLong(values[2]));
        return record;
    }
}
