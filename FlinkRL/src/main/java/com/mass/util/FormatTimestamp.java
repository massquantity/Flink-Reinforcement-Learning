package com.mass.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FormatTimestamp {
    public static String format(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }
}

