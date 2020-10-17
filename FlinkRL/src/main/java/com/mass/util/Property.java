package com.mass.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class Property {
    private static Properties contextProperties;
    private final static String CONFIG_NAME = "config.properties";

    static {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_NAME);
        contextProperties = new Properties();
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(in, StandardCharsets.UTF_8);
            contextProperties.load(inputStreamReader);
        } catch (IOException e) {
            System.err.println("===== Failed to load config file. =====");
            e.printStackTrace();
        }
    }

    public static String getStrValue(String key) {
        return contextProperties.getProperty(key);
    }
}
