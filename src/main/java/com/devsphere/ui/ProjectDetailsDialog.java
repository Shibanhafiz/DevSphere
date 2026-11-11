package com.devsphere.ui;

import com.devsphere.model.Project;
import com.devsphere.model.ProjectMember;
import com.devsphere.model.ProjectUpdate;
import com.devsphere.model.Session;
import com.devsphere.service.ProjectAccessException;
import com.devsphere.service.ProjectService;
import com.devsphere.service.ProjectFileService;
import com.devsphere.dao.ProjectFileDao;
import com.devsphere.dao.IssueDao;
import com.devsphere.dao.ProjectDao;
import com.devsphere.dao.UserDao;
import com.devsphere.service.IssueService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;

public class ProjectDetailsDialog extends JDialog {

    private static final Color NAVY = new Color(24, 39, 75);
    private static final Color BACKGROUND = new Color(246, 248, 252);

    private final ProjectService projectService;
    private final Session session;
    private final long projectId;
    private final Runnable onChanged;
    private final JLabel titleLabel = new JLabel();
    private final JLabel metadataLabel = new JLabel();
    private final JTextArea descriptionArea = new JTextArea();
    private final DefaultListModel<ProjectMember> memberModel = new DefaultListModel<>();
    private final JList<ProjectMember> memberList = new JList<>(memberModel);
    private final JPanel ownerActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final ProjectFileService fileService;
    private final ProjectFilesPanel filesPanel;
    private final ProjectIssuesPanel issuesPanel;
    private Project loadedProject;

    public ProjectDetailsDialog(Component parent, ProjectService projectService,
                                Session session, long projectId, Runnable onChanged) {
        super(javax.swing.SwingUtilities.getWindowAncestor(parent), "Project details", ModalityType.APPLICATION_MODAL);
        this.projectService = projectService;
        this.session = session;
        this.projectId = projectId;
        this.onChanged = onChanged;
        this.fileService = new ProjectFileService(new ProjectFileDao(), projectService);
        this.filesPanel = new ProjectFilesPanel(fileService, session, projectId);
        this.issuesPanel = new ProjectIssuesPanel(new IssueService(new IssueDao(), new ProjectDao(), new UserDao()), session, projectId);

        setSize(720, 620);
        setMinimumSize(new Dimension(620, 520));
        setLocationRelativeTo(parent);
        add(createContent());
        reload();
    }

    private JPanel createContent() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        content.add(createProjectInformation(), BorderLayout.NORTH);

        memberList.setCellRenderer(new MemberRenderer());
        memberList.setFixedCellHeight(48);
        JPanel membersPanel = new JPanel(new BorderLayout(0, 8));
        membersPanel.setOpaque(false);
        JLabel membersTitle = new JLabel("Team members");
        membersTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        membersTitle.setForeground(NAVY);
        membersPanel.add(membersTitle, BorderLayout.NORTH);
        membersPanel.add(new JScrollPane(memberList), BorderLayout.CENTER);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overview", createProjectInformation());
        tabs.addTab("Members", membersPanel);
        tabs.addTab("Issues", issuesPanel);
        tabs.addTab("Files", filesPanel);
        content.remove(0);
        content.add(tabs, BorderLayout.CENTER);

