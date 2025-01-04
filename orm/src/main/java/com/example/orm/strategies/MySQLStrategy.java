package com.example.orm.strategies;

import java.lang.reflect.Field;
import java.util.List;

import com.example.orm.entities.annotations.Column;

public class MySQLStrategy implements DatabaseStrategy {
    @Override
    public String getInsertSQL(String tableName, List<Field> fields) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        StringBuilder updateClause = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            Column column = fields.get(i).getAnnotation(Column.class);
            columns.append(column.name());
            values.append("?");
            updateClause.append(column.name()).append(" = VALUES(").append(column.name()).append(")");
            if (i < fields.size() - 1) {
                columns.append(", ");
                values.append(", ");
                updateClause.append(", ");
            }
        }
        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ") " +
               "ON DUPLICATE KEY UPDATE " + updateClause;
    }

    @Override
    public String formatTableName(String sql) {
        return sql.replaceAll("FROM\\s+(\\w+)", "FROM `$1`");
    }
} 