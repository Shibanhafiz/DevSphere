package com.devsphere.ui;

import com.devsphere.model.Session;
import com.devsphere.service.AuthService;
import com.devsphere.service.DuplicateUserException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.Arrays;

public class AuthFrame extends JFrame {

    private static final String LOGIN_CARD = "login";
    private static final String REGISTER_CARD = "register";
    private static final Color NAVY = new Color(24, 39, 75);
    private static final Color BLUE = new Color(54, 104, 220);
    private static final Color BACKGROUND = new Color(246, 248, 252);

    private final AuthService authService;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    public AuthFrame(AuthService authService) {
        this.authService = authService;
        setTitle("DevSphere - Developer Collaboration Platform");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 620);
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(null);

        add(createBrandPanel(), BorderLayout.WEST);
        cardPanel.setBackground(BACKGROUND);
        cardPanel.add(createLoginPanel(), LOGIN_CARD);
        cardPanel.add(createRegistrationPanel(), REGISTER_CARD);
        add(cardPanel, BorderLayout.CENTER);
    }

    private JPanel createBrandPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(300, 0));
        panel.setBackground(NAVY);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(8, 28, 8, 28);

        JLabel logo = new JLabel("DEVSPHERE");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 26));
        constraints.gridy = 0;
        panel.add(logo, constraints);

        JLabel tagline = new JLabel("<html>Build together.<br>Understand more.</html>");
        tagline.setForeground(new Color(205, 218, 250));
        tagline.setFont(new Font("SansSerif", Font.PLAIN, 17));
        constraints.gridy = 1;
        panel.add(tagline, constraints);

        JLabel description = new JLabel("A focused workspace for developers and teams.");
        description.setForeground(new Color(180, 194, 226));
        description.setFont(new Font("SansSerif", Font.PLAIN, 13));
        constraints.gridy = 2;
        constraints.insets = new Insets(30, 28, 8, 28);
        panel.add(description, constraints);

        return panel;
    }

    private JPanel createLoginPanel() {
        JPanel panel = createFormPanel();
        GridBagConstraints constraints = formConstraints();
        addFormTitle(panel, constraints, "Welcome back", "Sign in to continue to DevSphere");

        JTextField usernameOrEmail = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        addField(panel, constraints, 2, "Username or email", usernameOrEmail);
        addField(panel, constraints, 3, "Password", password);

        JButton loginButton = createPrimaryButton("Sign in");
        loginButton.addActionListener(event -> {
            char[] passwordValue = password.getPassword();
            try {
                Session session = authService.login(usernameOrEmail.getText(), passwordValue);
                if (session == null) {
                    showError("Invalid username/email or password.");
                } else {
                    password.setText("");
                    new DashboardFrame(authService, session).setVisible(true);
                    dispose();
                }
            } catch (SQLException exception) {
                showDatabaseError();
            } finally {
                Arrays.fill(passwordValue, '\0');
            }
        });
        addButton(panel, constraints, 4, loginButton);

        JButton registerLink = createLinkButton("New to DevSphere? Create an account");
        registerLink.addActionListener(event -> cardLayout.show(cardPanel, REGISTER_CARD));
        addButton(panel, constraints, 5, registerLink);
        return panel;
    }

    private JPanel createRegistrationPanel() {
        JPanel panel = createFormPanel();
        GridBagConstraints constraints = formConstraints();
        addFormTitle(panel, constraints, "Create your account", "Join your developer workspace");

        JTextField fullName = new JTextField(22);
        JTextField username = new JTextField(22);
        JTextField email = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        JPasswordField confirmPassword = new JPasswordField(22);
        JTextField skills = new JTextField(22);
        addField(panel, constraints, 2, "Full name *", fullName);
        addField(panel, constraints, 3, "Username *", username);
        addField(panel, constraints, 4, "Email *", email);
        addField(panel, constraints, 5, "Password *", password);
        addField(panel, constraints, 6, "Confirm password *", confirmPassword);
        addField(panel, constraints, 7, "Skills", skills);

        JButton registerButton = createPrimaryButton("Create account");
        registerButton.addActionListener(event -> {
            char[] passwordValue = password.getPassword();
            char[] confirmValue = confirmPassword.getPassword();
            try {
                authService.register(fullName.getText(), username.getText(), email.getText(),
                        passwordValue, confirmValue, skills.getText());
                showInfo("Your account has been created. You can now sign in.");
                clearRegistrationFields(fullName, username, email, password, confirmPassword, skills);
                cardLayout.show(cardPanel, LOGIN_CARD);
            } catch (IllegalArgumentException | DuplicateUserException exception) {
                showError(exception.getMessage());
            } catch (SQLException exception) {
                showDatabaseError();
            } finally {
                Arrays.fill(passwordValue, '\0');
                Arrays.fill(confirmValue, '\0');
            }
        });
        addButton(panel, constraints, 8, registerButton);

        JButton backButton = createLinkButton("Already have an account? Sign in");
        backButton.addActionListener(event -> cardLayout.show(cardPanel, LOGIN_CARD));
        addButton(panel, constraints, 9, backButton);
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(32, 46, 32, 46));
        return panel;
    }

    private GridBagConstraints formConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(6, 0, 6, 0);
        return constraints;
    }

    private void addFormTitle(JPanel panel, GridBagConstraints constraints, String title, String subtitle) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(NAVY);
        constraints.gridy = 0;
        panel.add(titleLabel, constraints);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(90, 100, 120));
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 22, 0);
        panel.add(subtitleLabel, constraints);
        constraints.insets = new Insets(5, 0, 5, 0);
    }

    private void addField(JPanel panel, GridBagConstraints constraints, int row,
                          String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(NAVY);
        constraints.gridy = row;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        panel.add(label, constraints);

        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(280, 34));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 216, 228)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.insets = new Insets(5, 16, 5, 0);
        panel.add(field, constraints);
        constraints.gridx = 0;
        constraints.insets = new Insets(5, 0, 5, 0);
    }

    private void addButton(JPanel panel, GridBagConstraints constraints, int row, JButton button) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(12, 0, 4, 0);
        panel.add(button, constraints);
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(BLUE);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return button;
    }

    private JButton createLinkButton(String text) {
        JButton button = new JButton(text);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setForeground(BLUE);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return button;
    }

    private void clearRegistrationFields(JTextField fullName, JTextField username, JTextField email,
                                         JPasswordField password, JPasswordField confirmPassword,
                                         JTextField skills) {
        fullName.setText("");
        username.setText("");
        email.setText("");
        password.setText("");
        confirmPassword.setText("");
        skills.setText("");
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "DevSphere", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Please check your details", JOptionPane.WARNING_MESSAGE);
    }

    private void showDatabaseError() {
        JOptionPane.showMessageDialog(this,
                "The database operation could not be completed. Check MySQL and your database configuration.",
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
