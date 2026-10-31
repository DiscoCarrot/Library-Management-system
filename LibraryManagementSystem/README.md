# Library Management System — Java Mini Project

A complete, self-contained **Library Management System** built for college project demonstrations and lab coursework using **Core Java (Java 17+)**, **Java Swing**, and **SQLite via JDBC**.

Runs 100% locally on any computer without needing an internet connection, external database servers (no MySQL/PostgreSQL), web frameworks, or build tools (no Maven/Gradle).

---

## 📑 Table of Contents

1. [Project Overview & Key Highlights](#project-overview--key-highlights)
2. [Default Credentials](#default-credentials)
3. [Architecture & Design](#architecture--design)
4. [Database Schema & Integrity](#database-schema--integrity)
5. [Feature Summary](#feature-summary)
6. [Project Directory Structure](#project-directory-structure)
7. [How to Compile and Run](#how-to-compile-and-run)
   - [Option A: Terminal / Command Line (Linux / macOS / Windows)](#option-a-terminal--command-line)
   - [Option B: IntelliJ IDEA](#option-b-intellij-idea)
   - [Option C: Eclipse IDE](#option-c-eclipse-ide)
   - [Option D: Apache NetBeans](#option-d-apache-netbeans)
8. [Automated Test Suite](#automated-test-suite)
9. [Manual Testing Checklist](#manual-testing-checklist)
10. [Troubleshooting Guide](#troubleshooting-guide)

---

## 🎯 Project Overview & Key Highlights

- **Pure Java & Swing**: Clean, modern desktop graphical user interface with responsive layouts, uneditable tables, styled metrics, and informative dialogs.
- **Embedded Local Database**: Uses SQLite via the official SQLite JDBC driver. The database file (`data/library.db`) and all required tables are automatically created on first launch.
- **Strict Data Integrity**:
  - Enforces SQLite foreign keys on every connection (`PRAGMA foreign_keys = ON`).
  - Uses atomic JDBC transactions (`commit` and `rollback`) for multi-step circulation actions.
  - Calculated book availability: dynamically derived as `total_copies - COUNT(active loans)` so stock never gets out of sync.
  - Prevents reducing book total copies below currently borrowed physical copies.
  - Hard deletion of books and members with loan history is blocked to preserve historical archives (safe deactivation toggle provided).
- **Security**: Passwords hashed using industry-standard **PBKDF2 with HMAC-SHA256** and per-user random salts. Plaintext passwords are never stored.
- **Zero External Server Setup**: No background services or server daemons needed.

---

## 🔑 Default Credentials

When launching the application for the first time, a default administrator account is automatically provisioned:

| Attribute | Default Value | Notes |
| :--- | :--- | :--- |
| **Username** | `admin` | Case-insensitive |
| **Password** | `admin123` | Secured with PBKDF2 salt hash |
| **Role** | `ADMIN` | System administrator |

> [!WARNING]
> These credentials are for local development, academic testing, and demonstration purposes. In a real-world deployment, change the default administrator password immediately.

---

## 🏗️ Architecture & Design

The project uses a clean, beginner-friendly **Model-DAO-View** architecture without excessive design patterns or complex frameworks:

```
┌─────────────────────────────────────────────────────────────┐
│                    Java Swing GUI Views                     │
│  LoginFrame | MainFrame | DashboardPanel | BookPanel        │
│  MemberPanel | IssueBookPanel | ReturnBookPanel | LoanPanel │
└──────────────────────────────┬──────────────────────────────┘
                               │ User Actions & Events
                               ▼
┌─────────────────────────────────────────────────────────────┐
│              Data Access Object Layer (DAOs)                │
│       UserDAO    |    BookDAO    |    MemberDAO    | LoanDAO │
│  (Business validations, PreparedStatement, Transactions)   │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
               ▼                              ▼
┌──────────────────────────────┐  ┌───────────────────────────┐
│     Domain Models & Utils    │  │       Database Layer      │
│  User, Book, Member, Loan,   │  │  Database.java (SQLite)   │
│  DashboardStats, PasswordUtil│  │  data/library.db (File)   │
└──────────────────────────────┘  └───────────────────────────┘
```

- **Domain Entities (`Book`, `Member`, `Loan`, `User`, `DashboardStats`)**: Pure Java models encapsulating attributes, getters/setters, and helper logic.
- **DAO Layer (`BookDAO`, `MemberDAO`, `LoanDAO`, `UserDAO`)**: Centralizes SQL queries, `PreparedStatement` parameter bindings, and transaction management (`setAutoCommit(false)`, `commit()`, `rollback()`).
- **Database (`Database.java`)**: Connection factory ensuring `PRAGMA foreign_keys = ON` on all connections and managing automated schema generation.
- **Security (`PasswordUtil.java`)**: Pure Java PBKDF2 implementation with `javax.crypto.SecretKeyFactory` and `SecureRandom`.

---

## 🗄️ Database Schema & Integrity

The application maintains four relational tables inside `data/library.db`:

### 1. `users`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique user identifier |
| `username` | TEXT | NOT NULL UNIQUE | Login username |
| `password_hash` | TEXT | NOT NULL | Salt and hash formatted `salt:hash` (Base64) |
| `role` | TEXT | NOT NULL | User authority (`ADMIN`, `LIBRARIAN`) |

### 2. `books`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique book identifier |
| `title` | TEXT | NOT NULL | Book title |
| `author` | TEXT | NOT NULL | Author name |
| `isbn` | TEXT | UNIQUE | ISBN code (NULL allowed for books without ISBN) |
| `category` | TEXT | | Book category / genre |
| `publication_year`| INTEGER | | 4-digit publication year |
| `total_copies` | INTEGER | NOT NULL CHECK(total_copies >= 0) | Total physical copies owned |
| `active` | INTEGER | NOT NULL DEFAULT 1 | 1 = Active, 0 = Deactivated |

### 3. `members`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique member identifier |
| `name` | TEXT | NOT NULL | Member's full name |
| `email` | TEXT | | Email contact |
| `phone` | TEXT | | Phone number |
| `registration_date`| TEXT | NOT NULL | ISO date `YYYY-MM-DD` |
| `active` | INTEGER | NOT NULL DEFAULT 1 | 1 = Active, 0 = Deactivated |

### 4. `loans`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique transaction identifier |
| `book_id` | INTEGER | NOT NULL REFERENCES books(id) | Foreign key to borrowed book |
| `member_id` | INTEGER | NOT NULL REFERENCES members(id)| Foreign key to borrowing member |
| `issue_date` | TEXT | NOT NULL | ISO date `YYYY-MM-DD` |
| `due_date` | TEXT | NOT NULL | ISO date `YYYY-MM-DD` |
| `return_date` | TEXT | | ISO date `YYYY-MM-DD` (NULL if active) |

### Key Availability Formula
Available copies are **never** stored as a redundant mutable column that can go out of sync. Availability is computed directly:
$$\text{Available Copies} = \text{total\_copies} - \text{COUNT(active loans where return\_date IS NULL)}$$

---

## 🚀 Feature Summary

### 1. Librarian Authentication
- Clean login dialog with default credentials hint.
- Passwords verified in constant time to thwart timing attacks.
- Safe logout returns the user to the login screen.

### 2. Real-Time Dashboard
- 6 live summary cards:
  - 📚 Total Book Titles (Active)
  - 📦 Total Physical Copies
  - ✅ Available Copies
  - 👥 Registered Members
  - 🔄 Active Loans
  - ⚠️ Overdue Loans (Alert highlight)
- Priority alerts table listing all loans currently past due.
- Quick navigation shortcuts to issue, return, or refresh.

### 3. Book Catalog Management
- Add, edit, and deactivate books.
- Search across title, author, ISBN, and category.
- Safe stock validation: prevents decreasing total copies below current checked-out count.
- Prevents duplicate ISBNs while safely allowing books with no ISBN (stored as `NULL`).
- Blocks deletion of books with borrowing history (recommends deactivation).

### 4. Member Management
- Register and update patrons with full contact information.
- Search by name, email, phone, or member ID.
- Real-time loan quota monitor ($X / 3$ books currently borrowed).
- Preserves member history upon deactivation; prevents deactivating members with unreturned books.

### 5. Issue Book (Circulation)
- Dual selection for member and book with live quota and stock indicators.
- Automatically defaults issue date to today (`LocalDate.now()`) and due date to +14 days.
- Quick-select due date buttons: `+7 Days`, `+14 Days`, `+30 Days`.
- Strict business rule enforcement in a single atomic transaction:
  1. Member must be active.
  2. Member cannot borrow more than 3 books at the same time.
  3. Book must have at least 1 copy available.
  4. Due date cannot precede issue date.

### 6. Return Book (Check-In)
- Displays all active unreturned loans.
- Filter active loans by book title or member name.
- Prominent warning popup if a book being returned is overdue, showing exact days overdue.
- Atomically marks return date and restores availability count.

### 7. Loan Records & Audit History
- Full transaction history log.
- Filter tabs: **All Loans**, **Currently Issued (Active)**, **Overdue**, **Returned**.
- Searchable by book title, member name, or loan ID.
- Strict overdue rule: only unreturned loans past due date are marked overdue. Returned loans are never marked overdue.

### 8. Optional Demo Data Generator
- Top header includes a `⚡ Load Demo Data` button to populate 9 classic books, 5 members, and sample loans (on-time, overdue, returned) for rapid project presentations.

---

## 📁 Project Directory Structure

```
LibraryManagementSystem/
├── compile.bat            # Windows 1-click compile batch script
├── compile.sh             # Linux/macOS compile shell script
├── run.bat                # Windows 1-click execution batch script
├── run.sh                 # Linux/macOS execution shell script
├── test.sh                # Automated test suite runner script
├── README.md              # Project documentation and guide
├── lib/
│   └── sqlite-jdbc.jar    # SQLite JDBC Driver (the only external JAR)
├── data/
│   └── library.db         # SQLite database file (created on launch)
├── bin/                   # Compiled .class bytecode files
└── src/
    ├── Main.java              # Application main entry point
    ├── Database.java          # SQLite connection and schema init
    ├── PasswordUtil.java      # PBKDF2 password hashing & salt
    ├── UITheme.java           # Visual styling constants & components
    ├── Book.java              # Book domain model
    ├── Member.java            # Member domain model
    ├── Loan.java              # Loan domain model
    ├── User.java              # User domain model
    ├── DashboardStats.java    # Metrics summary model
    ├── BookDAO.java           # Book catalog database operations
    ├── MemberDAO.java         # Member database operations
    ├── LoanDAO.java           # Loan transaction operations
    ├── UserDAO.java           # User authentication operations
    ├── LoginFrame.java        # Login GUI window
    ├── MainFrame.java         # Main dashboard & navigation frame
    ├── DashboardPanel.java    # Dashboard overview panel
    ├── BookPanel.java         # Book management panel
    ├── BookDialog.java        # Book add/edit modal dialog
    ├── MemberPanel.java       # Member management panel
    ├── MemberDialog.java      # Member add/edit modal dialog
    ├── IssueBookPanel.java    # Book issue transaction panel
    ├── ReturnBookPanel.java   # Book return panel
    ├── LoanRecordsPanel.java  # Borrowing history audit panel
    ├── SampleDataLoader.java  # Optional sample demo data seeder
    └── SystemTest.java        # Automated 41-check test suite
```

---

## 💻 How to Compile and Run

Targeted for **Java 17 or later** (OpenJDK 17, 21, 27+).

### Option A: Terminal / Command Line

#### On Linux / macOS:
1. Open a terminal and navigate to the project directory:
   ```bash
   cd LibraryManagementSystem
   ```
2. Make scripts executable (if not already):
   ```bash
   chmod +x *.sh
   ```
3. Compile and run:
   ```bash
   ./compile.sh
   ./run.sh
   ```
   *Or directly using standard `javac` and `java`:*
   ```bash
   # Compile:
   javac -cp lib/sqlite-jdbc.jar -d bin src/*.java

   # Run:
   java -cp bin:lib/sqlite-jdbc.jar Main
   ```

#### On Windows (Command Prompt / PowerShell):
1. Open Command Prompt (`cmd`) and navigate to the directory:
   ```cmd
   cd LibraryManagementSystem
   ```
2. Double-click `run.bat` or run:
   ```cmd
   compile.bat
   run.bat
   ```
   *Or directly using `javac` and `java`:*
   ```cmd
   javac -cp "lib/sqlite-jdbc.jar" -d bin src/*.java
   java -cp "bin;lib/sqlite-jdbc.jar" Main
   ```

---

### Option B: IntelliJ IDEA

1. Open IntelliJ IDEA and select **Open** (or **File > Open...**).
2. Browse to and select the `LibraryManagementSystem` folder.
3. In IntelliJ, open **File > Project Structure...** (or press `Ctrl+Alt+Shift+S` / `Cmd+;`):
   - Under **Project**, verify **SDK** is set to Java 17 or higher.
   - Under **Libraries**, click the `+` icon, select **Java**, and choose `LibraryManagementSystem/lib/sqlite-jdbc.jar`. Click **Apply**.
   - Under **Modules**, ensure the `src` folder is marked as **Sources**.
4. Navigate to `src/Main.java`, right-click, and select **Run 'Main.main()'**.

---

### Option C: Eclipse IDE

1. Open Eclipse and choose **File > New > Java Project**.
2. Uncheck "Use default location", click **Browse**, and select the `LibraryManagementSystem` folder.
3. Set execution environment JRE to **JavaSE-17** (or newer). Click **Next**.
4. In the **Libraries** tab, click **Add JARs...** (or **Add External JARs...**), browse to `lib/sqlite-jdbc.jar`, and select it.
5. Click **Finish**.
6. In Package Explorer, right-click `src/Main.java` > **Run As > Java Application**.

---

### Option D: Apache NetBeans

1. Open NetBeans and choose **File > New Project > Java with Ant > Java Project with Existing Sources**.
2. Set Project Name to `LibraryManagementSystem`, click **Next**.
3. Under **Source Package Folders**, add the `src` directory. Click **Finish**.
4. Right-click the project in the Projects panel > **Properties > Libraries**.
5. Under **Compile**, click **Add JAR/Folder** and select `lib/sqlite-jdbc.jar`. Click **OK**.
6. Right-click `Main.java` and select **Run File**.

---

## 🧪 Automated Test Suite

The project includes an automated test runner (`SystemTest.java`) that verifies all 17 specification requirements in headless mode without requiring a display server:

```bash
# Run test suite:
./test.sh

# Or directly:
java -Djava.awt.headless=true -cp bin:lib/sqlite-jdbc.jar SystemTest
```

### Automated Verification Results (41 / 41 Tests Passing):
- `[PASS]` Database file created on first launch
- `[PASS]` Connection established with foreign keys
- `[PASS]` Default admin account authenticates successfully
- `[PASS]` Incorrect password rejected
- `[PASS]` Non-existent username rejected
- `[PASS]` Book added with generated ID
- `[PASS]` Book fetched accurately
- `[PASS]` Book edited successfully
- `[PASS]` Book search finds title
- `[PASS]` Book deactivated
- `[PASS]` Book reactivated
- `[PASS]` Duplicate ISBN rejected with validation message
- `[PASS]` Multiple books with absent/null ISBN supported
- `[PASS]` Member registered with generated ID
- `[PASS]` Member search finds registered member
- `[PASS]` Initial available copies equal total copies
- `[PASS]` Book issued successfully
- `[PASS]` Available copies reduced by 1 after issue
- `[PASS]` Issuing book with 0 available copies rejected
- `[PASS]` Inactive member borrowing rejected
- `[PASS]` Member successfully borrowed 3 books
- `[PASS]` 4th book borrow attempt rejected (3-book limit enforced)
- `[PASS]` Availability decreased to 1 after loan
- `[PASS]` Availability restored to 2 after return
- `[PASS]` Returning an already returned loan is rejected
- `[PASS]` Unreturned loan past due date marked as overdue
- `[PASS]` Overdue days calculated accurately
- `[PASS]` Returned loan has isOverdue() == false
- `[PASS]` Status reports Returned
- `[PASS]` Hard delete of book with loan history prevented
- `[PASS]` Hard delete of member with loan history prevented
- `[PASS]` Historical loan record intact after deactivation
- `[PASS]` Data persists in SQLite database across reconnection
- `[PASS]` Invalid database query caught gracefully without unhandled exception
- `[PASS]` Sample data loader seeded demo records
- `[PASS]` Demo catalog has active titles
- `[PASS]` Demo catalog has physical copies
- `[PASS]` Demo catalog has active loans
- `[PASS]` Demo catalog has overdue loan for demo
- `[PASS]` Demo catalog available copies = total - active
- `[PASS]` Sample data loader is idempotent (does not duplicate on rerun)

---

## 📋 Manual Testing Checklist

Follow this checklist during an evaluation or project defense:

| # | Test Scenario | Steps to Execute | Expected Result |
| :---: | :--- | :--- | :--- |
| **1** | **First Launch** | Delete `data/library.db` and launch `Main`. | `data/library.db` is created automatically with schema and default `admin` user. |
| **2** | **Valid Login** | Enter `admin` / `admin123` and click "Sign In". | Logs in successfully and displays the Main Dashboard. |
| **3** | **Invalid Login** | Enter `admin` / `wrongpass`. | Shows error message "Invalid username or password". Access denied. |
| **4** | **Add Book** | Go to Books > "➕ Add Book" > Fill details > Save. | New book appears in table; available copies equal total copies. |
| **5** | **Duplicate ISBN** | Add another book with the exact same ISBN. | Validation error appears: "A book with ISBN ... already exists." |
| **6** | **Empty ISBN** | Add two different books leaving ISBN empty. | Both books save successfully (SQLite handles multiple NULLs). |
| **7** | **Register Member** | Go to Members > "➕ Register Member" > Save. | Member appears in table with active status and $0 / 3$ loans. |
| **8** | **Issue Book** | Go to Issue Book > Select member & book > Confirm. | Transaction succeeds. Dashboard shows active loans $+1$, available copies $-1$. |
| **9** | **Borrowing Limit** | Issue 3 books to the same member, then attempt a 4th. | 4th attempt is blocked: "Member has reached maximum borrowing limit of 3 books." |
| **10**| **Stock Depletion** | Attempt to issue a book when Available Copies is 0. | Blocked: "No copies of ... are currently available for borrowing." |
| **11**| **Return Book** | Go to Return Book > Select loan > "Process Return". | Loan is marked returned. Book available copies count increases by 1. |
| **12**| **Double Return** | Attempt to return the same loan again. | Table removes active loan; duplicate return is impossible. |
| **13**| **Overdue Detection**| Check Return Book or Loan Records for past due loans. | Overdue status shows warning with days overdue; return prompts alert popup. |
| **14**| **Returned vs Overdue**| Check Loan Records filter for "Returned". | Returned loans display "Returned" and are never classified as overdue. |
| **15**| **Deactivation Safety**| Try deactivating a book with active loans; toggle inactive. | Cannot deactivate with active checkout; historical records stay preserved. |
| **16**| **Data Persistence**| Close the application and relaunch. | All added books, members, and loan history persist unchanged. |

---

## 🔧 Troubleshooting Guide

### Q1: `ClassNotFoundException: org.sqlite.JDBC`
- **Cause**: The SQLite JDBC JAR is missing from the classpath.
- **Solution**:
  - In terminal: Ensure `-cp bin:lib/sqlite-jdbc.jar` (Linux/Mac) or `-cp "bin;lib/sqlite-jdbc.jar"` (Windows) is specified.
  - In IDE: Ensure `lib/sqlite-jdbc.jar` is added under Project Libraries / Build Path.

### Q2: GUI looks pixelated or small on 4K / High-DPI screens
- **Solution**: The application uses standard Java Look and Feel. You can enable automatic UI scaling by running with:
  ```bash
  java -Dsun.java2d.uiScale=2.0 -cp bin:lib/sqlite-jdbc.jar Main
  ```

### Q3: How to reset the database to factory clean state?
- Delete the file `data/library.db`. On the next application run, `library.db` will be recreated with the default admin account.

---

*Developed as a Java Mini Project. Perfect for college demonstrations, viva presentations, and laboratory evaluations.*
