package com.example.orm.repositories;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.orm.annotations.Column;
import com.example.orm.annotations.ColumnAttribute;
import com.example.orm.annotations.Entity;
import com.example.orm.config.DatabaseConfig;
import com.example.orm.database.ConnectionManagerFactory;
import com.example.orm.database.IConnectionManager;
import com.example.orm.querybuilder.QueryBuilder;
import com.example.orm.querybuilder.SQLCondition;
import com.example.orm.relationManager.DeleteRelationManager;
import com.example.orm.relationManager.InsertRelationManager;
import com.example.orm.relationManager.SaveRelationManager;
import com.example.orm.strategies.DatabaseStrategy;
import com.example.orm.strategies.MySQLStrategy;
import com.example.orm.strategies.PostgreSQLStrategy;

public class EntityRepository<T, ID> implements IRepository<T, ID> {
    private final Class<T> entityClass;
    private final IConnectionManager connectionManager;
    private DatabaseStrategy databaseStrategy = null;
    private final DeleteRelationManager<T, ID> deleteRelationManager = new DeleteRelationManager<>(this);
    private final InsertRelationManager<T, ID> insertRelationManager = new InsertRelationManager<>(this);
    private final SaveRelationManager<T, ID> saveRelationManager = new SaveRelationManager<>(this);
    

    public EntityRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
        String dbType = DatabaseConfig.getProperty("database.type");
        this.connectionManager = ConnectionManagerFactory.getConnectionManager(dbType);
        // based on db type
        if (dbType.equals("postgres")) {
            this.databaseStrategy = new PostgreSQLStrategy();
        } else if (dbType.equals("mysql")) {
            this.databaseStrategy = new MySQLStrategy();
        }
    }

    @Override
    public void save(T entity) throws Exception {
        try {
            saveRelationManager.checkRelationsBeforeSave(entity);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        Entity entityAnnotation = validateEntity();
        String tableName = entityAnnotation.tableName();
        List<Field> fields = new ArrayList<>();
        
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                fields.add(field);
            }
        }

        String sql = databaseStrategy.getInsertSQL(tableName, fields);

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
        try {
            deleteRelationManager.checkRelationsBeforeDelete(entity);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        Entity entityAnnotation = validateEntity();
        String tableName = formatTableName(entityAnnotation.tableName());
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
        String tableName = formatTableName(entityAnnotation.tableName());
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
        String tableName = formatTableName(entityAnnotation.tableName());
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
        IConnectionManager connectionManager = ConnectionManagerFactory.getConnectionManager(DatabaseConfig.getProperty("database.type"));
        
        // strategy based on db type
        DatabaseStrategy strategy;
        if (connectionManager.toString().equals("PostgreSQLConnectionManager")) {
            strategy = new PostgreSQLStrategy();
        } else if (connectionManager.toString().equals("MySQLConnectionManager")) {
            strategy = new MySQLStrategy();
        } else {
            throw new IllegalArgumentException("Unsupported database type: " + connectionManager.toString());
        }

        sql = strategy.formatTableName(sql);

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
            String tableName = formatTableName(entityAnnotation.tableName());
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

    private String formatTableName(String tableName) {
        if (connectionManager.toString().equals("MySQLConnectionManager")) {
            return "`" + tableName + "`";
        } else {
            return "\"" + tableName + "\"";
        }
    }
}