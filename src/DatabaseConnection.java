import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
        "aws-0-ap-northeast-1.pooler.supabase.com";

    private static final String USER = "postgres";

    private static final String PASSWORD =
        "YOUR_DATABASE_PASSWORD";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
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