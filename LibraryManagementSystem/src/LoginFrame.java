import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

/**
 * Login window for Librarian and Administrator authentication.
 */
public class LoginFrame extends JFrame {

    private final UserDAO userDAO = new UserDAO();
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    public LoginFrame() {
        setTitle("Library Management System — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 520);
        setMinimumSize(new Dimension(420, 480));
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(UITheme.BG_MAIN);
        setContentPane(contentPane);

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(UITheme.BG_SIDEBAR);
        headerPanel.setBorder(new EmptyBorder(24, 20, 24, 20));
        headerPanel.setLayout(new BorderLayout());

        JLabel lblIcon = new JLabel("📚", JLabel.CENTER);
        lblIcon.setFont(new Font("SansSerif", Font.PLAIN, 42));

        JLabel lblTitle = new JLabel("Library Management System", JLabel.CENTER);
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSubtitle = new JLabel("Librarian & Staff Portal", JLabel.CENTER);
        lblSubtitle.setFont(UITheme.FONT_REGULAR);
        lblSubtitle.setForeground(new Color(148, 163, 184));

        JPanel titleBox = new JPanel(new BorderLayout(0, 4));
        titleBox.setOpaque(false);
        titleBox.add(lblIcon, BorderLayout.NORTH);
        titleBox.add(lblTitle, BorderLayout.CENTER);
        titleBox.add(lblSubtitle, BorderLayout.SOUTH);

        headerPanel.add(titleBox, BorderLayout.CENTER);
        contentPane.add(headerPanel, BorderLayout.NORTH);

        // Center Form Panel
        JPanel cardWrapper = new JPanel(new GridBagLayout());
        cardWrapper.setOpaque(false);
        cardWrapper.setBorder(new EmptyBorder(20, 30, 10, 30));

        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(UITheme.FONT_BOLD);
        lblUser.setForeground(UITheme.TEXT_MAIN);
        formCard.add(lblUser, gbc);

        gbc.gridy = 1;
        txtUsername = UITheme.createTextField(15);
        txtUsername.setText("admin"); // Convenience for college demo
        formCard.add(txtUsername, gbc);

        // Password
        gbc.gridy = 2;
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(UITheme.FONT_BOLD);
        lblPass.setForeground(UITheme.TEXT_MAIN);
        formCard.add(lblPass, gbc);

        gbc.gridy = 3;
        txtPassword = new JPasswordField(15);
        txtPassword.setFont(UITheme.FONT_REGULAR);
        txtPassword.setBorder(new CompoundBorder(
            new LineBorder(UITheme.BORDER_LIGHT, 1, true),
            new EmptyBorder(6, 8, 6, 8)
        ));
        txtPassword.setText("admin123"); // Convenience for demo
        formCard.add(txtPassword, gbc);

        // Login Button
        gbc.gridy = 4;
        gbc.insets = new Insets(16, 8, 8, 8);
        btnLogin = UITheme.createPrimaryButton("Sign In");
        btnLogin.setFont(UITheme.FONT_SUBTITLE);
        btnLogin.setPreferredSize(new Dimension(100, 42));
        formCard.add(btnLogin, gbc);

        // Credential Helper Note
        gbc.gridy = 5;
        gbc.insets = new Insets(4, 8, 4, 8);
        JLabel lblNote = new JLabel("<html><center><small style='color:#64748B;'>Default: <b>admin</b> / <b>admin123</b><br>Passwords are secured with PBKDF2 salt hashing.</small></center></html>", JLabel.CENTER);
        formCard.add(lblNote, gbc);

        cardWrapper.add(formCard, new GridBagConstraints());
        contentPane.add(cardWrapper, BorderLayout.CENTER);

        // Event Listeners
        btnLogin.addActionListener(e -> performLogin());
        txtPassword.addActionListener(e -> performLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());

        // Focus
        SwingUtilities.invokeLater(() -> txtPassword.requestFocusInWindow());
    }

    private void performLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your username.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtUsername.requestFocusInWindow();
            return;
        }

        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your password.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtPassword.requestFocusInWindow();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Verifying...");

        try {
            User user = userDAO.authenticate(username, password);
            if (user != null) {
                // Success: Launch Main Application Frame
                dispose();
                SwingUtilities.invokeLater(() -> {
                    MainFrame mainFrame = new MainFrame(user);
                    mainFrame.setVisible(true);
                });
            } else {
                JOptionPane.showMessageDialog(this,
                    "Invalid username or password.\n\nPlease check your credentials and try again.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
                txtPassword.setText("");
                txtPassword.requestFocusInWindow();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Database error while authenticating: " + ex.getMessage(),
                "System Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            btnLogin.setEnabled(true);
            btnLogin.setText("Sign In");
        }
    }
}
