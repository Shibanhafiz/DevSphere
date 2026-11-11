package com.devsphere.dao;

import com.devsphere.DatabaseConnection;
import com.devsphere.model.ProjectFile;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectFileDao {
    private static final String COLUMNS = "f.id,f.project_id,f.original_name,f.stored_name,f.relative_path,f.file_size,f.file_extension,f.description,f.uploaded_by,u.username,f.uploaded_at";

    public List<ProjectFile> findForProject(long projectId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM project_files f JOIN users u ON u.id=f.uploaded_by WHERE f.project_id=? ORDER BY f.uploaded_at DESC, f.original_name";
        try (Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setLong(1, projectId); try (ResultSet r=s.executeQuery()) { List<ProjectFile> files=new ArrayList<>(); while(r.next()) files.add(map(r)); return files; }
        }
    }

    public ProjectFile find(long id, long projectId) throws SQLException {
        String sql="SELECT "+COLUMNS+" FROM project_files f JOIN users u ON u.id=f.uploaded_by WHERE f.id=? AND f.project_id=?";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,id);s.setLong(2,projectId);try(ResultSet r=s.executeQuery()){return r.next()?map(r):null;}}
    }

    public long insert(long projectId,String originalName,String storedName,String relativePath,long size,String extension,String description,long uploadedBy)throws SQLException{
        String sql="INSERT INTO project_files(project_id,original_name,stored_name,relative_path,file_size,file_extension,description,uploaded_by) VALUES(?,?,?,?,?,?,?,?)";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){s.setLong(1,projectId);s.setString(2,originalName);s.setString(3,storedName);s.setString(4,relativePath);s.setLong(5,size);s.setString(6,extension);s.setString(7,blankToNull(description));s.setLong(8,uploadedBy);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(r.next())return r.getLong(1);throw new SQLException("File ID was not returned.");}}
    }

    public void updateMetadata(long id,long projectId,String originalName,String description)throws SQLException{
        String sql="UPDATE project_files SET original_name=?,description=? WHERE id=? AND project_id=?";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setString(1,originalName);s.setString(2,blankToNull(description));s.setLong(3,id);s.setLong(4,projectId);if(s.executeUpdate()==0)throw new SQLException("File was not found.");}
    }

    public void replaceMetadata(long id,long projectId,String originalName,String storedName,String relativePath,long size,String extension)throws SQLException{
        String sql="UPDATE project_files SET original_name=?,stored_name=?,relative_path=?,file_size=?,file_extension=? WHERE id=? AND project_id=?";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setString(1,originalName);s.setString(2,storedName);s.setString(3,relativePath);s.setLong(4,size);s.setString(5,extension);s.setLong(6,id);s.setLong(7,projectId);if(s.executeUpdate()==0)throw new SQLException("File was not found.");}
    }

    public void delete(long id,long projectId)throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("DELETE FROM project_files WHERE id=? AND project_id=?")){s.setLong(1,id);s.setLong(2,projectId);if(s.executeUpdate()==0)throw new SQLException("File was not found.");}}

    private ProjectFile map(ResultSet r)throws SQLException{Timestamp t=r.getTimestamp(11);return new ProjectFile(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getString(5),r.getLong(6),r.getString(7),r.getString(8),r.getLong(9),r.getString(10),t==null?null:t.toLocalDateTime());}
    private String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
}
