/**
 * Represents a registered library member.
 */
public class Member {
    private int id;
    private String name;
    private String email;
    private String phone;
    private String registrationDate; // ISO format YYYY-MM-DD
    private boolean active;
    private int activeLoansCount; // Derived dynamically

    public Member() {
        this.active = true;
    }

    public Member(int id, String name, String email, String phone,
                  String registrationDate, boolean active, int activeLoansCount) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.registrationDate = registrationDate;
        this.active = active;
        this.activeLoansCount = activeLoansCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = (email != null && !email.trim().isEmpty()) ? email.trim() : null;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getActiveLoansCount() {
        return activeLoansCount;
    }

    public void setActiveLoansCount(int activeLoansCount) {
        this.activeLoansCount = activeLoansCount;
    }

    @Override
    public String toString() {
        return name + " (ID: " + id + ")" + (!active ? " [INACTIVE]" : "");
    }
}
