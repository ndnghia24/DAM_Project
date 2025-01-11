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

/**
 * This class demonstrates how to check (not create) relationship data before inserting:
 * - Ensures the parent entity has a primary key (if required).
 * - For @OneToOne, @ManyToOne, @OneToMany, and @ManyToMany, it checks each referenced ID
 *   to confirm it already exists in the DB. Otherwise, an exception is thrown.
 *
 * No additional rows are created or updated in related tables.
 */
public class InsertRelationManager<T, ID> {
    private final IConnectionManager connectionManager;

    public InsertRelationManager(EntityRepository<T, ID> repository) {
        String dbType = DatabaseConfig.getProperty("database.type");
        this.connectionManager = ConnectionManagerFactory.getConnectionManager(dbType);
    }

    /**
     * Checks that:
     * 1) The parent entity has a primary key.
     * 2) Each relationship annotation references an existing row (for @ManyToOne, etc.).
     * 3) For @OneToMany and @ManyToMany, that each child ID is valid—no DB insert or update is performed.
     *
     * @param entity The entity to validate before an insert.
     * @throws Exception If any referenced IDs do not exist, or parent ID is missing.
     */
    public void checkInsertRelation(T entity) throws Exception {
        Class<?> entityClass = entity.getClass();

        // 1) Confirm the parent has a primary key
        Object parentId = OrmReflectionUtils.getPrimaryKeyValue(entity);
        if (parentId == null) {
            throw new IllegalStateException(
                "Cannot insert " + entityClass.getSimpleName() 
                + ": The parent entity has no primary key set. Save or assign an ID first."
            );
        }

        // 2) Check each declared field for relationship annotations
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(OneToOne.class)) {
                handleOneToOneCheck(field, entity, entityClass);
            }
            if (field.isAnnotationPresent(ManyToOne.class)) {
                handleManyToOneCheck(field, entity, entityClass);
            }
            if (field.isAnnotationPresent(OneToMany.class)) {
                handleOneToManyCheck(field, entity, entityClass);
            }
            if (field.isAnnotationPresent(ManyToMany.class)) {
                handleManyToManyCheck(field, entity, entityClass);
            }
        }
    }

    /**
     * -------------------------
     * handleOneToOneCheck
     * -------------------------
     * Checks if the referenced ID for @OneToOne is present in the DB.
     */
    private void handleOneToOneCheck(Field field, T entity, Class<?> entityClass) throws Exception {
        OneToOne ann = field.getAnnotation(OneToOne.class);
        String targetEntityName = ann.targetEntity();

        field.setAccessible(true);
        Object relatedId = field.get(entity);
        if (relatedId == null) {
            // No relationship set
            return;
        }

        if (targetEntityName == null || targetEntityName.isEmpty()) {
            throw new IllegalArgumentException(
                "@OneToOne(targetEntity=...) must be specified if storing only an ID"
            );
        }

        Class<?> relatedClass = Class.forName(targetEntityName);
        if (!checkExists(relatedClass, relatedId)) {
            throw new IllegalStateException("OneToOne error: "
                + entityClass.getSimpleName() + " references ID=" + relatedId
                + " in " + relatedClass.getSimpleName()
                + ", but that record does not exist!");
        }
    }

    /**
     * -------------------------
     * handleManyToOneCheck
     * -------------------------
     * Confirms the referenced ID for @ManyToOne exists.
     */
    private void handleManyToOneCheck(Field field, T entity, Class<?> entityClass) throws Exception {
        ManyToOne ann = field.getAnnotation(ManyToOne.class);
        String targetEntityName = ann.targetEntity();

        field.setAccessible(true);
        Object relatedId = field.get(entity);
        if (relatedId == null) {
            // No relationship set
            return;
        }

        if (targetEntityName == null || targetEntityName.isEmpty()) {
            throw new IllegalArgumentException(
                "@ManyToOne(targetEntity=...) must be specified if storing only an ID"
            );
        }

        Class<?> relatedClass = Class.forName(targetEntityName);
        if (!checkExists(relatedClass, relatedId)) {
            throw new IllegalStateException("ManyToOne error: "
                + entityClass.getSimpleName() + " references ID=" + relatedId
                + " in " + relatedClass.getSimpleName()
                + ", but that record does not exist!");
        }
    }

    /**
     * -------------------------
     * handleOneToManyCheck
     * -------------------------
     * 2 scenarios:
     * A) The field is a typical collection (List<Integer>, etc.) of child IDs => we iterate and check each.
     * B) The field is a single scalar (like an int) that also has @OneToMany => we just log or do a basic check.
     */
    private void handleOneToManyCheck(Field field, T entity, Class<?> entityClass) throws Exception {
        OneToMany ann = field.getAnnotation(OneToMany.class);
        String targetEntityName = ann.targetEntity();

        field.setAccessible(true);
        Object fieldValue = field.get(entity);

        if (fieldValue == null) {
            // No children set
            return;
        }

        if (targetEntityName == null || targetEntityName.isEmpty()) {
            throw new RuntimeException(
                "@OneToMany(targetEntity=...) must be specified if storing only IDs"
            );
        }

        // Scenario A: If it's a collection or iterable, we check each ID
        if (fieldValue instanceof Iterable) {
            Class<?> childClass = Class.forName(targetEntityName);
            for (Object childId : (Iterable<?>) fieldValue) {
                if (childId == null) {
                    continue;
                }
                if (!checkExists(childClass, childId)) {
                    throw new IllegalStateException("OneToMany error: ID "
                        + childId + " of " + childClass.getSimpleName()
                        + " does not exist in DB!");
                }
            }
        }
        // Scenario B: If it's a single scalar, e.g. an int field
        else {
            // Possibly do nothing, or add logic if you want to confirm something
            System.out.println("OneToMany field is a single scalar: " + fieldValue 
                + " in " + entityClass.getSimpleName() 
                + ". Typically unusual, but skipping checks...");
        }
    }

    /**
     * -------------------------
     * handleManyToManyCheck
     * -------------------------
     * If the field is a collection of IDs referencing another entity,
     * we confirm each ID is valid in the DB. We do not do any join-table insert.
     */
    private void handleManyToManyCheck(Field field, T entity, Class<?> entityClass) throws Exception {
        ManyToMany ann = field.getAnnotation(ManyToMany.class);
        String targetEntityName = ann.targetEntity();

        field.setAccessible(true);
        Object fieldValue = field.get(entity);
        if (fieldValue == null) {
            return;
        }

        if (!(fieldValue instanceof Iterable)) {
            throw new RuntimeException(
                "@ManyToMany field must be a Collection or Iterable of IDs."
            );
        }

        if (targetEntityName == null || targetEntityName.isEmpty()) {
            throw new RuntimeException(
                "ManyToMany targetEntity is not specified!"
            );
        }

        Class<?> relatedClass = Class.forName(targetEntityName);
        for (Object relatedId : (Iterable<?>) fieldValue) {
            if (relatedId == null) {
                continue;
            }
            if (!checkExists(relatedClass, relatedId)) {
                throw new IllegalStateException("ManyToMany error: ID "
                    + relatedId + " of " + relatedClass.getSimpleName()
                    + " does not exist in DB!");
            }
        }
    }

    /**
     * Utility: checks if a record with the given ID exists in the table
     * for 'clazz', using its @Column(primaryKey=true) field.
     */
    private boolean checkExists(Class<?> clazz, Object id) throws Exception {
        String pkColumn = OrmReflectionUtils.getPrimaryKeyColumnName(clazz);
        String tableName = OrmReflectionUtils.getTableName(clazz);

        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + pkColumn + " = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        }
    }
}