import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for member management and borrowing validation.
 */
public class MemberDAO {

    /**
     * Registers a new member.
     *
     * @param member Member details
     * @return Generated member ID
     * @throws IllegalArgumentException on validation failure
     * @throws SQLException on database error
     */
    public int addMember(Member member) throws SQLException {
        if (member.getName() == null || member.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Member full name is required.");
        }

        if (member.getRegistrationDate() == null || member.getRegistrationDate().trim().isEmpty()) {
            member.setRegistrationDate(LocalDate.now().toString());
        }

        String sql = """
            INSERT INTO members (name, email, phone, registration_date, active)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, member.getName().trim());
            if (member.getEmail() != null && !member.getEmail().trim().isEmpty()) {
                pstmt.setString(2, member.getEmail().trim());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            if (member.getPhone() != null && !member.getPhone().trim().isEmpty()) {
                pstmt.setString(3, member.getPhone().trim());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            pstmt.setString(4, member.getRegistrationDate().trim());
            pstmt.setInt(5, member.isActive() ? 1 : 0);

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    member.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Updates an existing member's information.
     */
    public boolean updateMember(Member member) throws SQLException {
        if (member.getName() == null || member.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Member full name is required.");
        }

        String sql = """
            UPDATE members
            SET name = ?, email = ?, phone = ?, active = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, member.getName().trim());
            if (member.getEmail() != null && !member.getEmail().trim().isEmpty()) {
                pstmt.setString(2, member.getEmail().trim());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            if (member.getPhone() != null && !member.getPhone().trim().isEmpty()) {
                pstmt.setString(3, member.getPhone().trim());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            pstmt.setInt(4, member.isActive() ? 1 : 0);
            pstmt.setInt(5, member.getId());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Toggles member active status (deactivates or activates member).
     * Historical records remain intact when deactivated.
     */
    public boolean setMemberActive(int memberId, boolean active) throws SQLException {
        if (!active) {
            int activeLoans = getActiveLoansCountForMember(memberId);
            if (activeLoans > 0) {
                throw new IllegalArgumentException("Cannot deactivate member with " + activeLoans
                    + " active book loans. Return all borrowed books before deactivating.");
            }
        }

        String sql = "UPDATE members SET active = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, active ? 1 : 0);
            pstmt.setInt(2, memberId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Safely deletes a member only if they have NO loan history.
     */
    public boolean deleteMember(int memberId) throws SQLException {
        if (hasLoanHistory(memberId)) {
            throw new IllegalArgumentException("Cannot delete member with borrowing history. Deactivate the member instead to preserve library records.");
        }

        String sql = "DELETE FROM members WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Checks if member has any historical loan records.
     */
    public boolean hasLoanHistory(int memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE member_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Returns the count of books currently borrowed by this member (unreturned).
     */
    public int getActiveLoansCountForMember(int memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE member_id = ? AND return_date IS NULL";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Retrieves member by ID.
     */
    public Member getMemberById(int id) throws SQLException {
        String sql = """
            SELECT m.id, m.name, m.email, m.phone, m.registration_date, m.active,
                   (SELECT COUNT(*) FROM loans l WHERE l.member_id = m.id AND l.return_date IS NULL) AS active_loans
            FROM members m
            WHERE m.id = ?
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMember(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all members.
     */
    public List<Member> getAllMembers(boolean includeInactive) throws SQLException {
        String sql = """
            SELECT m.id, m.name, m.email, m.phone, m.registration_date, m.active,
                   (SELECT COUNT(*) FROM loans l WHERE l.member_id = m.id AND l.return_date IS NULL) AS active_loans
            FROM members m
            WHERE (? = 1 OR m.active = 1)
            ORDER BY m.name COLLATE NOCASE ASC
        """;
        List<Member> members = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, includeInactive ? 1 : 0);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    members.add(mapRowToMember(rs));
                }
            }
        }
        return members;
    }

    /**
     * Searches members by name, email, phone, or ID.
     */
    public List<Member> searchMembers(String query, boolean includeInactive) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return getAllMembers(includeInactive);
        }

        String searchPattern = "%" + query.trim().toLowerCase() + "%";
        String idQuery = query.trim();

        String sql = """
            SELECT m.id, m.name, m.email, m.phone, m.registration_date, m.active,
                   (SELECT COUNT(*) FROM loans l WHERE l.member_id = m.id AND l.return_date IS NULL) AS active_loans
            FROM members m
            WHERE (? = 1 OR m.active = 1)
              AND (LOWER(m.name) LIKE ? OR LOWER(COALESCE(m.email, '')) LIKE ? OR LOWER(COALESCE(m.phone, '')) LIKE ? OR CAST(m.id AS TEXT) = ?)
            ORDER BY m.name COLLATE NOCASE ASC
        """;
        List<Member> members = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, includeInactive ? 1 : 0);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            pstmt.setString(4, searchPattern);
            pstmt.setString(5, idQuery);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    members.add(mapRowToMember(rs));
                }
            }
        }
        return members;
    }

    /**
     * Retrieves active members eligible to borrow books (active and currently have < 3 active loans).
     */
    public List<Member> getActiveMembers() throws SQLException {
        return getAllMembers(false);
    }

    private Member mapRowToMember(ResultSet rs) throws SQLException {
        return new Member(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("registration_date"),
            rs.getInt("active") == 1,
            rs.getInt("active_loans")
        );
    }
}
