package com.devsphere.ui;

import com.devsphere.model.Project;
import com.devsphere.model.ProjectUpdate;
import com.devsphere.model.Session;
import com.devsphere.service.ProjectService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProjectsPanel extends JPanel {

    private static final Color NAVY = new Color(24, 39, 75);
    private static final Color BLUE = new Color(54, 104, 220);
    private static final Color BACKGROUND = new Color(246, 248, 252);

    private final ProjectService projectService;
    private final Session session;
    private final DefaultListModel<Project> projectModel = new DefaultListModel<>();
    private final JList<Project> projectList = new JList<>(projectModel);
    private final JLabel statusLabel = new JLabel();
    private final JTextField searchField = new JTextField(16);
    private final JComboBox<Object> statusFilter = new JComboBox<>();
    private final List<Project> loadedProjects = new ArrayList<>();

    public ProjectsPanel(ProjectService projectService, Session session) {
        this.projectService = projectService;
        this.session = session;
        setLayout(new BorderLayout(0, 12));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(28, 42, 36, 42));

        add(createHeader(), BorderLayout.NORTH);
        configureList();
        add(new JScrollPane(projectList), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        refresh();
    }

    public void refresh() {
        try {
            loadedProjects.clear();
            loadedProjects.addAll(projectService.getProjectsForUser(session));
            applyFilters();
            statusLabel.setText(projectModel.isEmpty()
                    ? "No projects yet. Create one to get started."
                    : projectModel.size() + " project(s) available");
        } catch (SQLException exception) {
            statusLabel.setText("Projects could not be loaded. SQL state: "
                    + exception.getSQLState() + ", error code: " + exception.getErrorCode());
        }
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);

        JLabel title = new JLabel("Projects");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(NAVY);
        header.add(title, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(event -> refresh());
        JButton createButton = UiStyle.primaryButton("Create project");
        createButton.addActionListener(event -> createProject());
        buttons.add(refreshButton);
        buttons.add(createButton);
        header.add(buttons, BorderLayout.EAST);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(new JLabel("Find"));
        filters.add(searchField);
        statusFilter.addItem("All statuses");
        for (com.devsphere.model.ProjectStatus value : com.devsphere.model.ProjectStatus.values()) statusFilter.addItem(value);
        filters.add(statusFilter);
        JButton apply = new JButton("Apply");
        apply.addActionListener(event -> applyFilters());
        filters.add(apply);
        JButton open = new JButton("Open selected");
        open.addActionListener(event -> openSelectedProject());
        filters.add(open);
        header.add(filters, BorderLayout.SOUTH);
        return header;
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        Object selectedStatus = statusFilter.getSelectedItem();
        projectModel.clear();
        for (Project project : loadedProjects) {
            boolean matchesText = query.isBlank() || project.name().toLowerCase().contains(query)
                    || (project.description() != null && project.description().toLowerCase().contains(query));
            boolean matchesStatus = !(selectedStatus instanceof com.devsphere.model.ProjectStatus status)
                    || project.status() == status;
            if (matchesText && matchesStatus) projectModel.addElement(project);
        }
        statusLabel.setText(projectModel.isEmpty() ? "No matching projects." : projectModel.size() + " project(s) shown");
    }

    private void configureList() {
        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectList.setCellRenderer(new ProjectRenderer());
        projectList.setFixedCellHeight(92);
        projectList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                projectList.setToolTipText(projectList.getSelectedValue() == null
                        ? null : projectList.getSelectedValue().description());
            }
        });
        projectList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    openSelectedProject();
                }
            }
        });
    }

    private void createProject() {
        ProjectUpdate update = ProjectFormDialog.show(this, null);
        if (update == null) {
            return;
        }
        try {
            Project project = projectService.createProject(session, update);
            refresh();
            projectList.setSelectedValue(project, true);
            openSelectedProject();
        } catch (IllegalArgumentException exception) {
            showWarning(exception.getMessage());
        } catch (SQLException exception) {
            showDatabaseError(exception);
        }
    }

    private void openSelectedProject() {
        Project selected = projectList.getSelectedValue();
        if (selected == null) {
            showWarning("Select a project first.");
            return;
        }
        ProjectDetailsDialog dialog = new ProjectDetailsDialog(
                this, projectService, session, selected.id(), this::refresh);
        dialog.setVisible(true);
        refresh();
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Project", JOptionPane.WARNING_MESSAGE);
    }

    private void showDatabaseError(SQLException exception) {
        JOptionPane.showMessageDialog(this,
                "The project operation could not be completed.\n"
                        + "SQL state: " + exception.getSQLState()
                        + ", error code: " + exception.getErrorCode() + "\n"
                        + exception.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class ProjectRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean selected, boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, selected, focused);
            Project project = (Project) value;
            String description = project.description() == null ? "" : project.description();
            if (description.length() > 90) {
                description = description.substring(0, 87) + "...";
            }
            label.setText("<html><b>" + escape(project.name()) + "</b> &nbsp;"
                    + escape(project.status().name()) + "<br>"
                    + escape(description) + "<br>Owner: " + escape(project.ownerUsername())
                    + " &nbsp; Members: " + project.memberCount() + "</html>");
            label.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            label.setFont(new Font("SansSerif", Font.PLAIN, 13));
            return label;
        }

        private String escape(String value) {
            return value == null ? "" : value.replace("&", "&amp;")
                    .replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
