package com.example.orm.config;

import java.io.IOException;
import java.util.Properties;

public class DatabaseConfig {
    private static final Properties properties = new Properties();
    
    static {
        try {
            properties.load(DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("database.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Could not load database configuration", e);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}