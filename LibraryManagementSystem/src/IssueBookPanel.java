import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Panel for issuing books to active library members with real-time borrowing quota validation.
 */
public class IssueBookPanel extends JPanel {

    private final MainFrame mainFrame;
    private final BookDAO bookDAO = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final LoanDAO loanDAO = new LoanDAO();

    private JComboBox<MemberItem> cbMembers;
    private JComboBox<BookItem> cbBooks;
    private JTextField txtIssueDate;
    private JTextField txtDueDate;
    private JLabel lblMemberQuota;
    private JLabel lblBookAvailability;
    private JButton btnIssue;

    public IssueBookPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
        loadDropdownData();
    }

    private void initUI() {
        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Issue Book");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel lblSubtitle = new JLabel("Assign a catalog book to a registered member and establish loan due date");
        lblSubtitle.setFont(UITheme.FONT_REGULAR);
        lblSubtitle.setForeground(UITheme.TEXT_MUTED);

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(lblSubtitle);
        add(headerPanel, BorderLayout.NORTH);

        // Center Form Container
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        centerWrapper.setOpaque(false);

        JPanel formCard = UITheme.createCardPanel();
        formCard.setPreferredSize(new Dimension(680, 460));
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 12, 10, 12);

        // 1. Member Selector
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel lblMember = new JLabel("Select Member *");
        lblMember.setFont(UITheme.FONT_BOLD);
        formCard.add(lblMember, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        cbMembers = new JComboBox<>();
        cbMembers.setFont(UITheme.FONT_REGULAR);
        cbMembers.setPreferredSize(new Dimension(360, 36));
        cbMembers.addActionListener(e -> updateMemberInfo());
        formCard.add(cbMembers, gbc);

        // Member quota feedback
        gbc.gridx = 1; gbc.gridy = 1;
        lblMemberQuota = new JLabel("Select a member to view active loan quota.");
        lblMemberQuota.setFont(UITheme.FONT_SMALL);
        lblMemberQuota.setForeground(UITheme.TEXT_MUTED);
        formCard.add(lblMemberQuota, gbc);

        // 2. Book Selector
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        JLabel lblBook = new JLabel("Select Book *");
        lblBook.setFont(UITheme.FONT_BOLD);
        formCard.add(lblBook, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        cbBooks = new JComboBox<>();
        cbBooks.setFont(UITheme.FONT_REGULAR);
        cbBooks.setPreferredSize(new Dimension(360, 36));
        cbBooks.addActionListener(e -> updateBookInfo());
        formCard.add(cbBooks, gbc);

        // Book availability feedback
        gbc.gridx = 1; gbc.gridy = 3;
        lblBookAvailability = new JLabel("Select a book to view stock availability.");
        lblBookAvailability.setFont(UITheme.FONT_SMALL);
        lblBookAvailability.setForeground(UITheme.TEXT_MUTED);
        formCard.add(lblBookAvailability, gbc);

        // 3. Issue Date
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        JLabel lblIssueDate = new JLabel("Issue Date (YYYY-MM-DD)");
        lblIssueDate.setFont(UITheme.FONT_BOLD);
        formCard.add(lblIssueDate, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtIssueDate = UITheme.createTextField(15);
        txtIssueDate.setText(LocalDate.now().toString());
        formCard.add(txtIssueDate, gbc);

        // 4. Due Date
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.3;
        JLabel lblDueDate = new JLabel("Due Date (YYYY-MM-DD)");
        lblDueDate.setFont(UITheme.FONT_BOLD);
        formCard.add(lblDueDate, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        JPanel duePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        duePanel.setOpaque(false);

        txtDueDate = UITheme.createTextField(12);
        txtDueDate.setText(LocalDate.now().plusDays(14).toString());
        duePanel.add(txtDueDate);

        // Quick date shortcuts
        JButton btn7Days = new JButton("+7 Days");
        btn7Days.setFont(UITheme.FONT_SMALL);
        btn7Days.addActionListener(e -> setDueDateFromIssue(7));

        JButton btn14Days = new JButton("+14 Days");
        btn14Days.setFont(UITheme.FONT_SMALL);
        btn14Days.addActionListener(e -> setDueDateFromIssue(14));

        JButton btn30Days = new JButton("+30 Days");
        btn30Days.setFont(UITheme.FONT_SMALL);
        btn30Days.addActionListener(e -> setDueDateFromIssue(30));

        duePanel.add(btn7Days);
        duePanel.add(btn14Days);
        duePanel.add(btn30Days);
        formCard.add(duePanel, gbc);

        // 5. Submit Button
        gbc.gridx = 1; gbc.gridy = 6;
        gbc.insets = new Insets(18, 12, 10, 12);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);

        btnIssue = UITheme.createPrimaryButton("Confirm & Issue Book");
        btnIssue.setPreferredSize(new Dimension(190, 40));
        btnIssue.addActionListener(e -> issueBook());

        JButton btnRefreshData = UITheme.createSecondaryButton("🔄 Refresh Lists");
        btnRefreshData.addActionListener(e -> loadDropdownData());

        btnPanel.add(btnIssue);
        btnPanel.add(btnRefreshData);
        formCard.add(btnPanel, gbc);

        centerWrapper.add(formCard);
        add(centerWrapper, BorderLayout.CENTER);
    }

    private void setDueDateFromIssue(int days) {
        try {
            LocalDate issue = LocalDate.parse(txtIssueDate.getText().trim());
            txtDueDate.setText(issue.plusDays(days).toString());
        } catch (Exception e) {
            txtDueDate.setText(LocalDate.now().plusDays(days).toString());
        }
    }

    private void updateMemberInfo() {
        MemberItem item = (MemberItem) cbMembers.getSelectedItem();
        if (item == null) {
            lblMemberQuota.setText("");
            return;
        }

        if (!item.member.isActive()) {
            lblMemberQuota.setText("⚠️ This member is INACTIVE and cannot borrow books.");
            lblMemberQuota.setForeground(UITheme.COLOR_DANGER);
            btnIssue.setEnabled(false);
            return;
        }

        int activeLoans = item.member.getActiveLoansCount();
        if (activeLoans >= LoanDAO.MAX_BORROW_LIMIT) {
            lblMemberQuota.setText("⚠️ Member has borrowed " + activeLoans + "/3 books (MAXIMUM LIMIT REACHED).");
            lblMemberQuota.setForeground(UITheme.COLOR_DANGER);
            btnIssue.setEnabled(false);
        } else {
            lblMemberQuota.setText("✓ Current active loans: " + activeLoans + "/3 (Eligible to borrow)");
            lblMemberQuota.setForeground(UITheme.COLOR_SUCCESS);
            btnIssue.setEnabled(true);
        }
    }

    private void updateBookInfo() {
        BookItem item = (BookItem) cbBooks.getSelectedItem();
        if (item == null) {
            lblBookAvailability.setText("");
            return;
        }

        int avail = item.book.getAvailableCopies();
        if (avail <= 0) {
            lblBookAvailability.setText("⚠️ All copies of this book are currently borrowed (0 copies available).");
            lblBookAvailability.setForeground(UITheme.COLOR_DANGER);
            btnIssue.setEnabled(false);
        } else {
            lblBookAvailability.setText("✓ " + avail + " of " + item.book.getTotalCopies() + " physical copies available.");
            lblBookAvailability.setForeground(UITheme.COLOR_SUCCESS);
            updateMemberInfo(); // re-check member eligibility
        }
    }

    /**
     * Loads active members and available books into dropdowns.
     */
    public void loadDropdownData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Member> members;
            private List<Book> books;

            @Override
            protected Void doInBackground() throws Exception {
                members = memberDAO.getAllMembers(false); // active only
                books = bookDAO.getAllBooks(false); // active books
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    DefaultComboBoxModel<MemberItem> memberModel = new DefaultComboBoxModel<>();
                    for (Member m : members) {
                        memberModel.addElement(new MemberItem(m));
                    }
                    cbMembers.setModel(memberModel);

                    DefaultComboBoxModel<BookItem> bookModel = new DefaultComboBoxModel<>();
                    for (Book b : books) {
                        bookModel.addElement(new BookItem(b));
                    }
                    cbBooks.setModel(bookModel);

                    updateMemberInfo();
                    updateBookInfo();
                } catch (Exception e) {
                    System.err.println("Error populating issue dropdowns: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void issueBook() {
        MemberItem memberItem = (MemberItem) cbMembers.getSelectedItem();
        BookItem bookItem = (BookItem) cbBooks.getSelectedItem();
        String issueDate = txtIssueDate.getText().trim();
        String dueDate = txtDueDate.getText().trim();

        if (memberItem == null) {
            JOptionPane.showMessageDialog(this, "Please select a member.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (bookItem == null) {
            JOptionPane.showMessageDialog(this, "Please select a book.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (issueDate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an issue date.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (dueDate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a due date.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            LocalDate start = LocalDate.parse(issueDate);
            LocalDate end = LocalDate.parse(dueDate);
            if (end.isBefore(start)) {
                JOptionPane.showMessageDialog(this, "Due date cannot be earlier than issue date.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int loanId = loanDAO.issueBook(bookItem.book.getId(), memberItem.member.getId(), issueDate, dueDate);
            JOptionPane.showMessageDialog(this,
                "Book successfully issued!\n\n"
                + "• Loan ID: " + loanId + "\n"
                + "• Book: " + bookItem.book.getTitle() + "\n"
                + "• Member: " + memberItem.member.getName() + "\n"
                + "• Issue Date: " + issueDate + "\n"
                + "• Due Date: " + dueDate,
                "Transaction Success",
                JOptionPane.INFORMATION_MESSAGE
            );

            // Refresh UI
            loadDropdownData();
            mainFrame.refreshDashboard();
            mainFrame.refreshLoanViews();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Transaction Rejected", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helper wrappers for ComboBox presentation
    private static class MemberItem {
        final Member member;
        MemberItem(Member member) { this.member = member; }
        @Override
        public String toString() {
            return member.getName() + " (ID: " + member.getId() + ") — " + member.getActiveLoansCount() + "/3 active";
        }
    }

    private static class BookItem {
        final Book book;
        BookItem(Book book) { this.book = book; }
        @Override
        public String toString() {
            return book.getTitle() + " — (" + book.getAvailableCopies() + " available)";
        }
    }
}
