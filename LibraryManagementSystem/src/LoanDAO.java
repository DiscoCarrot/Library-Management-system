import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for book borrowing transactions, returns, and history tracking.
 * Uses atomic JDBC transactions to maintain data integrity and enforce business rules.
 */
public class LoanDAO {

    public static final int MAX_BORROW_LIMIT = 3;

    /**
     * Issues a book to a member with strict business rule validation inside a JDBC transaction.
     *
     * @param bookId    ID of the book to issue
     * @param memberId  ID of the borrowing member
     * @param issueDate Date of issue (YYYY-MM-DD)
     * @param dueDate   Due date for return (YYYY-MM-DD)
     * @return Generated Loan ID
     * @throws IllegalArgumentException on business rule violation
     * @throws SQLException on database error
     */
    public int issueBook(int bookId, int memberId, String issueDate, String dueDate) throws SQLException {
        if (issueDate == null || issueDate.trim().isEmpty()) {
            issueDate = LocalDate.now().toString();
        }
        if (dueDate == null || dueDate.trim().isEmpty()) {
            dueDate = LocalDate.parse(issueDate).plusDays(14).toString();
        }

        // Validate date order
        try {
            LocalDate start = LocalDate.parse(issueDate);
            LocalDate end = LocalDate.parse(dueDate);
            if (end.isBefore(start)) {
                throw new IllegalArgumentException("Due date (" + dueDate + ") cannot precede issue date (" + issueDate + ").");
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) throw e;
            throw new IllegalArgumentException("Invalid date format. Expected YYYY-MM-DD.");
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Verify Member status
                String memberSql = "SELECT name, active FROM members WHERE id = ?";
                String memberName;
                boolean memberActive;
                try (PreparedStatement pstmt = conn.prepareStatement(memberSql)) {
                    pstmt.setInt(1, memberId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Member ID " + memberId + " not found.");
                        }
                        memberName = rs.getString("name");
                        memberActive = rs.getInt("active") == 1;
                    }
                }

                if (!memberActive) {
                    throw new IllegalArgumentException("Member '" + memberName + "' (ID: " + memberId + ") is deactivated and cannot borrow books.");
                }

                // 2. Verify Member borrowing limit (max 3 active loans)
                String memberLoansSql = "SELECT COUNT(*) FROM loans WHERE member_id = ? AND return_date IS NULL";
                int activeLoans;
                try (PreparedStatement pstmt = conn.prepareStatement(memberLoansSql)) {
                    pstmt.setInt(1, memberId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        activeLoans = rs.next() ? rs.getInt(1) : 0;
                    }
                }

                if (activeLoans >= MAX_BORROW_LIMIT) {
                    throw new IllegalArgumentException("Member '" + memberName + "' currently has " + activeLoans
                        + " books borrowed and has reached the maximum borrowing limit of " + MAX_BORROW_LIMIT + " books.");
                }

                // 3. Verify Book status and available copy count
                String bookSql = """
                    SELECT title, total_copies, active,
                           (total_copies - (SELECT COUNT(*) FROM loans l WHERE l.book_id = books.id AND l.return_date IS NULL)) AS available_copies
                    FROM books WHERE id = ?
                """;
                String bookTitle;
                boolean bookActive;
                int availableCopies;
                try (PreparedStatement pstmt = conn.prepareStatement(bookSql)) {
                    pstmt.setInt(1, bookId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Book ID " + bookId + " not found.");
                        }
                        bookTitle = rs.getString("title");
                        bookActive = rs.getInt("active") == 1;
                        availableCopies = rs.getInt("available_copies");
                    }
                }

                if (!bookActive) {
                    throw new IllegalArgumentException("Book '" + bookTitle + "' is deactivated and cannot be issued.");
                }

                if (availableCopies <= 0) {
                    throw new IllegalArgumentException("No copies of '" + bookTitle + "' are currently available for borrowing.");
                }

