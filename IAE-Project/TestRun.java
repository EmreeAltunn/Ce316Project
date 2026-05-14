import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestRun {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:iae.db");
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO configurations(name, run_command, requires_compilation, source_file_name, file_extension, created_at, updated_at) VALUES('Test2', 'a', 1, 'b', 'c', 'd', 'e')", Statement.RETURN_GENERATED_KEYS);
        pstmt.executeUpdate();
        ResultSet rs = pstmt.getGeneratedKeys();
        if (rs.next()) {
            System.out.println("Generated ID: " + rs.getInt(1));
        } else {
            System.out.println("No generated keys returned!");
        }
        conn.close();
    }
}
