package com.example.orm.Utils;

import java.lang.reflect.Field;

import com.example.orm.annotations.Column;
import com.example.orm.annotations.Entity;

public class OrmReflectionUtils {

    /**
     * Retrieves the table name from the @Entity(tableName=...).
     *
     * @param clazz The entity class.
     * @return The table name defined by @Entity.
     * @throws IllegalStateException if no @Entity annotation is found.
     */
    public static String getTableName(Class<?> clazz) {
        Entity entityAnn = clazz.getAnnotation(Entity.class);
        if (entityAnn == null) {
            throw new IllegalStateException(
                "No @Entity annotation found on class: " + clazz.getName()
            );
        }
        return entityAnn.tableName();
    }

    /**
     * Finds the field annotated with @Column(..., primaryKey=true).
     *
     * @param clazz The entity class to inspect.
     * @return The Field representing the primary key.
     * @throws IllegalStateException if no primary-key field is found.
     */
    public static Field findPrimaryKeyField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                Column colAnn = field.getAnnotation(Column.class);
                if (colAnn.primaryKey()) {
                    return field;
                }
            }
        }
        throw new IllegalStateException(
            "No primary-key field found in class: " + clazz.getName()
        );
    }

    /**
     * Retrieves the column name of the primary key, found via @Column(primaryKey=true).
     *
     * @param clazz The entity class.
     * @return The name of the primary key column.
     */
    public static String getPrimaryKeyColumnName(Class<?> clazz) {
        Field pkField = findPrimaryKeyField(clazz);
        Column colAnn = pkField.getAnnotation(Column.class);
        return colAnn.name();
    }

    /**
     * Gets the primary-key value from an entity instance at runtime (reflection).
     *
     * @param entity The entity instance whose PK we want.
     * @return The primary-key value.
     * @throws Exception if the primary-key field is inaccessible.
     */
    public static Object getPrimaryKeyValue(Object entity) throws Exception {
        Class<?> clazz = entity.getClass();
        Field pkField = findPrimaryKeyField(clazz);
        pkField.setAccessible(true);
        return pkField.get(entity);
    }
}