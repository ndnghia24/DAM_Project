package com.example.orm.repositories;

import com.example.orm.config.DatabaseConfig;
import com.example.orm.database.ConnectionManagerFactory;
import com.example.orm.database.IConnectionManager;
import com.example.orm.entities.annotations.Column;
import com.example.orm.entities.annotations.Entity;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

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