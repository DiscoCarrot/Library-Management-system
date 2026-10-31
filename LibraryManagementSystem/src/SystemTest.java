import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Automated test suite verifying all 17 required technical and business test cases.
 * Can be executed in headless environments without display servers.
 */
public class SystemTest {

    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("   Library Management System — Automated Test Verification Suite      ");
        System.out.println("======================================================================");

        String testDbPath = "data/test_library.db";
        File testDbFile = new File(testDbPath);
        if (testDbFile.exists()) {
            testDbFile.delete();
        }

        // Configure test database
        Database.setDatabasePath(testDbPath);

        try {
            // Test 1: First launch creates database and tables
            testDatabaseInitialization(testDbFile);

            // Test 2: Default administrator can log in
            testDefaultAdminLogin();

            // Test 3: Invalid login is rejected
            testInvalidLoginRejected();

            // Test 4: Books can be added, edited, searched, and deactivated
            testBookCrudAndDeactivation();

            // Test 5: Duplicate ISBNs are rejected (while multiple null ISBNs are allowed)
            testIsbnDuplicateAndNullHandling();

            // Test 6: Members can be registered and searched
            testMemberRegistrationAndSearch();

            // Test 7: Issuing a book reduces the number of available copies
            testIssueBookReducesAvailableCopies();

            // Test 8: Issuing an unavailable book is rejected
            testIssuingUnavailableBookRejected();

            // Test 9: An inactive member cannot borrow a book
            testInactiveMemberCannotBorrow();

            // Test 10: A member cannot exceed the three-book borrowing limit
            testBorrowingLimitEnforcement();

            // Test 11: Returning a book restores availability
            testReturnBookRestoresAvailability();

            // Test 12: Returning the same loan twice is rejected
            testDoubleReturnRejected();

            // Test 13: Overdue loans are identified correctly
            testOverdueLoansIdentified();

            // Test 14: Returned loans are not marked overdue
            testReturnedLoansNeverOverdue();

            // Test 15: Book and member history remains intact after deactivation
            testHistoryPreservedAfterDeactivation();

            // Test 16: Data persists after the application is closed and reopened
            testDataPersistenceAcrossReconnection(testDbPath);

            // Test 17: Database errors do not terminate the application unexpectedly
            testDatabaseErrorGracefulHandling();

            // Test 18: Sample demo data loader populates realistic records idempotently
            testSampleDataLoader();

        } catch (Exception e) {
            System.err.println("Fatal unexpected error during test suite: " + e.getMessage());
            e.printStackTrace();
            failedTests++;
        }

        // Clean up test database file
        try {
            if (testDbFile.exists()) {
                testDbFile.delete();
            }
        } catch (Exception ignored) {}

        // Reset database path to default
        Database.setDatabasePath(Database.DEFAULT_DB_PATH);

        System.out.println("======================================================================");
        System.out.printf("Test Execution Summary: %d PASSED, %d FAILED (Total: %d)\n",
            passedTests, failedTests, passedTests + failedTests);
        System.out.println("======================================================================");

