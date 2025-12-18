package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    public Connection getDBConnection() throws SQLException {
        String jdbcUrl = System.getenv("JDBC_URL");
        String username = System.getenv("USERNAME");
        String password = System.getenv("PASSWORD");

        if (jdbcUrl == null || username == null || password == null) {
            throw new SQLException("Variables d'environnement manquantes");
        }

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver PostgreSQL introuvable", e);
        }

        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}