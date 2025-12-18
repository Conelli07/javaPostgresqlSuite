package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    public Connection getDBConnection() throws SQLException {
        String jdbcUrl = "jdbc:postgresql://localhost:5432/mini_football_db";
        String username = "mini_football_db_manager";
        String password = "123456";

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver PostgreSQL introuvable", e);
        }

        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
