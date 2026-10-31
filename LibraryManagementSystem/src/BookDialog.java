import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;

/**
 * Modal dialog for adding or editing book records with thorough input validation.
 */
public class BookDialog extends JDialog {

    private final BookDAO bookDAO = new BookDAO();
    private Book book; // null if adding new, existing object if editing
    private boolean saved = false;

    private JTextField txtTitle;
    private JTextField txtAuthor;
    private JTextField txtIsbn;
    private JComboBox<String> cbCategory;
    private JTextField txtYear;
    private JTextField txtCopies;

    public BookDialog(JFrame parent, Book bookToEdit) {
        super(parent, bookToEdit == null ? "Add New Book" : "Edit Book (ID: " + bookToEdit.getId() + ")", true);
        this.book = bookToEdit;

        setSize(460, 480);
        setLocationRelativeTo(parent);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel contentPane = new JPanel(new BorderLayout(12, 12));
        contentPane.setBackground(UITheme.BG_MAIN);
        contentPane.setBorder(new EmptyBorder(16, 20, 16, 20));
        setContentPane(contentPane);

        // Header Title
        JLabel lblHeader = new JLabel(book == null ? "📚 Enter Book Details" : "✏️ Update Book Catalog Record");
        lblHeader.setFont(UITheme.FONT_SUBTITLE);
        lblHeader.setForeground(UITheme.TEXT_MAIN);
        contentPane.add(lblHeader, BorderLayout.NORTH);

        // Form Fields
        JPanel formPanel = UITheme.createCardPanel();
        formPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Row 0: Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(createFieldLabel("Title *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtTitle = UITheme.createTextField(20);
        formPanel.add(txtTitle, gbc);

        // Row 1: Author
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        formPanel.add(createFieldLabel("Author *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtAuthor = UITheme.createTextField(20);
        formPanel.add(txtAuthor, gbc);

        // Row 2: ISBN
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        formPanel.add(createFieldLabel("ISBN"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtIsbn = UITheme.createTextField(20);
        formPanel.add(txtIsbn, gbc);

        // Row 3: Category
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        formPanel.add(createFieldLabel("Category"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        String[] categories = {
            "Computer Science", "Fiction", "Non-Fiction", "Science",
            "Mathematics", "History", "Literature", "Philosophy", "Biography", "Other"
        };
        cbCategory = new JComboBox<>(categories);
        cbCategory.setEditable(true);
        cbCategory.setFont(UITheme.FONT_REGULAR);
        formPanel.add(cbCategory, gbc);

        // Row 4: Publication Year
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        formPanel.add(createFieldLabel("Publication Year"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtYear = UITheme.createTextField(10);
        txtYear.setText(String.valueOf(LocalDate.now().getYear()));
        formPanel.add(txtYear, gbc);

        // Row 5: Total Copies
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.3;
        formPanel.add(createFieldLabel("Total Copies *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtCopies = UITheme.createTextField(10);
        txtCopies.setText("1");
        formPanel.add(txtCopies, gbc);

        contentPane.add(formPanel, BorderLayout.CENTER);

        // Populate fields if editing
        if (book != null) {
            txtTitle.setText(book.getTitle());
            txtAuthor.setText(book.getAuthor());
            txtIsbn.setText(book.getIsbn() != null ? book.getIsbn() : "");
            cbCategory.setSelectedItem(book.getCategory() != null ? book.getCategory() : "Other");
            txtYear.setText(String.valueOf(book.getPublicationYear()));
            txtCopies.setText(String.valueOf(book.getTotalCopies()));
        }

        // Bottom Action Buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomPanel.setOpaque(false);

        JButton btnCancel = UITheme.createSecondaryButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = UITheme.createPrimaryButton(book == null ? "Add Book" : "Update Book");
        btnSave.addActionListener(e -> saveBook());

        bottomPanel.add(btnCancel);
        bottomPanel.add(btnSave);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_MAIN);
        return lbl;
    }

    private void saveBook() {
        String title = txtTitle.getText().trim();
        String author = txtAuthor.getText().trim();
        String isbn = txtIsbn.getText().trim();
        String category = cbCategory.getSelectedItem() != null ? cbCategory.getSelectedItem().toString().trim() : "";
        String yearStr = txtYear.getText().trim();
        String copiesStr = txtCopies.getText().trim();

        // Validation
        if (title.isEmpty()) {
            showError("Book title is required.");
            txtTitle.requestFocusInWindow();
            return;
        }

        if (author.isEmpty()) {
            showError("Author name is required.");
            txtAuthor.requestFocusInWindow();
            return;
        }

        int year = 0;
        if (!yearStr.isEmpty()) {
            try {
                year = Integer.parseInt(yearStr);
                if (year < 1000 || year > LocalDate.now().getYear() + 2) {
                    showError("Please enter a valid 4-digit publication year (e.g. 2024).");
                    txtYear.requestFocusInWindow();
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Publication year must be a numeric integer.");
                txtYear.requestFocusInWindow();
                return;
            }
        }

        int copies;
        try {
            copies = Integer.parseInt(copiesStr);
            if (copies < 0) {
                showError("Total copies cannot be negative.");
                txtCopies.requestFocusInWindow();
                return;
            }
        } catch (NumberFormatException e) {
            showError("Total copies must be a valid integer number.");
            txtCopies.requestFocusInWindow();
            return;
        }

        try {
            if (book == null) {
                // Add new book
                Book newBook = new Book(0, title, author, isbn.isEmpty() ? null : isbn, category, year, copies, true, copies);
                bookDAO.addBook(newBook);
                JOptionPane.showMessageDialog(this, "Book successfully added to catalog!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Update existing book
                book.setTitle(title);
                book.setAuthor(author);
                book.setIsbn(isbn.isEmpty() ? null : isbn);
                book.setCategory(category);
                book.setPublicationYear(year);
                book.setTotalCopies(copies);

                bookDAO.updateBook(book);
                JOptionPane.showMessageDialog(this, "Book details successfully updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }

            saved = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Database error while saving book: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }

    public boolean isSaved() {
        return saved;
    }
}
