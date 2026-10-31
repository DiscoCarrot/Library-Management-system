import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application window for the Library Management System.
 * Hosts navigation sidebar and CardLayout views for Dashboard, Books, Members, Issue, Return, and Loans.
 */
public class MainFrame extends JFrame {

    private final User currentUser;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentCardPanel = new JPanel(cardLayout);

    // Sidebar navigation buttons
    private final Map<String, JButton> navButtons = new HashMap<>();
    private String currentView = "DASHBOARD";

    // Panel instances
    private DashboardPanel dashboardPanel;
    private BookPanel bookPanel;
    private MemberPanel memberPanel;
    private IssueBookPanel issueBookPanel;
    private ReturnBookPanel returnBookPanel;
    private LoanRecordsPanel loanRecordsPanel;

    private JLabel lblStatusText;

    public MainFrame(User user) {
        this.currentUser = user;

        setTitle("Library Management System — Java Mini Project");
        setSize(1180, 780);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        initUI();
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(UITheme.BG_MAIN);
        setContentPane(rootPanel);

        // 1. Left Sidebar
        JPanel sidebar = createSidebar();
        rootPanel.add(sidebar, BorderLayout.WEST);

        // 2. Right Workspace Container (Top bar + Center cards + Bottom status bar)
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setOpaque(false);

        // Top Navigation Header
        JPanel topHeader = createTopHeader();
        workspace.add(topHeader, BorderLayout.NORTH);

        // Center Card Panels
        initCardViews();
        workspace.add(contentCardPanel, BorderLayout.CENTER);

        // Bottom Status Bar
        JPanel statusBar = createStatusBar();
        workspace.add(statusBar, BorderLayout.SOUTH);

        rootPanel.add(workspace, BorderLayout.CENTER);

        // Set initial view
        showView("DASHBOARD");
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(240, getHeight()));
        sidebar.setBackground(UITheme.BG_SIDEBAR);
        sidebar.setLayout(new BorderLayout());