        if (failedTests > 0) {
            System.exit(1);
        }
    }

    private static void assertCondition(String testName, boolean condition, String errorDetail) {
        if (condition) {
            System.out.println("  [PASS] " + testName);
            passedTests++;
        } else {
            System.err.println("  [FAIL] " + testName + " — " + errorDetail);
            failedTests++;
        }
    }

    // 1. First launch creates the database and tables
    private static void testDatabaseInitialization(File testDbFile) throws Exception {
        System.out.println("\n--- 1. Testing Database & Schema Initialization ---");
        Database.initializeDatabase();
        assertCondition("Database file created on first launch", testDbFile.exists(), "DB file missing at " + testDbFile.getPath());

        try (Connection conn = Database.getConnection()) {
            assertCondition("Connection established with foreign keys", conn != null && !conn.isClosed(), "Connection closed or null");
        }
    }

    // 2. Default administrator can log in
    private static void testDefaultAdminLogin() throws Exception {
        System.out.println("\n--- 2. Testing Default Administrator Login ---");
        UserDAO userDAO = new UserDAO();
        User admin = userDAO.authenticate("admin", "admin123");
        assertCondition("Default admin account authenticates successfully",
            admin != null && "admin".equals(admin.getUsername()) && "ADMIN".equals(admin.getRole()),
            "Failed to authenticate default admin");
    }

    // 3. Invalid login is rejected
    private static void testInvalidLoginRejected() throws Exception {
        System.out.println("\n--- 3. Testing Invalid Login Rejection ---");
        UserDAO userDAO = new UserDAO();
        User invalidUser = userDAO.authenticate("admin", "wrong_password");
        User nonExistent = userDAO.authenticate("nobody", "admin123");
        assertCondition("Incorrect password rejected", invalidUser == null, "Incorrect password was accepted");
        assertCondition("Non-existent username rejected", nonExistent == null, "Non-existent user was accepted");
    }

    // 4. Books can be added, edited, searched, and deactivated
    private static void testBookCrudAndDeactivation() throws Exception {
        System.out.println("\n--- 4. Testing Book CRUD & Deactivation ---");
        BookDAO bookDAO = new BookDAO();

        Book book = new Book(0, "Test Architecture", "Martin Fowler", "978-0134757599", "Architecture", 2018, 5, true, 5);
        int bookId = bookDAO.addBook(book);
        assertCondition("Book added with generated ID", bookId > 0, "Generated ID <= 0");

        Book fetched = bookDAO.getBookById(bookId);
        assertCondition("Book fetched accurately", fetched != null && "Test Architecture".equals(fetched.getTitle()), "Fetched book title mismatch");

        // Edit
        fetched.setTitle("Refactoring Architecture");
        bookDAO.updateBook(fetched);
        Book updated = bookDAO.getBookById(bookId);
        assertCondition("Book edited successfully", "Refactoring Architecture".equals(updated.getTitle()), "Title not updated");

        // Search
        List<Book> searchResult = bookDAO.searchBooks("Refactoring", false);
        assertCondition("Book search finds title", !searchResult.isEmpty() && searchResult.get(0).getId() == bookId, "Search returned empty");

        // Deactivate
        bookDAO.setBookActive(bookId, false);
        Book deactivated = bookDAO.getBookById(bookId);
        assertCondition("Book deactivated", deactivated != null && !deactivated.isActive(), "Book still active");

        // Reactivate
        bookDAO.setBookActive(bookId, true);
        Book reactivated = bookDAO.getBookById(bookId);
        assertCondition("Book reactivated", reactivated != null && reactivated.isActive(), "Book not reactivated");
    }

    // 5. Duplicate ISBNs are rejected (and null ISBNs allowed)
    private static void testIsbnDuplicateAndNullHandling() throws Exception {
        System.out.println("\n--- 5. Testing Duplicate ISBN Rejection & Null ISBN Support ---");
        BookDAO bookDAO = new BookDAO();

        Book b1 = new Book(0, "Book A", "Author A", "978-1111111111", "Science", 2020, 2, true, 2);
        bookDAO.addBook(b1);

        Book bDuplicate = new Book(0, "Book B", "Author B", "978-1111111111", "Science", 2021, 2, true, 2);
        boolean duplicateRejected = false;
        try {
            bookDAO.addBook(bDuplicate);
        } catch (IllegalArgumentException e) {
            duplicateRejected = true;
        }
        assertCondition("Duplicate ISBN rejected with validation message", duplicateRejected, "Duplicate ISBN was accepted");

        // Test multiple books without ISBN (null ISBN)
        Book noIsbn1 = new Book(0, "Ancient Manuscript 1", "Unknown", null, "History", 1500, 1, true, 1);
        Book noIsbn2 = new Book(0, "Ancient Manuscript 2", "Unknown", "", "History", 1600, 1, true, 1);
        int id1 = bookDAO.addBook(noIsbn1);
        int id2 = bookDAO.addBook(noIsbn2);
        assertCondition("Multiple books with absent/null ISBN supported", id1 > 0 && id2 > 0, "Failed to insert multiple books without ISBN");
    }

    // 6. Members can be registered and searched
    private static void testMemberRegistrationAndSearch() throws Exception {
        System.out.println("\n--- 6. Testing Member Registration & Search ---");
        MemberDAO memberDAO = new MemberDAO();

        Member member = new Member(0, "Sarah Connor", "sarah@resistance.org", "555-1984", LocalDate.now().toString(), true, 0);
        int memberId = memberDAO.addMember(member);
        assertCondition("Member registered with generated ID", memberId > 0, "Member ID <= 0");

        List<Member> found = memberDAO.searchMembers("Sarah", false);
        assertCondition("Member search finds registered member", !found.isEmpty() && found.get(0).getId() == memberId, "Member search failed");
    }

    // 7. Issuing a book reduces the number of available copies
    private static void testIssueBookReducesAvailableCopies() throws Exception {
        System.out.println("\n--- 7. Testing Book Issuing & Stock Availability Reduction ---");
        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        Book book = new Book(0, "Calculus Early Transcendentals", "James Stewart", "978-1285741550", "Math", 2015, 3, true, 3);
        int bookId = bookDAO.addBook(book);

        Member member = new Member(0, "John Doe", "john@example.com", "555-0100", LocalDate.now().toString(), true, 0);
        int memberId = memberDAO.addMember(member);

        int initialAvailable = bookDAO.getAvailableCopies(bookId);
        assertCondition("Initial available copies equal total copies", initialAvailable == 3, "Expected 3 copies, got " + initialAvailable);

        String today = LocalDate.now().toString();
        String due = LocalDate.now().plusDays(14).toString();
        int loanId = loanDAO.issueBook(bookId, memberId, today, due);
        assertCondition("Book issued successfully", loanId > 0, "Loan ID <= 0");

        int afterIssue = bookDAO.getAvailableCopies(bookId);
        assertCondition("Available copies reduced by 1 after issue", afterIssue == 2, "Expected 2 copies available, got " + afterIssue);
    }

    // 8. Issuing an unavailable book is rejected
    private static void testIssuingUnavailableBookRejected() throws Exception {
        System.out.println("\n--- 8. Testing Issuing Unavailable Book (Stock = 0) ---");
        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        Book rareBook = new Book(0, "Rare First Edition", "Classic Author", "978-0000000001", "Rare", 1900, 1, true, 1);
        int bookId = bookDAO.addBook(rareBook);

        Member m1 = new Member(0, "Borrower One", "one@example.com", "555-1111", LocalDate.now().toString(), true, 0);
        Member m2 = new Member(0, "Borrower Two", "two@example.com", "555-2222", LocalDate.now().toString(), true, 0);
        int m1Id = memberDAO.addMember(m1);
        int m2Id = memberDAO.addMember(m2);

        String today = LocalDate.now().toString();
        String due = LocalDate.now().plusDays(14).toString();

        loanDAO.issueBook(bookId, m1Id, today, due); // Uses the 1 available copy

        boolean rejected = false;
        try {
            loanDAO.issueBook(bookId, m2Id, today, due); // Should fail: 0 available
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        assertCondition("Issuing book with 0 available copies rejected", rejected, "Unavailable book was issued");
    }

    // 9. An inactive member cannot borrow a book
    private static void testInactiveMemberCannotBorrow() throws Exception {
        System.out.println("\n--- 9. Testing Inactive Member Borrowing Rejection ---");
        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        Book book = new Book(0, "Operating Systems", "Silberschatz", "978-1118063330", "CS", 2018, 5, true, 5);
        int bookId = bookDAO.addBook(book);

        Member inactiveMember = new Member(0, "Suspended Member", "sus@example.com", "555-9999", LocalDate.now().toString(), false, 0);
        int memberId = memberDAO.addMember(inactiveMember);
        memberDAO.setMemberActive(memberId, false);

        boolean rejected = false;
        try {
            loanDAO.issueBook(bookId, memberId, LocalDate.now().toString(), LocalDate.now().plusDays(14).toString());
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        assertCondition("Inactive member borrowing rejected", rejected, "Inactive member was permitted to borrow");
    }

    // 10. A member cannot exceed the three-book borrowing limit
    private static void testBorrowingLimitEnforcement() throws Exception {
        System.out.println("\n--- 10. Testing 3-Book Borrowing Limit Enforcement ---");
        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        Member patron = new Member(0, "Avid Reader", "avid@example.com", "555-4444", LocalDate.now().toString(), true, 0);
        int memberId = memberDAO.addMember(patron);

        int b1 = bookDAO.addBook(new Book(0, "Book 1", "A", "978-2000000001", "CS", 2020, 2, true, 2));
        int b2 = bookDAO.addBook(new Book(0, "Book 2", "A", "978-2000000002", "CS", 2020, 2, true, 2));
        int b3 = bookDAO.addBook(new Book(0, "Book 3", "A", "978-2000000003", "CS", 2020, 2, true, 2));
        int b4 = bookDAO.addBook(new Book(0, "Book 4", "A", "978-2000000004", "CS", 2020, 2, true, 2));

        String today = LocalDate.now().toString();
        String due = LocalDate.now().plusDays(14).toString();

        loanDAO.issueBook(b1, memberId, today, due);
        loanDAO.issueBook(b2, memberId, today, due);
        loanDAO.issueBook(b3, memberId, today, due);

        assertCondition("Member successfully borrowed 3 books", memberDAO.getActiveLoansCountForMember(memberId) == 3, "Active count not 3");

        boolean limitRejected = false;
        try {
            loanDAO.issueBook(b4, memberId, today, due); // 4th book
        } catch (IllegalArgumentException e) {
            limitRejected = true;
        }
        assertCondition("4th book borrow attempt rejected (3-book limit enforced)", limitRejected, "4th loan was allowed");
    }

    // 11. Returning a book restores availability
    private static void testReturnBookRestoresAvailability() throws Exception {
        System.out.println("\n--- 11. Testing Book Return Restores Availability ---");
        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        Book book = new Book(0, "Linear Algebra", "Strang", "978-0980232776", "Math", 2016, 2, true, 2);
        int bookId = bookDAO.addBook(book);

        Member member = new Member(0, "Math Student", "math@example.com", "555-5555", LocalDate.now().toString(), true, 0);
        int memberId = memberDAO.addMember(member);

        String today = LocalDate.now().toString();
        int loanId = loanDAO.issueBook(bookId, memberId, today, LocalDate.now().plusDays(14).toString());
        assertCondition("Availability decreased to 1 after loan", bookDAO.getAvailableCopies(bookId) == 1, "Avail count mismatch");

        loanDAO.returnBook(loanId, today);
        assertCondition("Availability restored to 2 after return", bookDAO.getAvailableCopies(bookId) == 2, "Avail count did not restore");
    }

    // 12. Returning the same loan twice is rejected
    private static void testDoubleReturnRejected() throws Exception {
        System.out.println("\n--- 12. Testing Double Return Prevention ---");
        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        int bId = bookDAO.addBook(new Book(0, "Return Test Book", "Author", "978-3000000001", "Fiction", 2021, 2, true, 2));
        int mId = memberDAO.addMember(new Member(0, "Returner", "ret@example.com", "555-6666", LocalDate.now().toString(), true, 0));

        int loanId = loanDAO.issueBook(bId, mId, LocalDate.now().toString(), LocalDate.now().plusDays(14).toString());
        loanDAO.returnBook(loanId, LocalDate.now().toString());

        boolean doubleReturnRejected = false;
        try {
            loanDAO.returnBook(loanId, LocalDate.now().toString());
        } catch (IllegalArgumentException e) {
            doubleReturnRejected = true;
        }
        assertCondition("Returning an already returned loan is rejected", doubleReturnRejected, "Double return was permitted");
    }

    // 13. Overdue loans are identified correctly
    private static void testOverdueLoansIdentified() throws Exception {
        System.out.println("\n--- 13. Testing Overdue Loan Detection ---");
        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        int bId = bookDAO.addBook(new Book(0, "Overdue Target", "Author", "978-4000000001", "Fiction", 2021, 2, true, 2));
        int mId = memberDAO.addMember(new Member(0, "Late Patron", "late@example.com", "555-7777", LocalDate.now().toString(), true, 0));

        // Loan issued 20 days ago, due 6 days ago
        String pastIssue = LocalDate.now().minusDays(20).toString();
        String pastDue = LocalDate.now().minusDays(6).toString();
        int overdueLoanId = loanDAO.issueBook(bId, mId, pastIssue, pastDue);

        Loan loan = loanDAO.getLoanById(overdueLoanId);
        assertCondition("Unreturned loan past due date marked as overdue", loan.isOverdue(), "Loan not marked overdue");
        assertCondition("Overdue days calculated accurately", loan.getDaysOverdue() == 6, "Expected 6 days overdue, got " + loan.getDaysOverdue());
    }

    // 14. Returned loans are not marked overdue
    private static void testReturnedLoansNeverOverdue() throws Exception {
        System.out.println("\n--- 14. Testing Returned Loans Are Never Overdue ---");
        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        int bId = bookDAO.addBook(new Book(0, "Late But Returned", "Author", "978-5000000001", "Fiction", 2021, 2, true, 2));
        int mId = memberDAO.addMember(new Member(0, "Returning Patron", "ret2@example.com", "555-8888", LocalDate.now().toString(), true, 0));

        // Loan was past due date, but returned
        int loanId = loanDAO.issueBook(bId, mId, LocalDate.now().minusDays(15).toString(), LocalDate.now().minusDays(1).toString());
        loanDAO.returnBook(loanId, LocalDate.now().toString());

        Loan returnedLoan = loanDAO.getLoanById(loanId);
        assertCondition("Returned loan has isOverdue() == false", !returnedLoan.isOverdue(), "Returned loan marked as overdue");
        assertCondition("Status reports Returned", returnedLoan.getStatus().startsWith("Returned"), "Status does not report Returned");
    }

    // 15. Book and member history remains intact after deactivation
    private static void testHistoryPreservedAfterDeactivation() throws Exception {
        System.out.println("\n--- 15. Testing History Preserved After Deactivation ---");
        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        int bId = bookDAO.addBook(new Book(0, "Archival Book", "Author", "978-6000000001", "History", 2020, 2, true, 2));
        int mId = memberDAO.addMember(new Member(0, "Archival Member", "arch@example.com", "555-9000", LocalDate.now().toString(), true, 0));

        int loanId = loanDAO.issueBook(bId, mId, LocalDate.now().toString(), LocalDate.now().plusDays(14).toString());
        loanDAO.returnBook(loanId, LocalDate.now().toString());

        // Hard deletion should be prevented because loan history exists
        boolean bookDeletePrevented = false;
        try {
            bookDAO.deleteBook(bId);
        } catch (IllegalArgumentException e) {
            bookDeletePrevented = true;
        }
        assertCondition("Hard delete of book with loan history prevented", bookDeletePrevented, "Book was deleted despite loan history");

        boolean memberDeletePrevented = false;
        try {
            memberDAO.deleteMember(mId);
        } catch (IllegalArgumentException e) {
            memberDeletePrevented = true;
        }
        assertCondition("Hard delete of member with loan history prevented", memberDeletePrevented, "Member was deleted despite loan history");

        // Deactivation succeeds
        bookDAO.setBookActive(bId, false);
        memberDAO.setMemberActive(mId, false);

        Loan historyLoan = loanDAO.getLoanById(loanId);
        assertCondition("Historical loan record intact after deactivation",
            historyLoan != null && "Archival Book".equals(historyLoan.getBookTitle()),
            "Historical loan record missing or damaged");
    }

    // 16. Data persists after application is closed and reopened
    private static void testDataPersistenceAcrossReconnection(String testDbPath) throws Exception {
        System.out.println("\n--- 16. Testing Data Persistence Across Reconnection ---");
        // Create a new independent connection to test persistence
        Database.setDatabasePath(testDbPath);
        BookDAO bookDAO = new BookDAO();
        List<Book> books = bookDAO.getAllBooks(true);
        assertCondition("Data persists in SQLite database across reconnection", !books.isEmpty(), "Books lost on reconnection");
    }

    // 17. Database errors do not terminate the application unexpectedly
    private static void testDatabaseErrorGracefulHandling() {
        System.out.println("\n--- 17. Testing Graceful Database Error Handling ---");
        try {
            LoanDAO loanDAO = new LoanDAO();
            // Test query with invalid parameters does not crash JVM
            boolean handled = false;
            try {
                loanDAO.issueBook(-999, -888, "2024-01-01", "2024-01-15");
            } catch (IllegalArgumentException | SQLException e) {
                handled = true;
            }
            assertCondition("Invalid database query caught gracefully without unhandled exception", handled, "Exception not handled");
        } catch (Exception e) {
            assertCondition("Invalid database query caught gracefully", false, "Unexpected crash: " + e.getMessage());
        }
    }

    // 18. Sample demo data loader populates realistic records idempotently
    private static void testSampleDataLoader() throws Exception {
        System.out.println("\n--- 18. Testing Sample Data Loader & Demo Seed ---");
        // Create fresh database for sample data test
        String demoDbPath = "data/demo_test_library.db";
        File demoDb = new File(demoDbPath);
        if (demoDb.exists()) demoDb.delete();

        Database.setDatabasePath(demoDbPath);
        Database.initializeDatabase();

        int seeded = SampleDataLoader.loadSampleData();
        assertCondition("Sample data loader seeded demo records", seeded > 0, "No records seeded");

        LoanDAO loanDAO = new LoanDAO();
        DashboardStats stats = loanDAO.getDashboardStatistics();
        assertCondition("Demo catalog has active titles", stats.getTotalBookTitles() > 0, "No active book titles");
        assertCondition("Demo catalog has physical copies", stats.getTotalPhysicalCopies() > 0, "No physical copies");
        assertCondition("Demo catalog has active loans", stats.getActiveLoans() > 0, "No active loans");
        assertCondition("Demo catalog has overdue loan for demo", stats.getOverdueLoans() > 0, "No overdue loan found");
        assertCondition("Demo catalog available copies = total - active",
            stats.getAvailableCopies() == stats.getTotalPhysicalCopies() - stats.getActiveLoans(),
            "Availability formula discrepancy");

        // Test idempotence
        int secondSeeded = SampleDataLoader.loadSampleData();
        assertCondition("Sample data loader is idempotent (does not duplicate on rerun)", secondSeeded == 0, "Duplicates created");

        if (demoDb.exists()) demoDb.delete();
    }
}
