import crsm.DatabaseConfig;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    public static Connection getConnection() throws SQLException {
        return DatabaseConfig.openConnection();
    }

    public static void main(String[] args) {

        try {
            Connection connection = getConnection();

            System.out.println("Connected to Supabase successfully!");

            connection.close();

        } catch (SQLException e) {

            System.out.println("Database connection failed!");
            System.out.println(e.getMessage());
        }
    }
}
