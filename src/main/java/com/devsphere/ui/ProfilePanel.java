package com.devsphere.ui;

import com.devsphere.model.ProfileCompletion;
import com.devsphere.model.ProfileUpdate;
import com.devsphere.model.ProfileVisibility;
import com.devsphere.model.User;
import com.devsphere.service.ProfileService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.function.Consumer;

public class ProfilePanel extends JPanel {

    private static final Color NAVY = new Color(24, 39, 75);
    private static final Color BLUE = new Color(54, 104, 220);
    private static final Color BACKGROUND = new Color(246, 248, 252);

    private final ProfileService profileService;
    private final long userId;
    private final Consumer<User> onSaved;

    private final JTextField fullName = new JTextField();
    private final JTextField username = new JTextField();
    private final JTextField email = new JTextField();
    private final JTextField headline = new JTextField();
    private final JTextArea biography = new JTextArea(4, 24);
    private final JTextField skills = new JTextField();
    private final JTextField githubUrl = new JTextField();
    private final JTextField linkedinUrl = new JTextField();
    private final JTextField portfolioUrl = new JTextField();
    private final JComboBox<ProfileVisibility> visibility = new JComboBox<>(ProfileVisibility.values());
    private final JLabel completionLabel = new JLabel();
    private final JLabel completedLabel = new JLabel();
    private final JLabel missingLabel = new JLabel();
    private User loadedProfile;

    public ProfilePanel(ProfileService profileService, long userId, Consumer<User> onSaved) {
        this.profileService = profileService;
        this.userId = userId;
        this.onSaved = onSaved;
        setLayout(new BorderLayout());
        UiStyle.page(this);

        add(createHeader(), BorderLayout.NORTH);
        add(new JScrollPane(createForm()), BorderLayout.CENTER);
        configureFields();
        reload();
    }

    public void reload() {
        try {
            loadedProfile = profileService.getProfile(userId);
            populateFields(loadedProfile);
            updateCompletion(loadedProfile);
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this,
                    "The profile could not be loaded. Check the database connection.",
                    "Profile Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BACKGROUND);
        header.setBorder(BorderFactory.createEmptyBorder(28, 42, 14, 42));

        JLabel title = new JLabel("My profile");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(UiStyle.NAVY);
        header.add(title, BorderLayout.WEST);

        JButton previewButton = UiStyle.primaryButton("Public preview");
        previewButton.addActionListener(event -> showPreview());
        header.add(previewButton, BorderLayout.EAST);
        return header;
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BACKGROUND);
        form.setBorder(BorderFactory.createEmptyBorder(10, 42, 36, 42));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 1;
        constraints.insets = new Insets(6, 0, 6, 14);

        addField(form, constraints, 0, "Full name *", fullName, false);
        addField(form, constraints, 1, "Username", username, true);
        addField(form, constraints, 2, "Email", email, true);
        addField(form, constraints, 3, "Professional headline", headline, false);
        addArea(form, constraints, 4, "About me / biography", biography);
        addField(form, constraints, 5, "Technical skills", skills, false);
        addField(form, constraints, 6, "GitHub profile URL", githubUrl, false);
        addField(form, constraints, 7, "LinkedIn profile URL", linkedinUrl, false);
        addField(form, constraints, 8, "Personal portfolio URL", portfolioUrl, false);

        JLabel visibilityLabel = createLabel("Profile visibility");
        constraints.gridy = 9;
        constraints.gridx = 0;
        constraints.weightx = 0;
        form.add(visibilityLabel, constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        form.add(visibility, constraints);

        JPanel completionPanel = createCompletionPanel();
        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(24, 0, 14, 0);
        form.add(completionPanel, constraints);

        JButton saveButton = UiStyle.primaryButton("Save profile");
        saveButton.addActionListener(event -> saveProfile());
        constraints.gridy = 11;
        constraints.insets = new Insets(4, 0, 4, 0);
        form.add(saveButton, constraints);
        return form;
    }

    private JPanel createCompletionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 229, 239)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;

        completionLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        completionLabel.setForeground(NAVY);
        constraints.gridy = 0;
        panel.add(completionLabel, constraints);

        completedLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        constraints.gridy = 1;
        constraints.insets = new Insets(8, 0, 3, 0);
        panel.add(completedLabel, constraints);

        missingLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        constraints.gridy = 2;
        constraints.insets = new Insets(3, 0, 0, 0);
        panel.add(missingLabel, constraints);
        return panel;
    }

    private void addField(JPanel form, GridBagConstraints constraints, int row,
                          String labelText, JTextField field, boolean readOnly) {
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        form.add(createLabel(labelText), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        field.setPreferredSize(new Dimension(420, 34));
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setEditable(!readOnly);
        if (readOnly) {
            field.setBackground(new Color(232, 235, 242));
        }
        form.add(field, constraints);
    }

    private void addArea(JPanel form, GridBagConstraints constraints, int row,
                         String labelText, JTextArea area) {
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.weightx = 0;
        form.add(createLabel(labelText), constraints);

        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("SansSerif", Font.PLAIN, 14));
        area.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
        constraints.gridx = 1;
        constraints.weightx = 1;
        form.add(new JScrollPane(area), constraints);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(NAVY);
        return label;
    }

    private void configureFields() {
        fullName.setToolTipText("Your name as it should appear on your profile");
        skills.setToolTipText("Separate skills with commas; remove a skill by deleting it");
        githubUrl.setToolTipText("Example: https://github.com/username");
        linkedinUrl.setToolTipText("Example: https://www.linkedin.com/in/username");
        portfolioUrl.setToolTipText("Example: https://your-portfolio.example");
    }

    private void populateFields(User profile) {
        fullName.setText(value(profile.fullName()));
        username.setText(value(profile.username()));
        email.setText(value(profile.email()));
        headline.setText(value(profile.professionalHeadline()));
        biography.setText(value(profile.biography()));
        skills.setText(value(profile.skills()));
        githubUrl.setText(value(profile.githubUrl()));
        linkedinUrl.setText(value(profile.linkedinUrl()));
        portfolioUrl.setText(value(profile.portfolioUrl()));
        visibility.setSelectedItem(profile.profileVisibility());
    }

    private void updateCompletion(User profile) {
        ProfileCompletion completion = profileService.calculateCompletion(profile);
        completionLabel.setText("Profile completion: " + completion.percentage() + "%");
        completedLabel.setText("Completed: " + join(completion.completedFields()));
        missingLabel.setText("Recommended next: " + join(completion.missingFields()));
    }

    private void saveProfile() {
        ProfileVisibility selectedVisibility = (ProfileVisibility) visibility.getSelectedItem();
        ProfileUpdate update = new ProfileUpdate(
                fullName.getText(),
                headline.getText(),
                biography.getText(),
                skills.getText(),
                githubUrl.getText(),
                linkedinUrl.getText(),
                portfolioUrl.getText(),
                selectedVisibility
        );

        try {
            profileService.updateProfile(userId, update);
            loadedProfile = profileService.getProfile(userId);
            populateFields(loadedProfile);
            updateCompletion(loadedProfile);
            onSaved.accept(loadedProfile);
            JOptionPane.showMessageDialog(this, "Your profile has been saved.",
                    "Profile updated", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "Please check your profile", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this,
                    "The profile could not be saved. Check the database connection.",
                    "Profile Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPreview() {
        if (loadedProfile == null) {
            return;
        }

        if (!profileService.canBeViewedPublicly(loadedProfile)) {
            JOptionPane.showMessageDialog(this,
                    "This profile is private. Other users cannot view its details.",
                    "Private profile", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel name = new JLabel(value(loadedProfile.fullName()));
        name.setFont(new Font("SansSerif", Font.BOLD, 22));
        name.setForeground(UiStyle.NAVY);
        content.add(name);
        content.add(new JLabel("@" + value(loadedProfile.username())));
        addPreviewText(content, loadedProfile.professionalHeadline());
        addPreviewText(content, loadedProfile.biography());
        addPreviewText(content, "Skills: " + value(loadedProfile.skills()));
        content.add(Box.createVerticalStrut(8));
        if (!value(loadedProfile.githubUrl()).isBlank()) content.add(UiStyle.externalLink(this, "GitHub", loadedProfile.githubUrl()));
        if (!value(loadedProfile.linkedinUrl()).isBlank()) content.add(UiStyle.externalLink(this, "LinkedIn", loadedProfile.linkedinUrl()));
        if (!value(loadedProfile.portfolioUrl()).isBlank()) content.add(UiStyle.externalLink(this, "Portfolio", loadedProfile.portfolioUrl()));
        JOptionPane.showMessageDialog(this, content,
                "Public profile preview", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addPreviewText(JPanel panel, String text) {
        if (text != null && !text.isBlank()) {
            JLabel label = new JLabel("<html>" + escape(text) + "</html>");
            label.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            panel.add(label);
        }
    }

    private String paragraph(String value) {
        return value == null || value.isBlank() ? "" : "<p>" + escape(value) + "</p>";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }

    private String join(java.util.List<String> values) {
        return values.isEmpty() ? "None yet" : String.join(", ", values);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
