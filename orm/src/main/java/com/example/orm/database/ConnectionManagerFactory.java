package com.example.orm.database;

public class ConnectionManagerFactory {
    public static IConnectionManager getConnectionManager(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
                return new MySQLConnectionManager();
            case "postgresql":
                return new PostgreSQLConnectionManager();
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }
}