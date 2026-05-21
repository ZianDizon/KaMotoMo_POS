package com.kamotomo.pos.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // 1. UPDATED: Added security bypass flags for the portable server
    private static final String URL = "jdbc:mysql://localhost:3306/kamotomo_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";

    // 2. UPDATED: Password is now empty because of the '--initialize-insecure' setup
    private static final String PASSWORD = "admin";

    public static Connection getConnection() {
        Connection connection = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found.");
        } catch (SQLException e) {
            System.out.println("Database connection failed. Is MySQL running?");
            e.printStackTrace(); // Added this so if it fails, you can see the exact reason in your terminal
        }
        return connection;
    }
}