        ownerActions.setOpaque(false);
        content.add(ownerActions, BorderLayout.SOUTH);
        return content;
    }

    private JPanel createProjectInformation() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setForeground(NAVY);
        panel.add(titleLabel, BorderLayout.NORTH);
        metadataLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        metadataLabel.setForeground(new Color(90, 100, 120));
        panel.add(metadataLabel, BorderLayout.CENTER);
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setRows(4);
        descriptionArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        descriptionArea.setOpaque(false);
        panel.add(descriptionArea, BorderLayout.SOUTH);
        return panel;
    }

    private void reload() {
        try {
            loadedProject = projectService.getProjectForUser(session, projectId);
            titleLabel.setText(loadedProject.name());
            metadataLabel.setText("Owner: " + loadedProject.ownerUsername()
                    + "    Status: " + loadedProject.status()
                    + "    Members: " + loadedProject.memberCount()
                    + "    Created: " + loadedProject.createdAt()
                    + "    Category: " + value(loadedProject.category()));
            descriptionArea.setText(loadedProject.description() == null
                    ? "No description provided." : loadedProject.description());

            memberModel.clear();
            for (ProjectMember member : projectService.getMembersForUser(session, projectId)) {
                memberModel.addElement(member);
            }
            configureOwnerActions();
        } catch (SQLException | ProjectAccessException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "Project Error", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void configureOwnerActions() {
        ownerActions.removeAll();
        if (loadedProject == null || loadedProject.ownerId() != session.userId()) {
            ownerActions.revalidate();
            ownerActions.repaint();
            return;
        }

        JButton editButton = new JButton("Edit project");
        editButton.addActionListener(event -> editProject());
        ownerActions.add(editButton);

        JButton deleteButton = new JButton("Delete project");
        deleteButton.addActionListener(event -> deleteProject());
        ownerActions.add(deleteButton);

        JTextField memberLookup = new JTextField(18);
        JButton addButton = new JButton("Add member");
        addButton.addActionListener(event -> addMember(memberLookup));
        ownerActions.add(new JLabel("Username/email:"));
        ownerActions.add(memberLookup);
        ownerActions.add(addButton);

        JButton removeButton = new JButton("Remove selected");
        removeButton.addActionListener(event -> removeSelectedMember());
        ownerActions.add(removeButton);
        ownerActions.revalidate();
        ownerActions.repaint();
    }

    private void editProject() {
        ProjectUpdate update = ProjectFormDialog.show(this, loadedProject);
        if (update == null) {
            return;
        }
        try {
            projectService.updateProject(session, projectId, update);
            onChanged.run();
            reload();
        } catch (IllegalArgumentException | ProjectAccessException exception) {
            showWarning(exception.getMessage());
        } catch (SQLException exception) {
            showDatabaseError(exception);
        }
    }

    private void addMember(JTextField memberLookup) {
        try {
            projectService.addMember(session, projectId, memberLookup.getText());
            memberLookup.setText("");
            onChanged.run();
            reload();
        } catch (IllegalArgumentException | ProjectAccessException exception) {
            showWarning(exception.getMessage());
        } catch (SQLException exception) {
            showDatabaseError(exception);
        }
    }

    private void removeSelectedMember() {
        ProjectMember selected = memberList.getSelectedValue();
        if (selected == null) {
            showWarning("Select a team member first.");
            return;
        }
        try {
            projectService.removeMember(session, projectId, selected.userId());
            onChanged.run();
            reload();
        } catch (IllegalArgumentException | ProjectAccessException exception) {
            showWarning(exception.getMessage());
        } catch (SQLException exception) {
            showDatabaseError(exception);
        }
    }

    private void deleteProject() {
        if (loadedProject == null) return;
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete project '" + loadedProject.name() + "'?\nMembers, issues, comments, and project files will be removed.\nRecorded contribution history will be preserved without project links.",
                "Confirm project deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            projectService.deleteProject(session, projectId);
            try { fileService.deleteProjectFiles(projectId); }
            catch (java.io.IOException cleanup) { showWarning("Project data was deleted, but some physical files could not be removed: " + cleanup.getMessage()); }
            onChanged.run();
            dispose();
        } catch (IllegalArgumentException | ProjectAccessException exception) { showWarning(exception.getMessage()); }
        catch (SQLException exception) { showDatabaseError(exception); }
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

    private String value(String text) { return text == null || text.isBlank() ? "Not specified" : text; }

    private static class MemberRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean selected, boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, selected, focused);
            ProjectMember member = (ProjectMember) value;
            label.setText(member.fullName() + "  (" + member.username() + ")  -  " + member.role());
            label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            return label;
        }
    }
}
