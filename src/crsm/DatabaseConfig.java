package crsm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** JDBC configuration is supplied by the local environment, never committed source. */
public final class DatabaseConfig {
    private DatabaseConfig() {}

    public static Connection openConnection() throws SQLException {
        String url = AppConfig.get("SUPABASE_JDBC_URL");
        String user = AppConfig.get("SUPABASE_DB_USER");
        String password = AppConfig.get("SUPABASE_DB_PASSWORD");
        if (blank(url) || blank(user) || blank(password)) {
            throw new SQLException("Set SUPABASE_JDBC_URL, SUPABASE_DB_USER, and SUPABASE_DB_PASSWORD in .env or the process environment to connect.");
        }
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new SQLException("SUPABASE_JDBC_URL must begin with jdbc:postgresql://");
        }
        return DriverManager.getConnection(url, user, password);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
