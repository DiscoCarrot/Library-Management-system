import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.sql.SQLException;

/**
 * Main application entry point for the Library Management System.
 * Initializes the local SQLite database, sets up system UI theme, and presents the Login Frame.
 */
public class Main {

    public static void main(String[] args) {
        // Optional CLI test trigger
        if (args.length > 0 && ("--test".equalsIgnoreCase(args[0]) || "--headless-test".equalsIgnoreCase(args[0]))) {
            System.out.println("Running automated test suite...");
            SystemTest.main(args);
            return;
        }

        // Set modern native Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to standard cross-platform Swing Look & Feel
        }

        // Initialize SQLite Database and Tables on startup
        try {
            Database.initializeDatabase();
            System.out.println("Library Management System initialized successfully.");
            System.out.println("Local Database path: " + Database.getDatabasePath());
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                null,
                "Failed to initialize local SQLite database:\n" + e.getMessage()
                + "\n\nPlease ensure write permissions are granted to the application directory.",
                "Database Initialization Error",
                JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }

        // Launch Login UI on Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            } catch (Exception e) {
                System.err.println("Fatal error launching GUI: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
