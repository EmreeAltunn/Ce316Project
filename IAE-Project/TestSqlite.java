import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestSqlite {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:test.db");
        Statement s = conn.createStatement();
        s.execute("CREATE TABLE IF NOT EXISTS test (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO test(name) VALUES(?)", Statement.RETURN_GENERATED_KEYS);
        pstmt.setString(1, "hello");
        pstmt.executeUpdate();
        ResultSet rs = pstmt.getGeneratedKeys();
        if (rs.next()) {
            System.out.println("Generated ID: " + rs.getInt(1));
        }
        conn.close();
    }
}
