package com.example.orm.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface IConnectionManager {
    Connection getConnection() throws SQLException;
    void closeConnection() throws SQLException;
}