        // Sidebar Brand Header
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new EmptyBorder(24, 20, 24, 20));

        JLabel lblLogo = new JLabel("📚 LMS", SwingConstants.LEFT);
        lblLogo.setFont(UITheme.FONT_TITLE);
        lblLogo.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Library System v1.0", SwingConstants.LEFT);
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(new Color(148, 163, 184));

        brandPanel.add(lblLogo);
        brandPanel.add(Box.createVerticalStrut(4));
        brandPanel.add(lblSub);
        sidebar.add(brandPanel, BorderLayout.NORTH);

        // Sidebar Nav Buttons
        JPanel navList = new JPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setOpaque(false);
        navList.setBorder(new EmptyBorder(8, 12, 8, 12));

        navList.add(createNavButton("DASHBOARD", "📊  Dashboard"));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("BOOKS", "📚  Books"));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("MEMBERS", "👥  Members"));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("ISSUE", "📤  Issue Book"));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("RETURN", "📥  Return Book"));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("LOANS", "📋  Loan Records"));

        sidebar.add(navList, BorderLayout.CENTER);

        // Sidebar Footer (Logout)
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        footerPanel.setBorder(new EmptyBorder(16, 12, 20, 12));

        JButton btnLogout = new JButton("🚪  Sign Out");
        btnLogout.setFont(UITheme.FONT_BOLD);
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setBackground(new Color(239, 68, 68)); // Red #EF4444
        btnLogout.setOpaque(true);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.setPreferredSize(new Dimension(200, 38));
        btnLogout.addActionListener(e -> logout());

        footerPanel.add(btnLogout, BorderLayout.CENTER);
        sidebar.add(footerPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createNavButton(String viewKey, String label) {
        JButton btn = new JButton(label);
        btn.setFont(UITheme.FONT_BOLD);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setForeground(new Color(203, 213, 225));
        btn.setBackground(UITheme.BG_SIDEBAR);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setPreferredSize(new Dimension(216, 42));
        btn.setBorder(new EmptyBorder(10, 14, 10, 14));

        btn.addActionListener(e -> showView(viewKey));
        navButtons.put(viewKey, btn);
        return btn;
    }

    private JPanel createTopHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_LIGHT),
            new EmptyBorder(12, 24, 12, 24)
        ));

        // Left Greeting
        JLabel lblUser = new JLabel("Logged in: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        lblUser.setFont(UITheme.FONT_BOLD);
        lblUser.setForeground(UITheme.TEXT_MAIN);
        header.add(lblUser, BorderLayout.WEST);

        // Right Tools (Date + Demo Data Seeder)
        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightTools.setOpaque(false);

        String dateFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"));
        JLabel lblDate = new JLabel("📅 " + dateFormatted);
        lblDate.setFont(UITheme.FONT_REGULAR);
        lblDate.setForeground(UITheme.TEXT_MUTED);

        JButton btnDemoData = UITheme.createSecondaryButton("⚡ Load Demo Data");
        btnDemoData.setToolTipText("Populate sample catalog and borrowing transactions for demonstrations");
        btnDemoData.addActionListener(e -> loadDemoData());

        rightTools.add(lblDate);
        rightTools.add(btnDemoData);
        header.add(rightTools, BorderLayout.EAST);

        return header;
    }

    private void initCardViews() {
        dashboardPanel = new DashboardPanel(this);
        bookPanel = new BookPanel(this);
        memberPanel = new MemberPanel(this);
        issueBookPanel = new IssueBookPanel(this);
        returnBookPanel = new ReturnBookPanel(this);
        loanRecordsPanel = new LoanRecordsPanel(this);

        contentCardPanel.add(dashboardPanel, "DASHBOARD");
        contentCardPanel.add(bookPanel, "BOOKS");
        contentCardPanel.add(memberPanel, "MEMBERS");
        contentCardPanel.add(issueBookPanel, "ISSUE");
        contentCardPanel.add(returnBookPanel, "RETURN");
        contentCardPanel.add(loanRecordsPanel, "LOANS");
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_LIGHT),
            new EmptyBorder(6, 20, 6, 20)
        ));

        lblStatusText = new JLabel("Ready  |  Database: " + Database.getDatabasePath() + "  |  Foreign Keys: ON");
        lblStatusText.setFont(UITheme.FONT_SMALL);
        lblStatusText.setForeground(UITheme.TEXT_MUTED);

        JLabel lblCopyright = new JLabel("Java Swing + SQLite Mini Project");
        lblCopyright.setFont(UITheme.FONT_SMALL);
        lblCopyright.setForeground(UITheme.TEXT_MUTED);

        bar.add(lblStatusText, BorderLayout.WEST);
        bar.add(lblCopyright, BorderLayout.EAST);
        return bar;
    }

    /**
     * Switches view card and updates sidebar active button styling.
     */
    public void showView(String viewKey) {
        this.currentView = viewKey;
        cardLayout.show(contentCardPanel, viewKey);

        // Highlight active sidebar item
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton btn = entry.getValue();
            if (entry.getKey().equals(viewKey)) {
                btn.setBackground(UITheme.BG_SIDEBAR_ACTIVE);
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(UITheme.BG_SIDEBAR);
                btn.setForeground(new Color(203, 213, 225));
            }
        }

        // Context-sensitive refreshes
        if ("DASHBOARD".equals(viewKey)) {
            dashboardPanel.refreshStatistics();
        } else if ("BOOKS".equals(viewKey)) {
            bookPanel.loadBooks();
        } else if ("MEMBERS".equals(viewKey)) {
            memberPanel.loadMembers();
        } else if ("ISSUE".equals(viewKey)) {
            issueBookPanel.loadDropdownData();
        } else if ("RETURN".equals(viewKey)) {
            returnBookPanel.loadActiveLoans();
        } else if ("LOANS".equals(viewKey)) {
            loanRecordsPanel.loadLoans();
        }
    }

    public void refreshDashboard() {
        if (dashboardPanel != null) {
            dashboardPanel.refreshStatistics();
        }
    }

    public void refreshBookViews() {
        if (bookPanel != null) {
            bookPanel.loadBooks();
        }
        if (issueBookPanel != null) {
            issueBookPanel.loadDropdownData();
        }
    }

    public void refreshMemberViews() {
        if (memberPanel != null) {
            memberPanel.loadMembers();
        }
        if (issueBookPanel != null) {
            issueBookPanel.loadDropdownData();
        }
    }

    public void refreshLoanViews() {
        if (returnBookPanel != null) {
            returnBookPanel.loadActiveLoans();
        }
        if (loanRecordsPanel != null) {
            loanRecordsPanel.loadLoans();
        }
    }

    private void loadDemoData() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Populate sample library records (books, members, loans) for demonstration?\nExisting records will not be overwritten.",
            "Load Demo Data",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int seeded = SampleDataLoader.loadSampleData();
                refreshDashboard();
                refreshBookViews();
                refreshMemberViews();
                refreshLoanViews();
                JOptionPane.showMessageDialog(this,
                    "Sample demonstration data loaded successfully (" + seeded + " records processed)!",
                    "Demo Data Ready",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this,
                    "Database error while seeding demo data: " + ex.getMessage(),
                    "Seeding Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to sign out?",
            "Confirm Sign Out",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame login = new LoginFrame();
                login.setVisible(true);
            });
        }
    }

    private void confirmExit() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Do you want to exit the Library Management System?\nAll records are saved in the local database.",
            "Exit Application",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }
}
