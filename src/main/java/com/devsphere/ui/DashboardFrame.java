package com.devsphere.ui;

import com.devsphere.dao.UserDao;
import com.devsphere.dao.ProjectDao;
import com.devsphere.dao.IssueDao;
import com.devsphere.dao.ContributionDao;
import com.devsphere.model.Session;
import com.devsphere.model.User;
import com.devsphere.service.AuthService;
import com.devsphere.service.ProjectService;
import com.devsphere.service.IssueService;
import com.devsphere.service.ContributionService;
import com.devsphere.service.ProfileService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

public class DashboardFrame extends JFrame {

    private static final String DASHBOARD_CARD = "dashboard";
    private static final String PROFILE_CARD = "profile";
    private static final String PROJECTS_CARD = "projects";
    private static final String ISSUES_CARD = "issues";
    private static final String CONTRIBUTION_CARD = "contribution";
    private static final Color NAVY = new Color(24, 39, 75);
    private static final Color BLUE = new Color(54, 104, 220);
    private static final Color BACKGROUND = new Color(246, 248, 252);

    private final AuthService authService;
    private final Session session;
    private final ProfileService profileService = new ProfileService(new UserDao());
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JLabel dashboardGreeting = new JLabel();
    private final ProfilePanel profilePanel;
    private final ProjectsPanel projectsPanel;
    private final IssuesPanel issuesPanel;
    private final ContributionPanel contributionPanel;
    private JButton activeNavigationButton;

