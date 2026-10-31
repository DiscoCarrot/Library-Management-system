import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Utility to populate realistic sample library data for presentations and demos.
 * This is an optional feature and does NOT run automatically on every launch.
 */
public class SampleDataLoader {

    /**
     * Loads sample books, members, and initial loans into the database.
     *
     * @return Number of records seeded
     * @throws SQLException on database error
     */
    public static int loadSampleData() throws SQLException {
        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        int seededCount = 0;

        // 1. Seed Books if catalog is empty
        if (bookDAO.getAllBooks(true).isEmpty()) {
            Book[] sampleBooks = {
                new Book(0, "Clean Code", "Robert C. Martin", "978-0132350884", "Computer Science", 2008, 5, true, 5),
                new Book(0, "The Pragmatic Programmer", "David Thomas, Andrew Hunt", "978-0135957059", "Computer Science", 2019, 4, true, 4),
                new Book(0, "Introduction to Algorithms", "Thomas H. Cormen", "978-0262033848", "Computer Science", 2009, 3, true, 3),
                new Book(0, "Effective Java", "Joshua Bloch", "978-0134685991", "Computer Science", 2018, 4, true, 4),
                new Book(0, "Design Patterns", "Erich Gamma, Richard Helm", "978-0201633610", "Computer Science", 1994, 3, true, 3),
                new Book(0, "To Kill a Mockingbird", "Harper Lee", "978-0060935467", "Fiction", 1960, 4, true, 4),
                new Book(0, "1984", "George Orwell", "978-0451524935", "Fiction", 1949, 5, true, 5),
                new Book(0, "A Brief History of Time", "Stephen Hawking", "978-0553380163", "Science", 1988, 3, true, 3),
                new Book(0, "Sapiens: A Brief History of Humankind", "Yuval Noah Harari", null, "History", 2014, 4, true, 4) // No ISBN example
            };

            for (Book b : sampleBooks) {
                bookDAO.addBook(b);
                seededCount++;
            }
        }

        // 2. Seed Members if empty
        if (memberDAO.getAllMembers(true).isEmpty()) {
            Member[] sampleMembers = {
                new Member(0, "Alice Johnson", "alice.johnson@example.com", "555-0101", LocalDate.now().minusDays(60).toString(), true, 0),
                new Member(0, "Bob Smith", "bob.smith@example.com", "555-0102", LocalDate.now().minusDays(45).toString(), true, 0),
                new Member(0, "Charlie Brown", "charlie.brown@example.com", "555-0103", LocalDate.now().minusDays(30).toString(), true, 0),
                new Member(0, "Diana Prince", "diana.prince@example.com", "555-0104", LocalDate.now().minusDays(15).toString(), true, 0),
                new Member(0, "Evan Wright", "evan.wright@example.com", "555-0105", LocalDate.now().minusDays(5).toString(), true, 0)
            };

            for (Member m : sampleMembers) {
                memberDAO.addMember(m);
                seededCount++;
            }
        }

        // 3. Seed Sample Loans if none exist
        if (loanDAO.getLoans("ALL", "").isEmpty()) {
            LocalDate today = LocalDate.now();

            // Loan 1: Alice Johnson borrowed Clean Code (active on-time)
            int loan1 = loanDAO.issueBook(1, 1, today.minusDays(5).toString(), today.plusDays(9).toString());
            if (loan1 > 0) seededCount++;

            // Loan 2: Bob Smith borrowed Effective Java (overdue loan)
            int loan2 = loanDAO.issueBook(4, 2, today.minusDays(20).toString(), today.minusDays(6).toString());
            if (loan2 > 0) seededCount++;

            // Loan 3: Charlie Brown borrowed To Kill a Mockingbird (completed returned loan)
            int loan3 = loanDAO.issueBook(6, 3, today.minusDays(15).toString(), today.minusDays(1).toString());
            if (loan3 > 0) {
                loanDAO.returnBook(loan3, today.minusDays(1).toString());
                seededCount++;
            }
        }

        return seededCount;
    }
}
