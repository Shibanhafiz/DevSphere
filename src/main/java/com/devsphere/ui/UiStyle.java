package com.devsphere.ui;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

/** Shared visual language and safe desktop-link behavior for DevSphere Swing screens. */
public final class UiStyle {
    public static final Color NAVY = new Color(24, 39, 75);
    public static final Color BLUE = new Color(54, 104, 220);
    public static final Color BLUE_DARK = new Color(39, 78, 170);
    public static final Color BACKGROUND = new Color(246, 248, 252);
    public static final Color TEXT_MUTED = new Color(90, 100, 120);
    public static final Color BORDER = new Color(224, 229, 239);
    public static final Color SUCCESS = new Color(34, 126, 78);
    public static final Color WARNING = new Color(166, 106, 18);

    private UiStyle() { }

    public static void page(JPanel panel) {
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 42, 36, 42));
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 28));
        label.setForeground(NAVY);
        return label;
    }

    public static JLabel sectionHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        label.setForeground(NAVY);
        return label;
    }

    public static void button(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(BLUE);
        button.setForeground(Color.WHITE);
        button(button);
        return button;
    }

    public static JButton linkButton(String text) {
        JButton button = new JButton(text);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setForeground(BLUE);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JPanel card() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        return panel;
    }

    public static JButton externalLink(Component parent, String label, String url) {
        JButton button = linkButton(label);
        button.setToolTipText(url);
        button.addActionListener(event -> openExternal(parent, url));
        return button;
    }

    public static void openExternal(Component parent, String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("Only valid HTTP and HTTPS links can be opened.");
            }
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IllegalStateException("This computer does not provide a supported web browser.");
            }
            Desktop.getDesktop().browse(uri);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(parent, exception.getMessage(), "Unable to open link", JOptionPane.WARNING_MESSAGE);
        }
    }
}
