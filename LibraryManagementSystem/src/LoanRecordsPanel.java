import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for viewing complete loan history with status filters (All, Active, Overdue, Returned).
 */
public class LoanRecordsPanel extends JPanel {

    private final MainFrame mainFrame;
    private final LoanDAO loanDAO = new LoanDAO();

    private JComboBox<String> cbFilter;
    private JTextField txtSearch;
    private JTable loanTable;
    private DefaultTableModel tableModel;
    private List<Loan> currentLoans = new ArrayList<>();

    public LoanRecordsPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
        loadLoans();
    }

    private void initUI() {
        // Header
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Loan Records & History");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel lblSubtitle = new JLabel("Audit complete borrowing logs, filter active vs returned books, and review overdue occurrences");
        lblSubtitle.setFont(UITheme.FONT_REGULAR);
        lblSubtitle.setForeground(UITheme.TEXT_MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        titleBlock.add(lblTitle);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(lblSubtitle);
        headerRow.add(titleBlock, BorderLayout.WEST);

        topContainer.add(headerRow);
        topContainer.add(Box.createVerticalStrut(14));

        // Filter & Search Toolbar
        JPanel searchBar = UITheme.createCardPanel();
        searchBar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 6));

        JLabel lblFilter = new JLabel("Status Filter:");
        lblFilter.setFont(UITheme.FONT_BOLD);
        searchBar.add(lblFilter);

        String[] filters = {"All Loans", "Currently Issued", "Overdue", "Returned"};
        cbFilter = new JComboBox<>(filters);
        cbFilter.setFont(UITheme.FONT_REGULAR);
        cbFilter.addActionListener(e -> loadLoans());
        searchBar.add(cbFilter);

        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setFont(UITheme.FONT_BOLD);
        searchBar.add(lblSearch);

        txtSearch = UITheme.createTextField(18);
        txtSearch.setToolTipText("Search by Book Title, Member Name, or Loan ID");
        txtSearch.addActionListener(e -> loadLoans());
        searchBar.add(txtSearch);

        JButton btnSearch = UITheme.createPrimaryButton("🔍 Search");
        btnSearch.addActionListener(e -> loadLoans());
        searchBar.add(btnSearch);

        JButton btnReset = UITheme.createSecondaryButton("Reset");
        btnReset.addActionListener(e -> {
            cbFilter.setSelectedIndex(0);
            txtSearch.setText("");
            loadLoans();
        });
        searchBar.add(btnReset);

        JButton btnRefresh = UITheme.createSecondaryButton("🔄 Refresh");
        btnRefresh.addActionListener(e -> loadLoans());
        searchBar.add(btnRefresh);

        topContainer.add(searchBar);
        add(topContainer, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"Loan ID", "Book Title", "Member Name", "Issue Date", "Due Date", "Return Date", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        loanTable = new JTable(tableModel);
        loanTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(loanTable);

        // Column widths
        loanTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        loanTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        loanTable.getColumnModel().getColumn(2).setPreferredWidth(170);
        loanTable.getColumnModel().getColumn(3).setPreferredWidth(105);
        loanTable.getColumnModel().getColumn(4).setPreferredWidth(105);
        loanTable.getColumnModel().getColumn(5).setPreferredWidth(105);
        loanTable.getColumnModel().getColumn(6).setPreferredWidth(150);

        JScrollPane scrollPane = new JScrollPane(loanTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Loads loans based on selected filter and search text.
     */
    public void loadLoans() {
        int filterIdx = cbFilter.getSelectedIndex();
        String filterKey;
        switch (filterIdx) {
            case 1: filterKey = "ACTIVE"; break;
            case 2: filterKey = "OVERDUE"; break;
            case 3: filterKey = "RETURNED"; break;
            default: filterKey = "ALL"; break;
        }

        String query = txtSearch.getText().trim();

        SwingWorker<List<Loan>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Loan> doInBackground() throws Exception {
                return loanDAO.getLoans(filterKey, query);
            }

            @Override
            protected void done() {
                try {
                    currentLoans = get();
                    tableModel.setRowCount(0);
                    for (Loan l : currentLoans) {
                        tableModel.addRow(new Object[]{
                            l.getId(),
                            l.getBookTitle(),
                            l.getMemberName(),
                            l.getIssueDate(),
                            l.getDueDate(),
                            l.getReturnDate() != null ? l.getReturnDate() : "—",
                            l.getStatus()
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(LoanRecordsPanel.this, "Error loading loan records: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
