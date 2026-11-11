package com.devsphere.ui;

import com.devsphere.model.*;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public final class IssueFormDialog {
    private IssueFormDialog() { }
    public static IssueUpdate show(Component parent, Issue existing, java.util.List<ProjectMember> members) {
        JTextField title=new JTextField(existing==null?"":existing.title(),28); JTextArea description=new JTextArea(existing==null?"":value(existing.description()),5,28);description.setLineWrap(true);description.setWrapStyleWord(true);
        JComboBox<IssueType> type=new JComboBox<>(IssueType.values()); JComboBox<IssuePriority> priority=new JComboBox<>(IssuePriority.values()); JComboBox<IssueStatus> status=new JComboBox<>(IssueStatus.values()); JComboBox<MemberChoice> assignee=new JComboBox<>(); assignee.addItem(new MemberChoice(null,"Unassigned"));
        for(ProjectMember m:members)assignee.addItem(new MemberChoice(m.userId(),m.username())); JTextField due=new JTextField(existing==null?"":(existing.dueDate()==null?"":existing.dueDate().toString()),12);
        if(existing!=null){type.setSelectedItem(existing.issueType());priority.setSelectedItem(existing.priority());status.setSelectedItem(existing.status());for(int i=0;i<assignee.getItemCount();i++)if(java.util.Objects.equals(assignee.getItemAt(i).id,existing.assigneeId()))assignee.setSelectedIndex(i);}
        JPanel p=new JPanel(new GridBagLayout());GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.fill=GridBagConstraints.HORIZONTAL;c.weightx=1;c.insets=new Insets(4,4,4,4);int row=0;
        add(p,c,row++,"Title *",title);add(p,c,row++,"Description",new JScrollPane(description));add(p,c,row++,"Type",type);add(p,c,row++,"Priority",priority);add(p,c,row++,"Status",status);add(p,c,row++,"Assignee",assignee);add(p,c,row++,"Due date (YYYY-MM-DD)",due);
        int result=JOptionPane.showConfirmDialog(parent,p,existing==null?"Create issue":"Edit issue",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);if(result!=JOptionPane.OK_OPTION)return null;
        LocalDate date=null;if(!due.getText().isBlank())try{date=LocalDate.parse(due.getText().trim());}catch(Exception e){throw new IllegalArgumentException("Due date must use YYYY-MM-DD.");}
        return new IssueUpdate(title.getText(),description.getText(),(IssueType)type.getSelectedItem(),(IssuePriority)priority.getSelectedItem(),(IssueStatus)status.getSelectedItem(),((MemberChoice)assignee.getSelectedItem()).id,date);
    }
    private static void add(JPanel p,GridBagConstraints c,int row,String label,Component field){c.gridy=row;p.add(new JLabel(label),c);c.gridy=++row;p.add(field,c);}
    private static String value(String v){return v==null?"":v;}
    public static final class MemberChoice{final Long id;final String name;MemberChoice(Long id,String name){this.id=id;this.name=name;}public String toString(){return name;}}
}
