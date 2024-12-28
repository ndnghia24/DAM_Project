package com.example.orm.database;

import com.example.orm.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgreSQLConnectionManager implements IConnectionManager {
    private static Connection connection;
    private final String url;
    private final String username;
    private final String password;

    public PostgreSQLConnectionManager() {
        this.url = DatabaseConfig.getProperty("database.url");
        this.username = DatabaseConfig.getProperty("database.username");
        this.password = DatabaseConfig.getProperty("database.password");
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
        }
        return connection;
    }

    @Override
    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}