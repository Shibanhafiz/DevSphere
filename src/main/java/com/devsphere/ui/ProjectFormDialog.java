package com.devsphere.ui;

import com.devsphere.model.Project;
import com.devsphere.model.ProjectStatus;
import com.devsphere.model.ProjectUpdate;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class ProjectFormDialog {

    private ProjectFormDialog() {
    }

    public static ProjectUpdate show(java.awt.Component parent, Project existingProject) {
        JTextField name = new JTextField(existingProject == null ? "" : existingProject.name(), 28);
        JTextArea description = new JTextArea(existingProject == null ? "" : value(existingProject.description()), 6, 28);
        JTextField category = new JTextField(existingProject == null ? "" : value(existingProject.category()), 28);
        JTextField technologies = new JTextField(existingProject == null ? "" : value(existingProject.technologies()), 28);
        JTextField githubUrl = new JTextField(existingProject == null ? "" : value(existingProject.githubUrl()), 28);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JComboBox<ProjectStatus> status = new JComboBox<>(ProjectStatus.values());
        status.setSelectedItem(existingProject == null ? ProjectStatus.PLANNING : existingProject.status());

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(5, 5, 5, 5);

        constraints.gridy = 0;
        panel.add(new JLabel("Project name *"), constraints);
        constraints.gridy = 1;
        panel.add(name, constraints);
        constraints.gridy = 2;
        panel.add(new JLabel("Description"), constraints);
        constraints.gridy = 3;
        panel.add(new JScrollPane(description), constraints);
        constraints.gridy = 4;
        panel.add(new JLabel("Category"), constraints);
        constraints.gridy = 5;
        panel.add(category, constraints);
        constraints.gridy = 6;
        panel.add(new JLabel("Technologies"), constraints);
        constraints.gridy = 7;
        panel.add(technologies, constraints);
        constraints.gridy = 8;
        panel.add(new JLabel("GitHub repository URL"), constraints);
        constraints.gridy = 9;
        panel.add(githubUrl, constraints);
        constraints.gridy = 10;
        panel.add(new JLabel("Status"), constraints);
        constraints.gridy = 11;
        panel.add(status, constraints);

        String title = existingProject == null ? "Create project" : "Edit project";
        int result = JOptionPane.showConfirmDialog(parent, panel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        return new ProjectUpdate(name.getText(), description.getText(), category.getText(),
                technologies.getText(), githubUrl.getText(), (ProjectStatus) status.getSelectedItem());
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
