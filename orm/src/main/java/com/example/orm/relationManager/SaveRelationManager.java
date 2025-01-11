package com.example.orm.relationManager;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.example.orm.Utils.OrmReflectionUtils;
import com.example.orm.annotations.ManyToMany;
import com.example.orm.annotations.ManyToOne;
import com.example.orm.annotations.OneToMany;
import com.example.orm.annotations.OneToOne;
import com.example.orm.config.DatabaseConfig;
import com.example.orm.database.ConnectionManagerFactory;
import com.example.orm.database.IConnectionManager;
import com.example.orm.repositories.EntityRepository;

public class SaveRelationManager<T, ID> {
    private final IConnectionManager connectionManager;

    private static final String GREEN_TEXT = "\u001B[32m";
    private static final String RESET_TEXT = "\u001B[0m";

    public SaveRelationManager(EntityRepository<T, ID> repository) {
        String dbType = DatabaseConfig.getProperty("database.type");
        this.connectionManager = ConnectionManagerFactory.getConnectionManager(dbType);
    }

    public void checkRelationsBeforeSave(T entity) throws Exception {
        Class<?> entityClass = entity.getClass();
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(OneToOne.class)) {
                handleOneToOneField(field, entity, entityClass);
            }
            if (field.isAnnotationPresent(ManyToOne.class)) {
                handleManyToOneField(field, entity, entityClass);
            }
            if (field.isAnnotationPresent(OneToMany.class)) {
                handleOneToManyField(field, entity, entityClass);
            }
            if (field.isAnnotationPresent(ManyToMany.class)) {
                handleManyToManyField(field, entity, entityClass);
            }
        }
    }

    private void handleOneToOneField(Field field, T entity, Class<?> entityClass) throws Exception {
        OneToOne ann = field.getAnnotation(OneToOne.class);
        String targetEntity = ann.targetEntity();
        String mappedBy = ann.mappedBy();
        field.setAccessible(true);
        Object value = field.get(entity);
        if (value == null) return;
        if (targetEntity == null || targetEntity.isEmpty()) {
            throw new IllegalArgumentException(GREEN_TEXT + "@OneToOne(targetEntity=...) is required when storing only ID" + RESET_TEXT);
        }
        if (mappedBy == null || mappedBy.isEmpty()) {
            throw new IllegalArgumentException(GREEN_TEXT + "@OneToOne(mappedBy=...) is required when storing only ID" + RESET_TEXT);
        }
        Class<?> relatedClass = Class.forName(targetEntity);
        boolean exists = checkExists(relatedClass, value);
        if (!exists) {
            throw new IllegalStateException(GREEN_TEXT + "OneToOne error: " + entityClass.getSimpleName() + " references ID=" + value
                    + " of " + relatedClass.getSimpleName() + ", but it doesn't exist in DB!" + RESET_TEXT);
        }
    }

    private void handleManyToOneField(Field field, T entity, Class<?> entityClass) throws Exception {
        ManyToOne ann = field.getAnnotation(ManyToOne.class);
        String targetEntity = ann.targetEntity();
        field.setAccessible(true);
        Object value = field.get(entity);
        if (value == null) return;
        if (targetEntity == null || targetEntity.isEmpty()) {
            throw new IllegalArgumentException(GREEN_TEXT + "@ManyToOne(targetEntity=...) is required when storing only ID" + RESET_TEXT);
        }
        Class<?> relatedClass = Class.forName(targetEntity);
        boolean exists = checkExists(relatedClass, value);
        if (!exists) {
            throw new IllegalStateException(GREEN_TEXT + "ManyToOne error: " + entityClass.getSimpleName() + " references ID=" + value
                    + " of " + relatedClass.getSimpleName() + ", but it doesn't exist in DB!" + RESET_TEXT);
        }
    }

    private void handleOneToManyField(Field field, T entity, Class<?> entityClass) throws Exception {
        OneToMany ann = field.getAnnotation(OneToMany.class);
        String targetEntity = ann.targetEntity();
        String mappedBy = ann.mappedBy();
        field.setAccessible(true);
        Object parentIdValue = field.get(entity);
        if (parentIdValue == null) return;
        if (targetEntity == null || targetEntity.isEmpty()) {
            throw new IllegalArgumentException(GREEN_TEXT + "@OneToMany(targetEntity=...) is required when storing only ID" + RESET_TEXT);
        }
        if (mappedBy == null || mappedBy.isEmpty()) {
            throw new IllegalArgumentException(GREEN_TEXT + "@OneToMany(mappedBy=...) is required to locate child rows" + RESET_TEXT);
        }
        throw new IllegalStateException(GREEN_TEXT + "OneToMany check: Parent " + entityClass.getSimpleName() + " ID=" + parentIdValue
                + " has child references mapped by field '" + mappedBy + "' in " + targetEntity + RESET_TEXT);
    }

    private void handleManyToManyField(Field field, T entity, Class<?> entityClass) throws Exception {
        ManyToMany ann = field.getAnnotation(ManyToMany.class);
        String joinTable = ann.joinTable();
        String targetEntity = ann.targetEntity();
        field.setAccessible(true);
        Object collectionValue = field.get(entity);
        if (collectionValue == null) return;
        if (!(collectionValue instanceof Iterable)) {
            throw new IllegalArgumentException(GREEN_TEXT + "@ManyToMany field must be a Collection or Iterable of IDs." + RESET_TEXT);
        }
        Object thisId = OrmReflectionUtils.getPrimaryKeyValue(entity);
        if (thisId == null) {
            throw new IllegalStateException(GREEN_TEXT + "Entity " + entityClass.getSimpleName() + " does not have a primary key yet." + RESET_TEXT);
        }
        if (joinTable == null || joinTable.isEmpty()) {
            throw new IllegalArgumentException(GREEN_TEXT + "ManyToMany joinTable is not specified!" + RESET_TEXT);
        }
        if (targetEntity == null || targetEntity.isEmpty()) {
            throw new IllegalArgumentException(GREEN_TEXT + "ManyToMany targetEntity is not specified!" + RESET_TEXT);
        }
        for (Object item : (Iterable<?>) collectionValue) {
            if (item == null) continue;
            Class<?> relatedClass = Class.forName(targetEntity);
            boolean exists = checkExists(relatedClass, item);
            if (!exists) {
                throw new IllegalStateException(GREEN_TEXT + "ManyToMany error: ID " + item + " of " + relatedClass.getSimpleName()
                        + " does not exist in DB!" + RESET_TEXT);
            }
        }
    }

    private boolean checkExists(Class<?> entityClass, Object id) throws Exception {
        String pkColumn = OrmReflectionUtils.getPrimaryKeyColumnName(entityClass);
        String tableName = OrmReflectionUtils.getTableName(entityClass);
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + pkColumn + " = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return (rs.getInt(1) > 0);
                }
                return false;
            }
        }
    }
}