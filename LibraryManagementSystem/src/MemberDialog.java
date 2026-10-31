import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;

/**
 * Modal dialog for registering new library members or updating existing profiles.
 */
public class MemberDialog extends JDialog {

    private final MemberDAO memberDAO = new MemberDAO();
    private Member member;
    private boolean saved = false;

    private JTextField txtName;
    private JTextField txtEmail;
    private JTextField txtPhone;
    private JTextField txtRegDate;

    public MemberDialog(JFrame parent, Member memberToEdit) {
        super(parent, memberToEdit == null ? "Register New Member" : "Edit Member (ID: " + memberToEdit.getId() + ")", true);
        this.member = memberToEdit;

        setSize(440, 380);
        setLocationRelativeTo(parent);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel contentPane = new JPanel(new BorderLayout(12, 12));
        contentPane.setBackground(UITheme.BG_MAIN);
        contentPane.setBorder(new EmptyBorder(16, 20, 16, 20));
        setContentPane(contentPane);

        // Header Title
        JLabel lblHeader = new JLabel(member == null ? "👤 Register Member" : "✏️ Update Member Profile");
        lblHeader.setFont(UITheme.FONT_SUBTITLE);
        lblHeader.setForeground(UITheme.TEXT_MAIN);
        contentPane.add(lblHeader, BorderLayout.NORTH);

        // Form Fields
        JPanel formPanel = UITheme.createCardPanel();
        formPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(createFieldLabel("Full Name *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtName = UITheme.createTextField(20);
        formPanel.add(txtName, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        formPanel.add(createFieldLabel("Email Address"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtEmail = UITheme.createTextField(20);
        formPanel.add(txtEmail, gbc);

        // Phone
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        formPanel.add(createFieldLabel("Phone Number"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtPhone = UITheme.createTextField(20);
        formPanel.add(txtPhone, gbc);

        // Registration Date
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        formPanel.add(createFieldLabel("Reg. Date (YYYY-MM-DD)"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtRegDate = UITheme.createTextField(15);
        txtRegDate.setText(LocalDate.now().toString());
        formPanel.add(txtRegDate, gbc);

        contentPane.add(formPanel, BorderLayout.CENTER);

        // Populate if editing
        if (member != null) {
            txtName.setText(member.getName());
            txtEmail.setText(member.getEmail() != null ? member.getEmail() : "");
            txtPhone.setText(member.getPhone() != null ? member.getPhone() : "");
            txtRegDate.setText(member.getRegistrationDate());
            txtRegDate.setEditable(false); // Do not alter registration date on edit
        }

        // Action Buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomPanel.setOpaque(false);

        JButton btnCancel = UITheme.createSecondaryButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = UITheme.createPrimaryButton(member == null ? "Register Member" : "Update Member");
        btnSave.addActionListener(e -> saveMember());

        bottomPanel.add(btnCancel);
        bottomPanel.add(btnSave);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_MAIN);
        return lbl;
    }

    private void saveMember() {
        String name = txtName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String regDate = txtRegDate.getText().trim();

        if (name.isEmpty()) {
            showError("Member full name is required.");
            txtName.requestFocusInWindow();
            return;
        }

        if (regDate.isEmpty()) {
            regDate = LocalDate.now().toString();
        }

        try {
            LocalDate.parse(regDate);
        } catch (Exception e) {
            showError("Invalid registration date format. Use YYYY-MM-DD (e.g. 2024-05-15).");
            txtRegDate.requestFocusInWindow();
            return;
        }

        try {
            if (member == null) {
                Member newMember = new Member(0, name, email.isEmpty() ? null : email, phone.isEmpty() ? null : phone, regDate, true, 0);
                memberDAO.addMember(newMember);
                JOptionPane.showMessageDialog(this, "Member registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                member.setName(name);
                member.setEmail(email.isEmpty() ? null : email);
                member.setPhone(phone.isEmpty() ? null : phone);

                memberDAO.updateMember(member);
                JOptionPane.showMessageDialog(this, "Member details updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }

            saved = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Database error while saving member: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }

    public boolean isSaved() {
        return saved;
    }
}
