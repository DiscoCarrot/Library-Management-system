/**
 * Data carrier for library dashboard summary statistics.
 */
public class DashboardStats {
    private int totalBookTitles;
    private int totalPhysicalCopies;
    private int availableCopies;
    private int totalMembers;
    private int activeLoans;
    private int overdueLoans;

    public DashboardStats(int totalBookTitles, int totalPhysicalCopies, int availableCopies,
                          int totalMembers, int activeLoans, int overdueLoans) {
        this.totalBookTitles = totalBookTitles;
        this.totalPhysicalCopies = totalPhysicalCopies;
        this.availableCopies = availableCopies;
        this.totalMembers = totalMembers;
        this.activeLoans = activeLoans;
        this.overdueLoans = overdueLoans;
    }

    public int getTotalBookTitles() {
        return totalBookTitles;
    }

    public int getTotalPhysicalCopies() {
        return totalPhysicalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public int getTotalMembers() {
        return totalMembers;
    }

    public int getActiveLoans() {
        return activeLoans;
    }

    public int getOverdueLoans() {
        return overdueLoans;
    }
}
