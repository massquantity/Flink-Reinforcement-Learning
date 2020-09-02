package com.mass.util;

import com.mass.entity.RecordEntity;

public class RecordToEntity {
    public static RecordEntity getRecord(String s) {
        String[] values = s.split(",");
        RecordEntity record = new RecordEntity();
        record.setUserId(Integer.parseInt(values[0]));
        record.setItemId(Integer.parseInt(values[1]));
        record.setTime(Long.parseLong(values[2]));
        return record;
    }
}