    public DashboardFrame(AuthService authService, Session session) {
        this.authService = authService;
        this.session = session;
        this.profilePanel = new ProfilePanel(profileService, session.userId(), this::refreshDashboard);
        this.projectsPanel = new ProjectsPanel(new ProjectService(new ProjectDao(), new UserDao()), session);
        this.issuesPanel = new IssuesPanel(new IssueService(new IssueDao(), new ProjectDao(), new UserDao()), session);
        this.contributionPanel = new ContributionPanel(new ContributionService(new ContributionDao()), session);

        setTitle("DevSphere - Developer Collaboration Platform");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 720);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);

        add(createNavigation(), BorderLayout.WEST);
        contentPanel.setBackground(BACKGROUND);
        contentPanel.add(createDashboardPanel(), DASHBOARD_CARD);
        contentPanel.add(profilePanel, PROFILE_CARD);
        contentPanel.add(projectsPanel, PROJECTS_CARD);
        contentPanel.add(issuesPanel, ISSUES_CARD);
        contentPanel.add(contributionPanel, CONTRIBUTION_CARD);
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createNavigation() {
        JPanel navigation = new JPanel(new GridBagLayout());
        navigation.setPreferredSize(new Dimension(220, 0));
        navigation.setBackground(NAVY);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(10, 18, 10, 18);

        JLabel logo = new JLabel("DEVSPHERE");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 22));
        constraints.gridy = 0;
        constraints.insets = new Insets(24, 18, 36, 18);
        navigation.add(logo, constraints);

        JButton dashboardButton = createNavigationButton("Dashboard");
        dashboardButton.addActionListener(event -> { activate(dashboardButton); cardLayout.show(contentPanel, DASHBOARD_CARD); });
        constraints.gridy = 1;
        constraints.insets = new Insets(5, 18, 5, 18);
        navigation.add(dashboardButton, constraints);

        JButton profileButton = createNavigationButton("My Profile");
        profileButton.addActionListener(event -> { activate(profileButton); showProfile(); });
        constraints.gridy = 2;
        navigation.add(profileButton, constraints);

        JButton projectsButton = createNavigationButton("Projects");
        projectsButton.addActionListener(event -> { activate(projectsButton); showProjects(); });
        constraints.gridy = 3;
        navigation.add(projectsButton, constraints);

        JButton issuesButton = createNavigationButton("Issues");
        issuesButton.addActionListener(event -> { activate(issuesButton); showIssues(); });
        constraints.gridy = 4;
        navigation.add(issuesButton, constraints);

        JButton contributionButton = createNavigationButton("Contribution");
        contributionButton.addActionListener(event -> { activate(contributionButton); showContribution(); });
        constraints.gridy = 5;
        navigation.add(contributionButton, constraints);

        JButton logoutButton = createNavigationButton("Logout");
        logoutButton.addActionListener(event -> logout());
        constraints.gridy = 6;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.SOUTH;
        constraints.insets = new Insets(5, 18, 24, 18);
        navigation.add(logoutButton, constraints);
        activate(dashboardButton);

        return navigation;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(48, 58, 48, 58));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.WEST;

        dashboardGreeting.setText("Welcome back, " + session.fullName());
        dashboardGreeting.setFont(new Font("SansSerif", Font.BOLD, 30));
        dashboardGreeting.setForeground(NAVY);
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 8, 0);
        panel.add(dashboardGreeting, constraints);

        JLabel subtitle = new JLabel("Your developer workspace starts here.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitle.setForeground(new Color(90, 100, 120));
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 30, 0);
        panel.add(subtitle, constraints);

        JPanel profileCard = new JPanel(new GridBagLayout());
        profileCard.setBackground(Color.WHITE);
        profileCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 229, 239)),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));
        GridBagConstraints cardConstraints = new GridBagConstraints();
        cardConstraints.gridx = 0;
        cardConstraints.anchor = GridBagConstraints.WEST;
        cardConstraints.fill = GridBagConstraints.HORIZONTAL;
        cardConstraints.weightx = 1;

        JLabel cardTitle = new JLabel("Developer profile");
        cardTitle.setFont(new Font("SansSerif", Font.BOLD, 19));
        cardTitle.setForeground(NAVY);
        cardConstraints.gridy = 0;
        profileCard.add(cardTitle, cardConstraints);

        JLabel cardText = new JLabel("Complete your profile so collaborators can understand your work.");
        cardText.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cardText.setForeground(new Color(90, 100, 120));
        cardConstraints.gridy = 1;
        cardConstraints.insets = new Insets(10, 0, 18, 0);
        profileCard.add(cardText, cardConstraints);

        JButton profileButton = new JButton("View my profile");
        profileButton.setBackground(BLUE);
        profileButton.setForeground(Color.WHITE);
        profileButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        profileButton.setFocusPainted(false);
        cardConstraints.gridy = 2;
        cardConstraints.insets = new Insets(0, 0, 0, 0);
        profileCard.add(profileButton, cardConstraints);
        profileButton.addActionListener(event -> showProfile());

        constraints.gridy = 2;
        constraints.insets = new Insets(0, 0, 0, 0);
        panel.add(profileCard, constraints);
        return panel;
    }

    private JButton createNavigationButton(String text) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setForeground(Color.WHITE);
        button.setBackground(NAVY);
        button.setFont(new Font("SansSerif", Font.PLAIN, 14));
        button.setBorder(BorderFactory.createEmptyBorder(11, 12, 11, 12));
        button.setFocusPainted(false);
        return button;
    }

    private void activate(JButton button) {
        if (activeNavigationButton != null) activeNavigationButton.setBackground(NAVY);
        activeNavigationButton = button;
        activeNavigationButton.setBackground(BLUE);
    }

    private void showProfile() {
        profilePanel.reload();
        cardLayout.show(contentPanel, PROFILE_CARD);
    }

    private void showProjects() {
        projectsPanel.refresh();
        cardLayout.show(contentPanel, PROJECTS_CARD);
    }

    private void showIssues() {
        issuesPanel.refresh();
        cardLayout.show(contentPanel, ISSUES_CARD);
    }

    private void showContribution() {
        contributionPanel.refresh();
        cardLayout.show(contentPanel, CONTRIBUTION_CARD);
    }

    private void refreshDashboard(User profile) {
        if (profile != null) {
            dashboardGreeting.setText("Welcome back, " + profile.fullName());
        }
    }

    private void logout() {
        dispose();
        new AuthFrame(authService).setVisible(true);
    }
}
