import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Visual styling theme and reusable UI component factories for a modern Swing interface.
 */
public class UITheme {

    // Color Palette
    public static final Color COLOR_PRIMARY = new Color(37, 99, 235);      // Royal Blue #2563EB
    public static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216);// Darker Blue #1D4ED8
    public static final Color COLOR_SUCCESS = new Color(22, 163, 74);      // Green #16A34A
    public static final Color COLOR_DANGER = new Color(220, 38, 38);       // Red #DC2626
    public static final Color COLOR_WARNING = new Color(217, 119, 6);      // Amber #D97706
    public static final Color COLOR_SECONDARY = new Color(100, 116, 139);  // Slate Gray #64748B

    public static final Color BG_MAIN = new Color(248, 250, 252);          // Light Gray #F8FAFC
    public static final Color BG_CARD = Color.WHITE;
    public static final Color BG_SIDEBAR = new Color(30, 41, 59);          // Dark Slate #1E293B
    public static final Color BG_SIDEBAR_HOVER = new Color(51, 65, 85);    // #334155
    public static final Color BG_SIDEBAR_ACTIVE = new Color(59, 130, 246);  // Accent Blue #3B82F6

    public static final Color TEXT_MAIN = new Color(15, 23, 42);           // #0F172A
    public static final Color TEXT_MUTED = new Color(100, 116, 139);       // #64748B
    public static final Color TEXT_LIGHT = new Color(241, 245, 249);       // #F1F5F9
    public static final Color BORDER_LIGHT = new Color(226, 232, 240);     // #E2E8F0

    // Typography
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_SECTION = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_REGULAR = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_METRIC = new Font("SansSerif", Font.BOLD, 26);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);

    /**
     * Creates a styled action button.
     */
    public static JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(FONT_BOLD);
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return button;
    }

    /**
     * Creates a primary blue button.
     */
    public static JButton createPrimaryButton(String text) {
        return createButton(text, COLOR_PRIMARY);
    }

    /**
     * Creates a secondary gray button.
     */
    public static JButton createSecondaryButton(String text) {
        return createButton(text, COLOR_SECONDARY);
    }

    /**
     * Creates a danger/red button.
     */
    public static JButton createDangerButton(String text) {
        return createButton(text, COLOR_DANGER);
    }

    /**
     * Creates a success/green button.
     */
    public static JButton createSuccessButton(String text) {
        return createButton(text, COLOR_SUCCESS);
    }

    /**
     * Creates a styled text field with clean padding and border.
     */
    public static JTextField createTextField(int columns) {
        JTextField tf = new JTextField(columns);
        tf.setFont(FONT_REGULAR);
        tf.setBorder(new CompoundBorder(
            new LineBorder(BORDER_LIGHT, 1, true),
            new EmptyBorder(6, 8, 6, 8)
        ));
        return tf;
    }

    /**
     * Creates a styled card container panel.
     */
    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_CARD);
        panel.setBorder(new CompoundBorder(
            new LineBorder(BORDER_LIGHT, 1, true),
            new EmptyBorder(16, 16, 16, 16)
        ));
        return panel;
    }

    /**
     * Styles a JTable for professional data presentation.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_REGULAR);
        table.setRowHeight(32);
        table.setShowGrid(true);
        table.setGridColor(BORDER_LIGHT);
        table.setSelectionBackground(new Color(219, 234, 254)); // Light Blue #DBEAFE
        table.setSelectionForeground(new Color(30, 58, 138));   // Dark Blue
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(TEXT_MAIN);
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_LIGHT));

        // Center align text for numeric/status columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        // Apply alternating row striping renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(TEXT_MAIN);
                }
                return c;
            }
        });
    }
}
