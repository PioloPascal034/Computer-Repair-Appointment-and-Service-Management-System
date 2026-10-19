package crsm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** JDBC configuration is supplied by the local environment, never committed source. */
public final class DatabaseConfig {
    private DatabaseConfig() {}

    public static Connection openConnection() throws SQLException {
        String url = System.getenv("SUPABASE_JDBC_URL");
        String user = System.getenv("SUPABASE_DB_USER");
        String password = System.getenv("SUPABASE_DB_PASSWORD");
        if (blank(url) || blank(user) || blank(password)) {
            throw new SQLException("Set SUPABASE_JDBC_URL, SUPABASE_DB_USER, and SUPABASE_DB_PASSWORD to connect.");
        }
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new SQLException("SUPABASE_JDBC_URL must begin with jdbc:postgresql://");
        }
        return DriverManager.getConnection(url, user, password);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