                // 4. Insert loan record
                String insertSql = """
                    INSERT INTO loans (book_id, member_id, issue_date, due_date, return_date)
                    VALUES (?, ?, ?, ?, NULL)
                """;
                int generatedId = -1;
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, bookId);
                    pstmt.setInt(2, memberId);
                    pstmt.setString(3, issueDate.trim());
                    pstmt.setString(4, dueDate.trim());
                    pstmt.executeUpdate();

                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            generatedId = rs.getInt(1);
                        }
                    }
                }

                conn.commit();
                return generatedId;
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) ex;
                }
                throw new SQLException("Failed to issue book: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Records a book return within a transaction.
     *
     * @param loanId     ID of the active loan to return
     * @param returnDate Return date (YYYY-MM-DD, defaults to today)
     * @throws IllegalArgumentException if loan was already returned or not found
     * @throws SQLException on database error
     */
    public void returnBook(int loanId, String returnDate) throws SQLException {
        if (returnDate == null || returnDate.trim().isEmpty()) {
            returnDate = LocalDate.now().toString();
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check current loan status
                String checkSql = "SELECT return_date FROM loans WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                    pstmt.setInt(1, loanId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Loan record ID " + loanId + " does not exist.");
                        }
                        String existingReturnDate = rs.getString("return_date");
                        if (existingReturnDate != null && !existingReturnDate.trim().isEmpty()) {
                            throw new IllegalArgumentException("Book has already been returned on " + existingReturnDate + ".");
                        }
                    }
                }

                // Update return_date
                String updateSql = "UPDATE loans SET return_date = ? WHERE id = ? AND return_date IS NULL";
                int updatedRows;
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, returnDate.trim());
                    pstmt.setInt(2, loanId);
                    updatedRows = pstmt.executeUpdate();
                }

                if (updatedRows == 0) {
                    throw new IllegalArgumentException("Failed to update loan. It may have already been returned.");
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) ex;
                }
                throw new SQLException("Failed to process book return: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Retrieves loans based on status filter and search query.
     *
     * @param filter "ALL", "ACTIVE", "OVERDUE", or "RETURNED"
     * @param query  Optional search term matching book title, member name, or loan ID
     */
    public List<Loan> getLoans(String filter, String query) throws SQLException {
        String today = LocalDate.now().toString();
        StringBuilder sql = new StringBuilder("""
            SELECT l.id, l.book_id, l.member_id, l.issue_date, l.due_date, l.return_date,
                   b.title AS book_title, b.isbn AS book_isbn,
                   m.name AS member_name, m.email AS member_email
            FROM loans l
            JOIN books b ON l.book_id = b.id
            JOIN members m ON l.member_id = m.id
            WHERE 1=1
        """);

        if (filter != null) {
            switch (filter.toUpperCase()) {
                case "ACTIVE":
                    sql.append(" AND l.return_date IS NULL AND l.due_date >= '").append(today).append("'");
                    break;
                case "OVERDUE":
                    sql.append(" AND l.return_date IS NULL AND l.due_date < '").append(today).append("'");
                    break;
                case "RETURNED":
                    sql.append(" AND l.return_date IS NOT NULL");
                    break;
                case "ALL_ACTIVE": // Both on-time and overdue active loans
                    sql.append(" AND l.return_date IS NULL");
                    break;
                case "ALL":
                default:
                    break;
            }
        }

        boolean hasQuery = (query != null && !query.trim().isEmpty());
        if (hasQuery) {
            sql.append("""
                AND (LOWER(b.title) LIKE ? OR LOWER(m.name) LIKE ? OR LOWER(COALESCE(b.isbn, '')) LIKE ? OR CAST(l.id AS TEXT) = ?)
            """);
        }

        sql.append(" ORDER BY l.id DESC");

        List<Loan> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            if (hasQuery) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                pstmt.setString(1, pattern);
                pstmt.setString(2, pattern);
                pstmt.setString(3, pattern);
                pstmt.setString(4, query.trim());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRowToLoan(rs));
                }
            }
        }
        return result;
    }

    /**
     * Gets all currently active loans (unreturned).
     */
    public List<Loan> getActiveLoans(String query) throws SQLException {
        return getLoans("ALL_ACTIVE", query);
    }

    /**
     * Retrieves a single loan record by ID.
     */
    public Loan getLoanById(int id) throws SQLException {
        String sql = """
            SELECT l.id, l.book_id, l.member_id, l.issue_date, l.due_date, l.return_date,
                   b.title AS book_title, b.isbn AS book_isbn,
                   m.name AS member_name, m.email AS member_email
            FROM loans l
            JOIN books b ON l.book_id = b.id
            JOIN members m ON l.member_id = m.id
            WHERE l.id = ?
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToLoan(rs);
                }
            }
        }
        return null;
    }

    /**
     * Computes the complete dashboard overview statistics.
     */
    public DashboardStats getDashboardStatistics() throws SQLException {
        String today = LocalDate.now().toString();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Total active book titles
            int totalTitles = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books WHERE active = 1")) {
                if (rs.next()) totalTitles = rs.getInt(1);
            }

            // 2. Total physical copies
            int totalPhysicalCopies = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(total_copies), 0) FROM books WHERE active = 1")) {
                if (rs.next()) totalPhysicalCopies = rs.getInt(1);
            }

            // 3. Active loans
            int activeLoans = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM loans WHERE return_date IS NULL")) {
                if (rs.next()) activeLoans = rs.getInt(1);
            }

            // 4. Overdue loans (unreturned and due date < today)
            int overdueLoans = 0;
            String overdueSql = "SELECT COUNT(*) FROM loans WHERE return_date IS NULL AND due_date < '" + today + "'";
            try (ResultSet rs = stmt.executeQuery(overdueSql)) {
                if (rs.next()) overdueLoans = rs.getInt(1);
            }

            // 5. Total active members
            int totalMembers = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM members WHERE active = 1")) {
                if (rs.next()) totalMembers = rs.getInt(1);
            }

            // 6. Available copies = total copies of active books - active loans
            int availableCopies = Math.max(0, totalPhysicalCopies - activeLoans);

            return new DashboardStats(
                totalTitles,
                totalPhysicalCopies,
                availableCopies,
                totalMembers,
                activeLoans,
                overdueLoans
            );
        }
    }

    private Loan mapRowToLoan(ResultSet rs) throws SQLException {
        Loan loan = new Loan(
            rs.getInt("id"),
            rs.getInt("book_id"),
            rs.getInt("member_id"),
            rs.getString("issue_date"),
            rs.getString("due_date"),
            rs.getString("return_date")
        );
        loan.setBookTitle(rs.getString("book_title"));
        loan.setBookIsbn(rs.getString("book_isbn"));
        loan.setMemberName(rs.getString("member_name"));
        loan.setMemberEmail(rs.getString("member_email"));
        return loan;
    }
}
