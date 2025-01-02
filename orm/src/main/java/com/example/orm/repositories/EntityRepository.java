package com.example.orm.repositories;

import com.example.orm.config.DatabaseConfig;
import com.example.orm.database.ConnectionManagerFactory;
import com.example.orm.database.IConnectionManager;
import com.example.orm.entities.annotations.Column;
import com.example.orm.entities.annotations.ColumnAttribute;
import com.example.orm.entities.annotations.Entity;
import com.example.orm.querybuilder.QueryBuilder;
import com.example.orm.querybuilder.SQLCondition;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;

public class EntityRepository<T, ID> implements IRepository<T, ID> {
    private final Class<T> entityClass;
    private final IConnectionManager connectionManager;

    public EntityRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
        String dbType = DatabaseConfig.getProperty("database.type");
        this.connectionManager = ConnectionManagerFactory.getConnectionManager(dbType);
    }

    @Override
    public void save(T entity) throws Exception {
        Entity entityAnnotation = validateEntity();
        String tableName = "\"" + entityAnnotation.tableName() + "\"";
        List<Field> fields = new ArrayList<>();
        
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                fields.add(field);
            }
        }

        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            Column column = fields.get(i).getAnnotation(Column.class);
            columns.append(column.name());
            values.append("?");
            if (i < fields.size() - 1) {
                columns.append(", ");
                values.append(", ");
            }
        }

        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ") " +
                    "ON CONFLICT (" + getPrimaryKeyField().getAnnotation(Column.class).name() + ") DO UPDATE SET ";
        
        for (int i = 0; i < fields.size(); i++) {
            Column column = fields.get(i).getAnnotation(Column.class);
            sql += column.name() + " = EXCLUDED." + column.name();
            if (i < fields.size() - 1) {
                sql += ", ";
            }
        }

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                field.setAccessible(true);
                stmt.setObject(i + 1, field.get(entity));
            }
            
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(T entity) throws Exception {
        Entity entityAnnotation = validateEntity();
        String tableName = "\"" + entityAnnotation.tableName() + "\"";
        Field primaryKeyField = getPrimaryKeyField();
        Column pkColumn = primaryKeyField.getAnnotation(Column.class);
        
        String sql = "DELETE FROM " + tableName + " WHERE " + pkColumn.name() + " = ?";
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            primaryKeyField.setAccessible(true);
            stmt.setObject(1, primaryKeyField.get(entity));
            stmt.executeUpdate();
        }
    }

    @Override
    public Optional<T> findById(ID id) throws Exception {
        Entity entityAnnotation = validateEntity();
        String tableName = "\"" + entityAnnotation.tableName() + "\"";
        Field primaryKeyField = getPrimaryKeyField();
        
        String sql = "SELECT * FROM " + tableName + " WHERE " + 
            primaryKeyField.getAnnotation(Column.class).name() + " = ?";
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToEntity(rs));
            }
            return Optional.empty();
        }
    }

    @Override
    public List<T> findAll() throws Exception {
        Entity entityAnnotation = validateEntity();
        String tableName = "\"" + entityAnnotation.tableName() + "\"";
        String sql = "SELECT * FROM " + tableName;
        try (Connection conn = connectionManager.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            return mapResultSetToList(rs);
        }
    }

    // Execute raw query
    public static List<Map<String, Object>> executeRawQuery(String sql) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        IConnectionManager connectionManager = ConnectionManagerFactory.getConnectionManager("postgresql");
        
        // Automatically add double quotes for table names
        sql = sql.replaceAll("FROM\\s+(\\w+)", "FROM \"$1\"");
        
        try (Connection conn = connectionManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
        }
        
        return results;
    }

    public ConditionBuilder<T> findWithConditions() {
        return new ConditionBuilder<>();
    }
    
    public class ConditionBuilder<E> {
        private final QueryBuilder queryBuilder;
    
        public ConditionBuilder() {
            Entity entityAnnotation = validateEntity();
            String tableName = "\"" + entityAnnotation.tableName() + "\"";
            this.queryBuilder = new QueryBuilder().select("*").from(tableName);
        }
    
        public ConditionBuilder<E> where(ColumnAttribute column, String value) {
            queryBuilder.where(column.getColumnName() + " = '" + value + "'");
            return this;
        }
    
        public ConditionBuilder<E> groupBy(ColumnAttribute... columnAttributes) {
            String[] columns = new String[columnAttributes.length];
            for (int i = 0; i < columnAttributes.length; i++) {
                columns[i] = columnAttributes[i].getColumnName();
            }
            queryBuilder.groupBy(columns);
            return this;
        }
    
        public ConditionBuilder<E> having(SQLCondition condition) {
            queryBuilder.having(condition.toString());
            return this;
        }
    
        @SuppressWarnings("unchecked")
        public List<E> execute() throws Exception {
            String sql = queryBuilder.build();
            try (Connection conn = connectionManager.getConnection();
                 Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(sql);
                return (List<E>) mapResultSetToList(rs);
            }
        }
    }

    private Entity validateEntity() {
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        if (entityAnnotation == null) {
            throw new IllegalArgumentException("Class must be annotated with @Entity");
        }
        return entityAnnotation;
    }

    private Field getPrimaryKeyField() {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class) && 
                field.getAnnotation(Column.class).primaryKey()) {
                return field;
            }
        }
        throw new IllegalArgumentException("No primary key field found");
    }

    private T mapResultSetToEntity(ResultSet rs) throws Exception {
        T entity = entityClass.getDeclaredConstructor().newInstance();
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                field.set(entity, rs.getObject(column.name()));
            }
        }
        return entity;
    }

    private List<T> mapResultSetToList(ResultSet rs) throws Exception {
        List<T> entities = new ArrayList<>();
        while (rs.next()) {
            entities.add(mapResultSetToEntity(rs));
        }
        return entities;
    }
}