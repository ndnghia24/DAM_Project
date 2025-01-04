package com.example.orm.strategies;

import java.lang.reflect.Field;
import java.util.List;

public interface DatabaseStrategy {
    String getInsertSQL(String tableName, List<Field> fields);
    String formatTableName(String sql);
} 