import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;

/**
 * Main dashboard view showing key library inventory and circulation metrics.
 */
public class DashboardPanel extends JPanel {

    private final MainFrame mainFrame;
    private final LoanDAO loanDAO = new LoanDAO();

    // Metric labels
    private JLabel lblBookTitles;
    private JLabel lblTotalCopies;
    private JLabel lblAvailableCopies;
    private JLabel lblMembers;
    private JLabel lblActiveLoans;
    private JLabel lblOverdueLoans;

    // Overdue summary table
    private DefaultTableModel overdueTableModel;
    private JTable overdueTable;

    public DashboardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
        refreshStatistics();
    }

    private void initUI() {
        // Top Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("System Dashboard");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel lblSubtitle = new JLabel("Real-time library statistics, catalog inventory, and circulation health");
        lblSubtitle.setFont(UITheme.FONT_REGULAR);
        lblSubtitle.setForeground(UITheme.TEXT_MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        titleBlock.add(lblTitle);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(lblSubtitle);

        // Quick Actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);

        JButton btnIssue = UITheme.createPrimaryButton("📤 Issue Book");
        btnIssue.addActionListener(e -> mainFrame.showView("ISSUE"));

        JButton btnReturn = UITheme.createSuccessButton("📥 Return Book");
        btnReturn.addActionListener(e -> mainFrame.showView("RETURN"));

        JButton btnRefresh = UITheme.createSecondaryButton("🔄 Refresh");
        btnRefresh.addActionListener(e -> refreshStatistics());

        actionsPanel.add(btnIssue);
        actionsPanel.add(btnReturn);
        actionsPanel.add(btnRefresh);

        topPanel.add(titleBlock, BorderLayout.WEST);
        topPanel.add(actionsPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center Content
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // 1. Stat Metric Cards (2 rows x 3 columns)
        JPanel cardsGrid = new JPanel(new GridLayout(2, 3, 16, 16));
        cardsGrid.setOpaque(false);
        cardsGrid.setMaximumSize(new Dimension(Short.MAX_VALUE, 220));

        lblBookTitles = new JLabel("0", JLabel.LEFT);
        lblTotalCopies = new JLabel("0", JLabel.LEFT);
        lblAvailableCopies = new JLabel("0", JLabel.LEFT);
        lblMembers = new JLabel("0", JLabel.LEFT);
        lblActiveLoans = new JLabel("0", JLabel.LEFT);
        lblOverdueLoans = new JLabel("0", JLabel.LEFT);

        cardsGrid.add(createMetricCard("Book Titles", lblBookTitles, "📚", new Color(37, 99, 235)));       // Blue
        cardsGrid.add(createMetricCard("Total Physical Copies", lblTotalCopies, "📦", new Color(79, 70, 229))); // Indigo
        cardsGrid.add(createMetricCard("Available Copies", lblAvailableCopies, "✅", new Color(5, 150, 105)));   // Emerald
        cardsGrid.add(createMetricCard("Registered Members", lblMembers, "👥", new Color(14, 116, 144)));    // Cyan
        cardsGrid.add(createMetricCard("Active Loans", lblActiveLoans, "🔄", new Color(217, 119, 6)));        // Amber
        cardsGrid.add(createMetricCard("Overdue Loans", lblOverdueLoans, "⚠️", new Color(220, 38, 38)));     // Red

        centerPanel.add(cardsGrid);
        centerPanel.add(Box.createVerticalStrut(20));

        // 2. Overdue Attention Section
        JPanel overdueCard = UITheme.createCardPanel();
        overdueCard.setLayout(new BorderLayout(0, 10));

        JLabel lblOverdueHeader = new JLabel("⚠️ Priority Alerts: Overdue Loans Requiring Return");
        lblOverdueHeader.setFont(UITheme.FONT_SECTION);
        lblOverdueHeader.setForeground(UITheme.COLOR_DANGER);
        overdueCard.add(lblOverdueHeader, BorderLayout.NORTH);

        String[] cols = {"Loan ID", "Book Title", "Member Name", "Issue Date", "Due Date", "Days Overdue"};
        overdueTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        overdueTable = new JTable(overdueTableModel);
        UITheme.styleTable(overdueTable);
        JScrollPane scrollPane = new JScrollPane(overdueTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scrollPane.setPreferredSize(new Dimension(800, 180));

        overdueCard.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(overdueCard);

        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, String icon, Color accentColor) {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(12, 4));

        // Left accent bar
        JPanel bar = new JPanel();
        bar.setBackground(accentColor);
        bar.setPreferredSize(new Dimension(5, 40));
        card.add(bar, BorderLayout.WEST);

        // Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UITheme.FONT_REGULAR);
        lblTitle.setForeground(UITheme.TEXT_MUTED);

        valueLabel.setFont(UITheme.FONT_METRIC);
        valueLabel.setForeground(UITheme.TEXT_MAIN);

        content.add(lblTitle);
        content.add(Box.createVerticalStrut(2));
        content.add(valueLabel);
        card.add(content, BorderLayout.CENTER);

        // Right icon
        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("SansSerif", Font.PLAIN, 28));
        card.add(lblIcon, BorderLayout.EAST);

        return card;
    }

    /**
     * Refreshes the metrics and overdue table from SQLite using SwingWorker.
     */
    public void refreshStatistics() {
        SwingWorker<DashboardStats, Void> worker = new SwingWorker<>() {
            private List<Loan> overdueLoansList;

            @Override
            protected DashboardStats doInBackground() throws Exception {
                DashboardStats stats = loanDAO.getDashboardStatistics();
                overdueLoansList = loanDAO.getLoans("OVERDUE", "");
                return stats;
            }

            @Override
            protected void done() {
                try {
                    DashboardStats stats = get();
                    lblBookTitles.setText(String.valueOf(stats.getTotalBookTitles()));
                    lblTotalCopies.setText(String.valueOf(stats.getTotalPhysicalCopies()));
                    lblAvailableCopies.setText(String.valueOf(stats.getAvailableCopies()));
                    lblMembers.setText(String.valueOf(stats.getTotalMembers()));
                    lblActiveLoans.setText(String.valueOf(stats.getActiveLoans()));
                    lblOverdueLoans.setText(String.valueOf(stats.getOverdueLoans()));

                    // Update overdue table
                    overdueTableModel.setRowCount(0);
                    if (overdueLoansList != null) {
                        for (Loan l : overdueLoansList) {
                            overdueTableModel.addRow(new Object[]{
                                l.getId(),
                                l.getBookTitle(),
                                l.getMemberName(),
                                l.getIssueDate(),
                                l.getDueDate(),
                                l.getDaysOverdue() + " days overdue"
                            });
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error loading dashboard metrics: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
}
