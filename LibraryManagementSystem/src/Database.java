import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connectivity, schema creation, and initialization.
 * Automatically creates the database file and tables on first launch.
 */
public class Database {

    public static final String DEFAULT_DB_PATH = "data/library.db";
    private static String databasePath = DEFAULT_DB_PATH;

    static {
        try {
            // Load SQLite JDBC Driver class
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL: SQLite JDBC Driver not found in classpath. " + e.getMessage());
        }
    }

    /**
     * Sets a custom database path (useful for testing).
     *
     * @param path File path to the SQLite database
     */
    public static void setDatabasePath(String path) {
        databasePath = path;
    }

    /**
     * Gets the currently configured database file path.
     */
    public static String getDatabasePath() {
        return databasePath;
    }

    /**
     * Returns the JDBC URL for SQLite.
     */
    public static String getDatabaseUrl() {
        return "jdbc:sqlite:" + databasePath;
    }

    /**
     * Obtains a new database connection with SQLite foreign key constraints enabled.
     *
     * @return Open Connection to the SQLite database
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        ensureDataDirectoryExists();
        Connection conn = DriverManager.getConnection(getDatabaseUrl());
        
        // Enforce foreign key constraints on every SQLite connection
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Ensures that the parent directory for the database file exists.
     */
    private static void ensureDataDirectoryExists() {
        if (databasePath != null && !databasePath.startsWith(":memory:")) {
            File dbFile = new File(databasePath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
        }
    }

    /**
     * Initializes the database schema and seeds default administrator if not present.
     */
    public static void initializeDatabase() throws SQLException {
        ensureDataDirectoryExists();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Create 'users' table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL
                );
            """);

            // 2. Create 'books' table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS books (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    author TEXT NOT NULL,
                    isbn TEXT UNIQUE,
                    category TEXT,
                    publication_year INTEGER,
                    total_copies INTEGER NOT NULL CHECK(total_copies >= 0),
                    active INTEGER NOT NULL DEFAULT 1
                );
            """);

            // 3. Create 'members' table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    email TEXT,
                    phone TEXT,
                    registration_date TEXT NOT NULL,
                    active INTEGER NOT NULL DEFAULT 1
                );
            """);

            // 4. Create 'loans' table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS loans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id INTEGER NOT NULL,
                    member_id INTEGER NOT NULL,
                    issue_date TEXT NOT NULL,
                    due_date TEXT NOT NULL,
                    return_date TEXT,
                    FOREIGN KEY (book_id) REFERENCES books(id),
                    FOREIGN KEY (member_id) REFERENCES members(id)
                );
            """);

            // 5. Create performance indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_isbn ON books(isbn);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_loans_book_id ON loans(book_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_loans_member_id ON loans(member_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_loans_return_date ON loans(return_date);");

            // 6. Check and create default administrator account if none exists
            seedDefaultAdminIfMissing(conn);
        }
    }

    /**
     * Seeds the default 'admin' account only if no administrator exists in the database.
     */
    private static void seedDefaultAdminIfMissing(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertAdminSql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertAdminSql)) {
                    pstmt.setString(1, "admin");
                    pstmt.setString(2, PasswordUtil.hashPassword("admin123"));
                    pstmt.setString(3, "ADMIN");
                    pstmt.executeUpdate();
                    System.out.println("Default administrator created: username='admin', password='admin123'");
                }
            }
        }
    }
}
