import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Represents a book borrowing transaction (loan).
 */
public class Loan {
    private int id;
    private int bookId;
    private int memberId;
    private String issueDate; // YYYY-MM-DD
    private String dueDate;   // YYYY-MM-DD
    private String returnDate; // YYYY-MM-DD, null if active

    // Joined helper fields for easy UI rendering
    private String bookTitle;
    private String memberName;
    private String bookIsbn;
    private String memberEmail;

    public Loan() {}

    public Loan(int id, int bookId, int memberId, String issueDate, String dueDate, String returnDate) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        this.issueDate = issueDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getBookIsbn() {
        return bookIsbn;
    }

    public void setBookIsbn(String bookIsbn) {
        this.bookIsbn = bookIsbn;
    }

    public String getMemberEmail() {
        return memberEmail;
    }

    public void setMemberEmail(String memberEmail) {
        this.memberEmail = memberEmail;
    }

    /**
     * Checks if this loan has been returned.
     */
    public boolean isReturned() {
        return returnDate != null && !returnDate.trim().isEmpty();
    }

    /**
     * A loan is overdue if and only if it is NOT returned and its due date precedes today's date.
     * Returned loans are never marked overdue.
     */
    public boolean isOverdue() {
        if (isReturned()) {
            return false;
        }
        if (dueDate == null || dueDate.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDate due = LocalDate.parse(dueDate);
            return due.isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculates the number of days overdue. Returns 0 if not overdue.
     */
    public long getDaysOverdue() {
        if (!isOverdue()) {
            return 0;
        }
        try {
            LocalDate due = LocalDate.parse(dueDate);
            return ChronoUnit.DAYS.between(due, LocalDate.now());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Returns a human-readable status string.
     */
    public String getStatus() {
        if (isReturned()) {
            return "Returned";
        }
        if (isOverdue()) {
            long days = getDaysOverdue();
            return "OVERDUE (" + days + (days == 1 ? " day)" : " days)");
        }
        return "Active (On Time)";
    }
}
