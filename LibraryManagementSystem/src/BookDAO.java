import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for book catalog management and inventory rules.
 */
public class BookDAO {

    /**
     * Checks if a given ISBN is already used by another book.
     * Empty or null ISBNs are not considered duplicate.
     */
    public boolean isIsbnDuplicate(String isbn, int excludeBookId) throws SQLException {
        if (isbn == null || isbn.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM books WHERE UPPER(TRIM(isbn)) = UPPER(TRIM(?)) AND id != ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, isbn.trim());
            pstmt.setInt(2, excludeBookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Adds a new book to the database.
     *
     * @param book Book details
     * @return Generated book ID
     * @throws IllegalArgumentException on business validation failure
     * @throws SQLException on database error
     */
    public int addBook(Book book) throws SQLException {
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Book title is required.");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("Author is required.");
        }
        if (book.getTotalCopies() < 0) {
            throw new IllegalArgumentException("Total copies cannot be negative.");
        }
        if (book.getIsbn() != null && isIsbnDuplicate(book.getIsbn(), 0)) {
            throw new IllegalArgumentException("A book with ISBN '" + book.getIsbn() + "' already exists.");
        }

        String sql = """
            INSERT INTO books (title, author, isbn, category, publication_year, total_copies, active)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, book.getTitle().trim());
            pstmt.setString(2, book.getAuthor().trim());
            if (book.getIsbn() != null && !book.getIsbn().trim().isEmpty()) {
                pstmt.setString(3, book.getIsbn().trim());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            pstmt.setString(4, book.getCategory() != null ? book.getCategory().trim() : null);
            pstmt.setInt(5, book.getPublicationYear());
            pstmt.setInt(6, book.getTotalCopies());
            pstmt.setInt(7, book.isActive() ? 1 : 0);

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    book.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Updates an existing book. Enforces that total copies cannot be reduced below active loans.
     */
    public boolean updateBook(Book book) throws SQLException {
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Book title is required.");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("Author is required.");
        }
        if (book.getTotalCopies() < 0) {
            throw new IllegalArgumentException("Total copies cannot be negative.");
        }

        int activeLoans = getActiveLoansCountForBook(book.getId());
        if (book.getTotalCopies() < activeLoans) {
            throw new IllegalArgumentException("Cannot reduce total copies to " + book.getTotalCopies()
                + ". There are currently " + activeLoans + " copies checked out by members.");
        }

        if (book.getIsbn() != null && isIsbnDuplicate(book.getIsbn(), book.getId())) {
            throw new IllegalArgumentException("A book with ISBN '" + book.getIsbn() + "' already exists.");
        }

        String sql = """
            UPDATE books
            SET title = ?, author = ?, isbn = ?, category = ?, publication_year = ?, total_copies = ?, active = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, book.getTitle().trim());
            pstmt.setString(2, book.getAuthor().trim());
            if (book.getIsbn() != null && !book.getIsbn().trim().isEmpty()) {
                pstmt.setString(3, book.getIsbn().trim());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            pstmt.setString(4, book.getCategory() != null ? book.getCategory().trim() : null);
            pstmt.setInt(5, book.getPublicationYear());
            pstmt.setInt(6, book.getTotalCopies());
            pstmt.setInt(7, book.isActive() ? 1 : 0);
            pstmt.setInt(8, book.getId());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Toggles active status (deactivates or activates a book).
     */
    public boolean setBookActive(int bookId, boolean active) throws SQLException {
        if (!active) {
            int activeLoans = getActiveLoansCountForBook(bookId);
            if (activeLoans > 0) {
                throw new IllegalArgumentException("Cannot deactivate book while " + activeLoans
                    + " copies are currently checked out. Return active copies first.");
            }
        }

        String sql = "UPDATE books SET active = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, active ? 1 : 0);
            pstmt.setInt(2, bookId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Safely deletes a book only if it has NO borrowing history.
     */
    public boolean deleteBook(int bookId) throws SQLException {
        if (hasLoanHistory(bookId)) {
            throw new IllegalArgumentException("Cannot delete book with loan history. Deactivate the book instead to preserve records.");
        }

        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Checks if the book has ever been borrowed (historical loans).
     */
    public boolean hasLoanHistory(int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE book_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Gets the number of currently active loans (unreturned copies) for a book.
     */
    public int getActiveLoansCountForBook(int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE book_id = ? AND return_date IS NULL";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Calculates available copies for a book: total_copies - active_loans.
     */
    public int getAvailableCopies(int bookId) throws SQLException {
        String sql = """
            SELECT (b.total_copies - (SELECT COUNT(*) FROM loans l WHERE l.book_id = b.id AND l.return_date IS NULL))
            FROM books b WHERE b.id = ?
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Retrieves a book by ID with available copies calculated.
     */
    public Book getBookById(int id) throws SQLException {
        String sql = """
            SELECT b.id, b.title, b.author, b.isbn, b.category, b.publication_year, b.total_copies, b.active,
                   (b.total_copies - (SELECT COUNT(*) FROM loans l WHERE l.book_id = b.id AND l.return_date IS NULL)) AS available_copies
            FROM books b
            WHERE b.id = ?
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToBook(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all books.
     */
    public List<Book> getAllBooks(boolean includeInactive) throws SQLException {
        String sql = """
            SELECT b.id, b.title, b.author, b.isbn, b.category, b.publication_year, b.total_copies, b.active,
                   (b.total_copies - (SELECT COUNT(*) FROM loans l WHERE l.book_id = b.id AND l.return_date IS NULL)) AS available_copies
            FROM books b
            WHERE (? = 1 OR b.active = 1)
            ORDER BY b.title COLLATE NOCASE ASC
        """;
        List<Book> books = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, includeInactive ? 1 : 0);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    books.add(mapRowToBook(rs));
                }
            }
        }
        return books;
    }

    /**
     * Searches books by title, author, ISBN, or category.
     */
    public List<Book> searchBooks(String query, boolean includeInactive) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return getAllBooks(includeInactive);
        }

        String searchPattern = "%" + query.trim().toLowerCase() + "%";
        String sql = """
            SELECT b.id, b.title, b.author, b.isbn, b.category, b.publication_year, b.total_copies, b.active,
                   (b.total_copies - (SELECT COUNT(*) FROM loans l WHERE l.book_id = b.id AND l.return_date IS NULL)) AS available_copies
            FROM books b
            WHERE (? = 1 OR b.active = 1)
              AND (LOWER(b.title) LIKE ? OR LOWER(b.author) LIKE ? OR LOWER(COALESCE(b.isbn, '')) LIKE ? OR LOWER(COALESCE(b.category, '')) LIKE ?)
            ORDER BY b.title COLLATE NOCASE ASC
        """;
        List<Book> books = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, includeInactive ? 1 : 0);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            pstmt.setString(4, searchPattern);
            pstmt.setString(5, searchPattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    books.add(mapRowToBook(rs));
                }
            }
        }
        return books;
    }

    /**
     * Returns list of books that are active and have at least 1 copy available.
     */
    public List<Book> getAvailableBooks() throws SQLException {
        String sql = """
            SELECT b.id, b.title, b.author, b.isbn, b.category, b.publication_year, b.total_copies, b.active,
                   (b.total_copies - (SELECT COUNT(*) FROM loans l WHERE l.book_id = b.id AND l.return_date IS NULL)) AS available_copies
            FROM books b
            WHERE b.active = 1
              AND (b.total_copies - (SELECT COUNT(*) FROM loans l WHERE l.book_id = b.id AND l.return_date IS NULL)) > 0
            ORDER BY b.title COLLATE NOCASE ASC
        """;
        List<Book> books = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                books.add(mapRowToBook(rs));
            }
        }
        return books;
    }

    private Book mapRowToBook(ResultSet rs) throws SQLException {
        return new Book(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("isbn"),
            rs.getString("category"),
            rs.getInt("publication_year"),
            rs.getInt("total_copies"),
            rs.getInt("active") == 1,
            rs.getInt("available_copies")
        );
    }
}
