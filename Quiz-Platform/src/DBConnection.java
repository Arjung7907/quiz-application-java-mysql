import java.sql.*;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/quizdb";
    private static final String USER = "root";
    private static final String PASS = "arjun@123"; // change this

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // quick test main (optional)
    public static void main(String[] args) {
        try (Connection c = getConnection()) {
            System.out.println("DB OK");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
