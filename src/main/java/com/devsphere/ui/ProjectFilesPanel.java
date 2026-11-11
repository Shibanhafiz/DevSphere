package com.devsphere.ui;

import com.devsphere.model.ProjectFile;
import com.devsphere.model.Session;
import com.devsphere.service.ProjectFileService;
import com.devsphere.service.ProjectAccessException;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ProjectFilesPanel extends JPanel {
    private final ProjectFileService service;
    private final Session session;
    private final long projectId;
    private final FileTableModel model = new FileTableModel();
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel(" ");

    public ProjectFilesPanel(ProjectFileService service, Session session, long projectId) {
        this.service=service;this.session=session;this.projectId=projectId;
        setLayout(new BorderLayout(0,8));setOpaque(false);
        table.setRowHeight(28);table.setAutoCreateRowSorter(true);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table),BorderLayout.CENTER);add(buttons(),BorderLayout.NORTH);add(status,BorderLayout.SOUTH);refresh();
    }
    public void refresh(){try{model.set(service.list(session,projectId));status.setText(model.rows.size()+" file(s)");}catch(Exception e){status.setText("Files could not be loaded: "+safe(e));}}
    private JPanel buttons(){JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));p.setOpaque(false);JButton upload=new JButton("Upload");upload.addActionListener(e->upload());JButton download=new JButton("Download");download.addActionListener(e->download());JButton edit=new JButton("Rename / description");edit.addActionListener(e->edit());JButton replace=new JButton("Replace");replace.addActionListener(e->replace());JButton delete=new JButton("Delete");delete.addActionListener(e->delete());p.add(upload);p.add(download);p.add(edit);p.add(replace);p.add(delete);return p;}
    private ProjectFile selected(){int row=table.getSelectedRow();if(row<0)return null;return model.rows.get(table.convertRowIndexToModel(row));}
    private void upload(){JFileChooser chooser=new JFileChooser();if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;String description=JOptionPane.showInputDialog(this,"Optional file description:","");run("Uploading file...",()->service.upload(session,projectId,chooser.getSelectedFile().toPath(),description));}
    private void download(){ProjectFile f=selected();if(f==null){warn("Select a file first.");return;}JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File(f.originalName()));if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;run("Downloading file...",()->service.download(session,f,chooser.getSelectedFile().toPath()));}
    private void edit(){ProjectFile f=selected();if(f==null){warn("Select a file first.");return;}JTextField name=new JTextField(f.originalName(),28);JTextField desc=new JTextField(f.description()==null?"":f.description(),28);JPanel p=new JPanel(new GridLayout(0,1,4,4));p.add(new JLabel("Filename"));p.add(name);p.add(new JLabel("Description"));p.add(desc);if(JOptionPane.showConfirmDialog(this,p,"Edit file metadata",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION)run("Saving file details...",()->{service.renameOrDescribe(session,f,name.getText(),desc.getText());return null;});}
    private void replace(){ProjectFile f=selected();if(f==null){warn("Select a file first.");return;}JFileChooser chooser=new JFileChooser();if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)run("Replacing file...",()->service.replace(session,f,chooser.getSelectedFile().toPath()));}
    private void delete(){ProjectFile f=selected();if(f==null){warn("Select a file first.");return;}if(JOptionPane.showConfirmDialog(this,"Delete '"+f.originalName()+"'? This cannot be undone.","Confirm file deletion",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)==JOptionPane.YES_OPTION)run("Deleting file...",()->{service.delete(session,f);return null;});}
    private <T> void run(String message,Task<T> task){status.setText(message);SwingWorker<T,Void> worker=new SwingWorker<>(){protected T doInBackground()throws Exception{return task.run();}protected void done(){try{get();status.setText("Operation completed.");refresh();}catch(Exception e){status.setText("Operation failed: "+safe(e));}}};worker.execute();}
    private void warn(String message){JOptionPane.showMessageDialog(this,message,"Project files",JOptionPane.WARNING_MESSAGE);}
    private String safe(Exception e){Throwable t=e;while(t.getCause()!=null)t=t.getCause();return t.getMessage()==null?"Please check the file and database configuration.":t.getMessage();}
    @FunctionalInterface private interface Task<T>{T run()throws Exception;}
    private static class FileTableModel extends AbstractTableModel{private final String[] columns={"Filename","Type","Size","Uploader","Uploaded"};private List<ProjectFile> rows=new ArrayList<>();void set(List<ProjectFile> v){rows=new ArrayList<>(v);fireTableDataChanged();}public int getRowCount(){return rows.size();}public int getColumnCount(){return columns.length;}public String getColumnName(int c){return columns[c];}public Object getValueAt(int r,int c){ProjectFile f=rows.get(r);return switch(c){case 0->f.originalName();case 1->f.extension().isBlank()?"File":f.extension().toUpperCase();case 2->format(f.fileSize());case 3->f.uploaderUsername();default->f.uploadedAt();};}private static String format(long n){if(n<1024)return n+" B";if(n<1024*1024)return new DecimalFormat("0.0").format(n/1024.0)+" KB";return new DecimalFormat("0.0").format(n/1024.0/1024.0)+" MB";}}
}
