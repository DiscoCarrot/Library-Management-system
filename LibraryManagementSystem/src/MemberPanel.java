import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Management panel for searching, viewing, adding, editing, and deactivating library members.
 */
public class MemberPanel extends JPanel {

    private final MainFrame mainFrame;
    private final MemberDAO memberDAO = new MemberDAO();

    private JTextField txtSearch;
    private JCheckBox chkShowInactive;
    private JTable memberTable;
    private DefaultTableModel tableModel;
    private List<Member> currentMembers = new ArrayList<>();

    public MemberPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
        loadMembers();
    }

    private void initUI() {
        // Top Section: Title & Search Toolbar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // Header Row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Member Management");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel lblSubtitle = new JLabel("Register patrons, update contact details, monitor active loan quotas, and manage membership status");
        lblSubtitle.setFont(UITheme.FONT_REGULAR);
        lblSubtitle.setForeground(UITheme.TEXT_MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        titleBlock.add(lblTitle);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(lblSubtitle);
        headerRow.add(titleBlock, BorderLayout.WEST);

        // Action Buttons Row (Add, Edit, Deactivate)
        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBtns.setOpaque(false);

        JButton btnAdd = UITheme.createPrimaryButton("➕ Register Member");
        btnAdd.addActionListener(e -> openAddDialog());

        JButton btnEdit = UITheme.createSecondaryButton("✏️ Edit Member");
        btnEdit.addActionListener(e -> openEditDialog());

        JButton btnToggleActive = UITheme.createButton("🔄 Toggle Status", UITheme.COLOR_WARNING);
        btnToggleActive.addActionListener(e -> toggleMemberStatus());

        actionBtns.add(btnAdd);
        actionBtns.add(btnEdit);
        actionBtns.add(btnToggleActive);

        headerRow.add(actionBtns, BorderLayout.EAST);
        topContainer.add(headerRow);
        topContainer.add(Box.createVerticalStrut(14));

        // Search Filter Bar
        JPanel searchBar = UITheme.createCardPanel();
        searchBar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 4));

        JLabel lblSearch = new JLabel("Search Members:");
        lblSearch.setFont(UITheme.FONT_BOLD);
        searchBar.add(lblSearch);

        txtSearch = UITheme.createTextField(22);
        txtSearch.setToolTipText("Search by Name, Email, Phone, or Member ID");
        txtSearch.addActionListener(e -> loadMembers());
        searchBar.add(txtSearch);

        JButton btnSearch = UITheme.createPrimaryButton("🔍 Search");
        btnSearch.addActionListener(e -> loadMembers());
        searchBar.add(btnSearch);

        JButton btnClear = UITheme.createSecondaryButton("Reset");
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            chkShowInactive.setSelected(false);
            loadMembers();
        });
        searchBar.add(btnClear);

        chkShowInactive = new JCheckBox("Include Inactive Members");
        chkShowInactive.setOpaque(false);
        chkShowInactive.setFont(UITheme.FONT_REGULAR);
        chkShowInactive.addActionListener(e -> loadMembers());
        searchBar.add(chkShowInactive);

        topContainer.add(searchBar);
        add(topContainer, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"ID", "Full Name", "Email", "Phone", "Registration Date", "Active Loans", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        memberTable = new JTable(tableModel);
        memberTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(memberTable);

        // Adjust column widths
        memberTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        memberTable.getColumnModel().getColumn(1).setPreferredWidth(190);
        memberTable.getColumnModel().getColumn(2).setPreferredWidth(190);
        memberTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        memberTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        memberTable.getColumnModel().getColumn(5).setPreferredWidth(90);
        memberTable.getColumnModel().getColumn(6).setPreferredWidth(85);

        // Double-click row to edit
        memberTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    openEditDialog();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(memberTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Loads members asynchronously using SwingWorker.
     */
    public void loadMembers() {
        String query = txtSearch.getText().trim();
        boolean showInactive = chkShowInactive.isSelected();

        SwingWorker<List<Member>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Member> doInBackground() throws Exception {
                return memberDAO.searchMembers(query, showInactive);
            }

            @Override
            protected void done() {
                try {
                    currentMembers = get();
                    tableModel.setRowCount(0);
                    for (Member m : currentMembers) {
                        tableModel.addRow(new Object[]{
                            m.getId(),
                            m.getName(),
                            m.getEmail() != null ? m.getEmail() : "—",
                            m.getPhone() != null ? m.getPhone() : "—",
                            m.getRegistrationDate(),
                            m.getActiveLoansCount() + " / 3",
                            m.isActive() ? "Active" : "Inactive"
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MemberPanel.this, "Error loading members: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private Member getSelectedMember() {
        int selectedRow = memberTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < currentMembers.size()) {
            return currentMembers.get(selectedRow);
        }
        return null;
    }

    private void openAddDialog() {
        MemberDialog dialog = new MemberDialog(mainFrame, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadMembers();
            mainFrame.refreshDashboard();
        }
    }

    private void openEditDialog() {
        Member selected = getSelectedMember();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a member from the table to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        MemberDialog dialog = new MemberDialog(mainFrame, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadMembers();
            mainFrame.refreshDashboard();
        }
    }

    private void toggleMemberStatus() {
        Member selected = getSelectedMember();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a member from the table to change status.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean willDeactivate = selected.isActive();
        String actionName = willDeactivate ? "deactivate" : "activate";

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to " + actionName + " member '" + selected.getName() + "' (ID: " + selected.getId() + ")?\n"
            + (willDeactivate ? "Deactivated members cannot borrow new books. Historical records are preserved." : "The member will be able to borrow books again."),
            "Confirm " + (willDeactivate ? "Deactivation" : "Activation"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                memberDAO.setMemberActive(selected.getId(), !willDeactivate);
                loadMembers();
                mainFrame.refreshDashboard();
                JOptionPane.showMessageDialog(this, "Member has been " + (willDeactivate ? "deactivated" : "activated") + " successfully.");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Deactivate", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
