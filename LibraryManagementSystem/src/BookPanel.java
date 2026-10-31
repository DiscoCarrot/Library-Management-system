import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Management panel for searching, viewing, adding, editing, and deactivating catalog books.
 */
public class BookPanel extends JPanel {

    private final MainFrame mainFrame;
    private final BookDAO bookDAO = new BookDAO();

    private JTextField txtSearch;
    private JCheckBox chkShowInactive;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private List<Book> currentBooks = new ArrayList<>();

    public BookPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
        loadBooks();
    }

    private void initUI() {
        // Top Section: Title & Search Toolbar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // Header Row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Book Management");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel lblSubtitle = new JLabel("Browse inventory, track available copies, register new titles, and manage records");
        lblSubtitle.setFont(UITheme.FONT_REGULAR);
        lblSubtitle.setForeground(UITheme.TEXT_MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        titleBlock.add(lblTitle);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(lblSubtitle);
        headerRow.add(titleBlock, BorderLayout.WEST);

        // Action Buttons Row (Add, Edit, Deactivate)
        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBtns.setOpaque(false);

        JButton btnAdd = UITheme.createPrimaryButton("➕ Add Book");
        btnAdd.addActionListener(e -> openAddDialog());

        JButton btnEdit = UITheme.createSecondaryButton("✏️ Edit Book");
        btnEdit.addActionListener(e -> openEditDialog());

        JButton btnToggleActive = UITheme.createButton("🔄 Toggle Active", UITheme.COLOR_WARNING);
        btnToggleActive.addActionListener(e -> toggleBookStatus());

        actionBtns.add(btnAdd);
        actionBtns.add(btnEdit);
        actionBtns.add(btnToggleActive);

        headerRow.add(actionBtns, BorderLayout.EAST);
        topContainer.add(headerRow);
        topContainer.add(Box.createVerticalStrut(14));

        // Search Filter Bar
        JPanel searchBar = UITheme.createCardPanel();
        searchBar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 4));

        JLabel lblSearch = new JLabel("Search Catalog:");
        lblSearch.setFont(UITheme.FONT_BOLD);
        searchBar.add(lblSearch);

        txtSearch = UITheme.createTextField(22);
        txtSearch.setToolTipText("Search by Title, Author, ISBN, or Category");
        txtSearch.addActionListener(e -> loadBooks());
        searchBar.add(txtSearch);

        JButton btnSearch = UITheme.createPrimaryButton("🔍 Search");
        btnSearch.addActionListener(e -> loadBooks());
        searchBar.add(btnSearch);

        JButton btnClear = UITheme.createSecondaryButton("Reset");
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            chkShowInactive.setSelected(false);
            loadBooks();
        });
        searchBar.add(btnClear);

        chkShowInactive = new JCheckBox("Include Deactivated Books");
        chkShowInactive.setOpaque(false);
        chkShowInactive.setFont(UITheme.FONT_REGULAR);
        chkShowInactive.addActionListener(e -> loadBooks());
        searchBar.add(chkShowInactive);

        topContainer.add(searchBar);
        add(topContainer, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"ID", "Title", "Author", "ISBN", "Category", "Year", "Total Copies", "Available Copies", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        bookTable = new JTable(tableModel);
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(bookTable);

        // Adjust column widths
        bookTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        bookTable.getColumnModel().getColumn(5).setPreferredWidth(60);
        bookTable.getColumnModel().getColumn(6).setPreferredWidth(85);
        bookTable.getColumnModel().getColumn(7).setPreferredWidth(95);
        bookTable.getColumnModel().getColumn(8).setPreferredWidth(85);

        // Double-click row to edit
        bookTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    openEditDialog();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(bookTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Loads books matching search query asynchronously.
     */
    public void loadBooks() {
        String query = txtSearch.getText().trim();
        boolean showInactive = chkShowInactive.isSelected();

        SwingWorker<List<Book>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Book> doInBackground() throws Exception {
                return bookDAO.searchBooks(query, showInactive);
            }

            @Override
            protected void done() {
                try {
                    currentBooks = get();
                    tableModel.setRowCount(0);
                    for (Book b : currentBooks) {
                        tableModel.addRow(new Object[]{
                            b.getId(),
                            b.getTitle(),
                            b.getAuthor(),
                            b.getIsbn() != null ? b.getIsbn() : "—",
                            b.getCategory() != null ? b.getCategory() : "—",
                            b.getPublicationYear() > 0 ? b.getPublicationYear() : "—",
                            b.getTotalCopies(),
                            b.getAvailableCopies(),
                            b.isActive() ? "Active" : "Deactivated"
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(BookPanel.this, "Error loading books: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private Book getSelectedBook() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < currentBooks.size()) {
            return currentBooks.get(selectedRow);
        }
        return null;
    }

    private void openAddDialog() {
        BookDialog dialog = new BookDialog(mainFrame, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadBooks();
            mainFrame.refreshDashboard();
        }
    }

    private void openEditDialog() {
        Book selected = getSelectedBook();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a book from the table to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BookDialog dialog = new BookDialog(mainFrame, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadBooks();
            mainFrame.refreshDashboard();
        }
    }

    private void toggleBookStatus() {
        Book selected = getSelectedBook();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a book from the table to deactivate or activate.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean willDeactivate = selected.isActive();
        String actionName = willDeactivate ? "deactivate" : "activate";

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to " + actionName + " '" + selected.getTitle() + "'?\n"
            + (willDeactivate ? "Deactivating will preserve loan history but prevent issuing new copies." : "The book will become available for borrowing again."),
            "Confirm " + (willDeactivate ? "Deactivation" : "Activation"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                bookDAO.setBookActive(selected.getId(), !willDeactivate);
                loadBooks();
                mainFrame.refreshDashboard();
                JOptionPane.showMessageDialog(this, "Book has been " + (willDeactivate ? "deactivated" : "activated") + " successfully.");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Deactivate", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
