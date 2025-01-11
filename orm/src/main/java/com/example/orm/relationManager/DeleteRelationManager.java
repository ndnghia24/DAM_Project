package com.example.orm.relationManager;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.example.orm.Utils.OrmReflectionUtils;
import com.example.orm.annotations.Column;
import com.example.orm.annotations.ManyToMany;
import com.example.orm.annotations.ManyToOne;
import com.example.orm.annotations.OneToMany;
import com.example.orm.annotations.OneToOne;
import com.example.orm.config.DatabaseConfig;
import com.example.orm.database.ConnectionManagerFactory;
import com.example.orm.database.IConnectionManager;
import com.example.orm.repositories.EntityRepository;

public class DeleteRelationManager<T, ID> {

    private final IConnectionManager connectionManager;

    public DeleteRelationManager(EntityRepository<T, ID> repository) {
        String dbType = DatabaseConfig.getProperty("database.type");
        this.connectionManager = ConnectionManagerFactory.getConnectionManager(dbType);
    }

    public void checkRelationsBeforeDelete(T entity) throws Exception {
        Class<?> entityClass = entity.getClass();
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(OneToOne.class)) {
                handleOneToOneBeforeDelete(field, entity, entityClass);
            }
            if (field.isAnnotationPresent(ManyToOne.class)) {
                handleManyToOneBeforeDelete(field, entity, entityClass);
            }
            if (field.isAnnotationPresent(OneToMany.class)) {
                handleOneToManyBeforeDelete(field, entity, entityClass);
            }
            if (field.isAnnotationPresent(ManyToMany.class)) {
                handleManyToManyBeforeDelete(field, entity, entityClass);
            }
        }
    }

    private void handleOneToOneBeforeDelete(Field field, T entity, Class<?> entityClass) throws Exception {
        OneToOne ann = field.getAnnotation(OneToOne.class);
        String targetEntityName = ann.targetEntity();
        String mappedBy = ann.mappedBy();
        field.setAccessible(true);
        Object relatedId = field.get(entity);
        if (relatedId == null) return;
        if (targetEntityName == null || targetEntityName.isEmpty()) {
            throw new IllegalArgumentException("\u001B[31m@OneToOne(targetEntity=...) is required when referencing an ID field\u001B[0m");
        }
        if (mappedBy == null || mappedBy.isEmpty()) {
            throw new IllegalArgumentException("\u001B[31m@OneToOne(mappedBy=...) must specify the foreign key field in the target entity\u001B[0m");
        }
    }

    private void handleManyToOneBeforeDelete(Field field, T entity, Class<?> entityClass) throws Exception {
        field.setAccessible(true);
        Object relatedId = field.get(entity);
        if (relatedId == null) return;
    }

    private void handleOneToManyBeforeDelete(Field field, T entity, Class<?> entityClass) throws Exception {
        OneToMany ann = field.getAnnotation(OneToMany.class);
        String mappedBy = ann.mappedBy();
        String targetEntityName = ann.targetEntity();
        field.setAccessible(true);
        Object parentIdValue = field.get(entity);
        if (parentIdValue == null) return;
        if (targetEntityName == null || targetEntityName.isEmpty()) {
            throw new IllegalArgumentException("\u001B[31m@OneToMany(targetEntity=...) is required to locate child rows.\u001B[0m");
        }
        Class<?> childClass = Class.forName(targetEntityName.contains(".")
                ? targetEntityName
                : "com.example.orm.entities." + targetEntityName);
        Field childField = childClass.getDeclaredField(mappedBy);
        if (!childField.isAnnotationPresent(Column.class)) {
            throw new IllegalArgumentException("\u001B[31m@OneToMany(mappedBy=...) field '" + mappedBy + "' in child class "
                    + childClass.getSimpleName() + " must have a @Column annotation.\u001B[0m");
        }
        Column childColumnAnn = childField.getAnnotation(Column.class);
        String childFkColumnName = childColumnAnn.name();
        String childTableName = OrmReflectionUtils.getTableName(childClass);
        String sql = "SELECT COUNT(*) FROM " + childTableName + " WHERE " + childFkColumnName + " = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, parentIdValue);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int childCount = rs.getInt(1);
                    if (childCount > 0) {
                        throw new IllegalStateException("\u001B[31mCannot delete " + entityClass.getSimpleName()
                                + " (ID=" + parentIdValue + ") because " + childCount
                                + " child record(s) exist in " + childClass.getSimpleName() + ".\u001B[0m");
                    }
                }
            }
        }
    }

    private void handleManyToManyBeforeDelete(Field field, T entity, Class<?> entityClass) throws Exception {
        ManyToMany ann = field.getAnnotation(ManyToMany.class);
        String mappedBy = ann.mappedBy();
        String joinTable = ann.joinTable();
        String targetEntityName = ann.targetEntity();
        Object parentIdValue = OrmReflectionUtils.getPrimaryKeyValue(entity);
        if (parentIdValue == null) return;
        if (joinTable == null || joinTable.isEmpty()) {
            throw new IllegalArgumentException("\u001B[31m@ManyToMany(joinTable=...) is required to check references.\u001B[0m");
        }
        if (targetEntityName == null || targetEntityName.isEmpty()) {
            throw new IllegalArgumentException("\u001B[31m@ManyToMany(targetEntity=...) is required to locate child references.\u001B[0m");
        }
        String thisIdColumn = mappedBy;
        if (thisIdColumn == null || thisIdColumn.isEmpty()) {
            throw new IllegalArgumentException("\u001B[31m@ManyToMany(mappedBy=...) is required to define the join-table column referencing this entity.\u001B[0m");
        }
        String sql = "SELECT COUNT(*) FROM " + joinTable + " WHERE " + thisIdColumn + " = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, parentIdValue);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int linkCount = rs.getInt(1);
                    if (linkCount > 0) {
                        throw new IllegalStateException("\u001B[31mCannot delete " + entityClass.getSimpleName()
                                + " (ID=" + parentIdValue + ") because " + linkCount
                                + " link(s) exist in join table '" + joinTable + "' referencing it.\u001B[0m");
                    }
                }
            }
        }
    }
}