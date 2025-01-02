package com.example.orm.entities;

import com.example.orm.entities.annotations.Column;
import com.example.orm.entities.annotations.ColumnAttribute;
import com.example.orm.entities.annotations.Entity;

@Entity(tableName = "Comments")
public class Comment {
    @Column(name = "id", primaryKey = true)
    private int id;

    @Column(name = "postId")
    private int postId;

    @Column(name = "userId")
    private String userId;

    @Column(name = "content")
    private String content;

    // Enum for column names
    public enum Attributes implements ColumnAttribute {
        ID("id"),
        POST_ID("postId"),
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

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
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