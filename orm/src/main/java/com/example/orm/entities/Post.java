package com.example.orm.entities;

import com.example.orm.annotations.Column;
import com.example.orm.annotations.ColumnAttribute;
import com.example.orm.annotations.Entity;
import com.example.orm.annotations.ManyToOne;

@Entity(tableName = "Posts")
public class Post {
    @Column(name = "id", primaryKey = true)
    private int id;

    @Column(name = "userId")
    @ManyToOne(mappedBy = "id", targetEntity = "User")
    private String userId;

    @Column(name = "content")
    private String content;

    // Enum for column names
    public enum Attributes implements ColumnAttribute {
        ID("id"),
        USER_ID("userId"),
        CONTENT("content");

        private final String columnName;

        Attributes(String columnName) {
            this.columnName = columnName;
        }

        @Override
        public String getColumnName() {
            return columnName;
        }
    }

    // getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}