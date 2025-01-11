package com.example.orm.entities;

import com.example.orm.annotations.Column;
import com.example.orm.annotations.ColumnAttribute;
import com.example.orm.annotations.Entity;
import com.example.orm.annotations.OneToMany;

@Entity(tableName = "Users")
public class User {
    @Column(name = "id", primaryKey = true)
    private int id;

    @Column(name = "username")
    @OneToMany(mappedBy = "userId", targetEntity = "Comment")
    private String username;

    @Column(name = "password")
    private String password;

    // Enum for column names
    public enum Attributes implements ColumnAttribute {
        ID("id"),
        USERNAME("username"),
        PASSWORD("password");

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}