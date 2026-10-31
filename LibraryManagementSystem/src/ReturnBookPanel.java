import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for processing book returns and handling overdue loan warnings.
 */
public class ReturnBookPanel extends JPanel {

    private final MainFrame mainFrame;
    private final LoanDAO loanDAO = new LoanDAO();

    private JTextField txtSearch;
    private JTextField txtReturnDate;
    private JTable loanTable;
    private DefaultTableModel tableModel;
    private List<Loan> activeLoansList = new ArrayList<>();

    public ReturnBookPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
        loadActiveLoans();
    }

    private void initUI() {
        // Header
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Return Book");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel lblSubtitle = new JLabel("Check in borrowed books, identify overdue loans, and automatically restore stock availability");
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

        // Toolbar: Search + Return Date + Process Button
        JPanel toolBar = UITheme.createCardPanel();
        toolBar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 6));

        JLabel lblSearch = new JLabel("Filter Active Loans:");
        lblSearch.setFont(UITheme.FONT_BOLD);
        toolBar.add(lblSearch);

        txtSearch = UITheme.createTextField(18);
        txtSearch.setToolTipText("Search by Book Title or Member Name");
        txtSearch.addActionListener(e -> loadActiveLoans());
        toolBar.add(txtSearch);

        JButton btnSearch = UITheme.createPrimaryButton("🔍 Filter");
        btnSearch.addActionListener(e -> loadActiveLoans());
        toolBar.add(btnSearch);

        JButton btnReset = UITheme.createSecondaryButton("Reset");
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            loadActiveLoans();
        });
        toolBar.add(btnReset);

        toolBar.add(Box.createHorizontalStrut(20));

        JLabel lblReturnDate = new JLabel("Return Date:");
        lblReturnDate.setFont(UITheme.FONT_BOLD);
        toolBar.add(lblReturnDate);

        txtReturnDate = UITheme.createTextField(10);
        txtReturnDate.setText(LocalDate.now().toString());
        toolBar.add(txtReturnDate);

        JButton btnProcessReturn = UITheme.createSuccessButton("📥 Process Return");
        btnProcessReturn.setPreferredSize(new Dimension(150, 34));
        btnProcessReturn.addActionListener(e -> processReturn());
        toolBar.add(btnProcessReturn);

        topContainer.add(toolBar);
        add(topContainer, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"Loan ID", "Book Title", "Member Name", "Issue Date", "Due Date", "Current Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        loanTable = new JTable(tableModel);
        loanTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(loanTable);

        // Adjust column widths
        loanTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        loanTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        loanTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        loanTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        loanTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        loanTable.getColumnModel().getColumn(5).setPreferredWidth(180);

        JScrollPane scrollPane = new JScrollPane(loanTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Loads currently active (unreturned) loans.
     */
    public void loadActiveLoans() {
        String query = txtSearch.getText().trim();

        SwingWorker<List<Loan>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Loan> doInBackground() throws Exception {
                return loanDAO.getActiveLoans(query);
            }

            @Override
            protected void done() {
                try {
                    activeLoansList = get();
                    tableModel.setRowCount(0);
                    for (Loan l : activeLoansList) {
                        tableModel.addRow(new Object[]{
                            l.getId(),
                            l.getBookTitle(),
                            l.getMemberName(),
                            l.getIssueDate(),
                            l.getDueDate(),
                            l.getStatus()
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ReturnBookPanel.this, "Error loading active loans: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void processReturn() {
        int selectedRow = loanTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= activeLoansList.size()) {
            JOptionPane.showMessageDialog(this, "Please select an active loan from the table to process its return.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Loan loan = activeLoansList.get(selectedRow);
        String returnDate = txtReturnDate.getText().trim();

        if (returnDate.isEmpty()) {
            returnDate = LocalDate.now().toString();
        }

        try {
            LocalDate.parse(returnDate);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid return date format. Please use YYYY-MM-DD.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check if overdue to show clear warning
        if (loan.isOverdue()) {
            long days = loan.getDaysOverdue();
            int choice = JOptionPane.showConfirmDialog(
                this,
                "⚠️ OVERDUE RETURN NOTICE!\n\n"
                + "• Book: " + loan.getBookTitle() + "\n"
                + "• Member: " + loan.getMemberName() + "\n"
                + "• Due Date: " + loan.getDueDate() + "\n"
                + "• Overdue By: " + days + " days\n\n"
                + "Do you wish to confirm the return of this overdue book?",
                "Overdue Loan Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        } else {
            int choice = JOptionPane.showConfirmDialog(
                this,
                "Confirm return of '" + loan.getBookTitle() + "' by " + loan.getMemberName() + "?",
                "Confirm Return",
                JOptionPane.YES_NO_OPTION
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            loanDAO.returnBook(loan.getId(), returnDate);
            JOptionPane.showMessageDialog(this,
                "Book successfully checked in and returned!\nAvailable copy count has been restored.",
                "Return Processed",
                JOptionPane.INFORMATION_MESSAGE
            );

            // Refresh UI
            loadActiveLoans();
            mainFrame.refreshDashboard();
            mainFrame.refreshBookViews();
            mainFrame.refreshLoanViews();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Return Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
