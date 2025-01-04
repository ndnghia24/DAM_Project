package com.example.orm.strategies;

import java.lang.reflect.Field;
import java.util.List;

import com.example.orm.entities.annotations.Column;

public class PostgreSQLStrategy implements DatabaseStrategy {
    @Override
    public String getInsertSQL(String tableName, List<Field> fields) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        StringBuilder conflictClause = new StringBuilder("ON CONFLICT (");
        for (int i = 0; i < fields.size(); i++) {
            Column column = fields.get(i).getAnnotation(Column.class);
            columns.append(column.name());
            values.append("?");
            if (i < fields.size() - 1) {
                columns.append(", ");
                values.append(", ");
            }
        }
        conflictClause.append(getPrimaryKeyField().getAnnotation(Column.class).name()).append(") DO UPDATE SET ");
        for (int i = 0; i < fields.size(); i++) {
            Column column = fields.get(i).getAnnotation(Column.class);
            conflictClause.append(column.name()).append(" = EXCLUDED.").append(column.name());
            if (i < fields.size() - 1) {
                conflictClause.append(", ");
            }
        }
        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ") " + conflictClause;
    }

    @Override
    public String formatTableName(String sql) {
        return sql.replaceAll("FROM\\s+(\\w+)", "FROM \"$1\"");
    }

    private Field getPrimaryKeyField() {
        return null;
    }
} 