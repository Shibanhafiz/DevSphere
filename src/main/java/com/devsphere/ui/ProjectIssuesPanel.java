package com.devsphere.ui;

import com.devsphere.model.Issue;
import com.devsphere.model.Session;
import com.devsphere.service.IssueService;

import javax.swing.*;
import java.awt.*;

public class ProjectIssuesPanel extends JPanel {
    private final IssueService service; private final Session session; private final long projectId;
    private final DefaultListModel<Issue> model=new DefaultListModel<>(); private final JList<Issue> list=new JList<>(model); private final JLabel status=new JLabel(" ");
    public ProjectIssuesPanel(IssueService service,Session session,long projectId){this.service=service;this.session=session;this.projectId=projectId;setLayout(new BorderLayout(0,8));setOpaque(false);list.setCellRenderer(new Renderer());list.setFixedCellHeight(54);list.addMouseListener(new java.awt.event.MouseAdapter(){public void mouseClicked(java.awt.event.MouseEvent e){if(e.getClickCount()==2)open();}});add(new JScrollPane(list),BorderLayout.CENTER);add(status,BorderLayout.SOUTH);refresh();}
    private void refresh(){try{model.clear();for(Issue i:service.listForProject(session,projectId))model.addElement(i);status.setText(model.size()+" issue(s); double-click an issue to open details");}catch(Exception e){status.setText("Issues unavailable: "+e.getMessage());}}
    private void open(){Issue i=list.getSelectedValue();if(i!=null)new IssueDetailsDialog(this,service,session,i.id(),this::refresh).setVisible(true);}
    private static class Renderer extends DefaultListCellRenderer{public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){JLabel x=(JLabel)super.getListCellRendererComponent(l,v,i,s,f);Issue q=(Issue)v;x.setText("#"+q.id()+"  "+q.title()+"  |  "+q.issueType()+"  |  "+q.priority()+"  |  "+q.status());x.setBorder(BorderFactory.createEmptyBorder(8,10,8,10));return x;}}
}
