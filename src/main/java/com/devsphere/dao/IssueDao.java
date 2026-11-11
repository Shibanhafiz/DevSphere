package com.devsphere.dao;

import com.devsphere.DatabaseConnection;
import com.devsphere.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IssueDao {
    private static final String ISSUE_COLUMNS = """
            i.id, i.project_id, p.name AS project_name, i.title, i.description, i.issue_type,
            i.priority, i.status, i.reporter_id, reporter.username AS reporter_username,
            i.assignee_id, assignee.username AS assignee_username, i.due_date,
            i.created_at, i.updated_at
            """;
    private static final String FIND_BASE = """
            SELECT %s FROM issues i JOIN projects p ON p.id = i.project_id
            JOIN users reporter ON reporter.id = i.reporter_id
            LEFT JOIN users assignee ON assignee.id = i.assignee_id
            JOIN project_members viewer ON viewer.project_id = i.project_id AND viewer.user_id = ?
            """.formatted(ISSUE_COLUMNS);
    private static final String INSERT = """
            INSERT INTO issues (project_id,title,description,issue_type,priority,status,reporter_id,assignee_id,due_date)
            VALUES (?,?,?,?,?,?,?,?,?)
            """;
    private static final String UPDATE = """
            UPDATE issues SET title=?, description=?, issue_type=?, priority=?, status=?, assignee_id=?, due_date=?
            WHERE id=? AND EXISTS (SELECT 1 FROM project_members WHERE project_id=issues.project_id AND user_id=?)
            """;
    private static final String ADD_ACTIVITY = """
            INSERT INTO issue_activity (issue_id,user_id,action_type,old_value,new_value) VALUES (?,?,?,?,?)
            """;

    public Issue create(long userId, long projectId, IssueUpdate update) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                    setIssue(statement, update, projectId, userId);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Issue ID was not returned.");
                        id = keys.getLong(1);
                    }
                }
                addActivity(connection, id, userId, "ISSUE_CREATED", null, update.title());
                connection.commit();
                return findById(userId, id);
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally { connection.setAutoCommit(true); }
        }
    }

    public List<Issue> findForUser(long userId, IssueFilter filter) throws SQLException {
        StringBuilder sql = new StringBuilder(FIND_BASE).append(" WHERE 1=1 ");
        List<Object> values = new ArrayList<>(); values.add(userId);
        if (filter.search() != null && !filter.search().isBlank()) {
            sql.append(" AND (i.title LIKE ? OR CAST(i.id AS CHAR) = ?) ");
            String search = "%" + filter.search().trim() + "%";
            values.add(search); values.add(filter.search().trim());
        }
        if (filter.status() != null) { sql.append(" AND i.status = ? "); values.add(filter.status().name()); }
        if (filter.priority() != null) { sql.append(" AND i.priority = ? "); values.add(filter.priority().name()); }
        if (filter.issueType() != null) { sql.append(" AND i.issue_type = ? "); values.add(filter.issueType().name()); }
        if (filter.assigneeId() != null) { sql.append(" AND i.assignee_id = ? "); values.add(filter.assigneeId()); }
        sql.append(" ORDER BY i.updated_at DESC, i.created_at DESC");
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, values);
            try (ResultSet result = statement.executeQuery()) { List<Issue> issues = new ArrayList<>(); while (result.next()) issues.add(mapIssue(result)); return issues; }
        }
    }

    public Issue findById(long userId, long issueId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(
                FIND_BASE + " WHERE i.id = ?")) {
            statement.setLong(1, userId); statement.setLong(2, issueId);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? mapIssue(result) : null; }
        }
    }

    public List<Issue> findForProject(long userId, long projectId) throws SQLException {
        try (Connection connection=DatabaseConnection.getConnection(); PreparedStatement statement=connection.prepareStatement(FIND_BASE+" WHERE i.project_id=? ORDER BY i.updated_at DESC, i.created_at DESC")) {
            statement.setLong(1,userId); statement.setLong(2,projectId);
            try(ResultSet result=statement.executeQuery()){List<Issue> issues=new ArrayList<>();while(result.next())issues.add(mapIssue(result));return issues;}
        }
    }

    public void update(long userId, long issueId, IssueUpdate update, Issue old) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
                statement.setString(1, update.title());
                statement.setString(2, update.description().isBlank() ? null : update.description());
                statement.setString(3, update.issueType().name());
                statement.setString(4, update.priority().name());
                statement.setString(5, update.status().name());
                if (update.assigneeId() == null) statement.setNull(6, Types.BIGINT); else statement.setLong(6, update.assigneeId());
                if (update.dueDate() == null) statement.setNull(7, Types.DATE); else statement.setDate(7, Date.valueOf(update.dueDate()));
                statement.setLong(8, issueId); statement.setLong(9, userId);
                if (statement.executeUpdate() == 0) throw new SQLException("Issue is unavailable to this project member.");
                if (old.priority() != update.priority()) addActivity(connection, issueId, userId, "PRIORITY_CHANGED", old.priority().name(), update.priority().name());
                if (old.status() != update.status()) addActivity(connection, issueId, userId, "STATUS_CHANGED", old.status().name(), update.status().name());
                String oldAssignee = old.assigneeUsername() == null ? "Unassigned" : old.assigneeUsername();
                String newAssignee = update.assigneeId() == null ? "Unassigned" : String.valueOf(update.assigneeId());
                if (!java.util.Objects.equals(old.assigneeId(), update.assigneeId())) addActivity(connection, issueId, userId, "ASSIGNEE_CHANGED", oldAssignee, newAssignee);
                connection.commit();
            } catch (SQLException exception) { connection.rollback(); throw exception; }
            finally { connection.setAutoCommit(true); }
        }
    }

    public List<IssueComment> comments(long userId, long issueId) throws SQLException {
        String sql = """
                SELECT c.id,c.user_id,u.username,c.comment,c.created_at FROM issue_comments c JOIN users u ON u.id=c.user_id
                WHERE c.issue_id=? AND EXISTS (SELECT 1 FROM project_members pm JOIN issues i ON i.project_id=pm.project_id WHERE i.id=c.issue_id AND pm.user_id=?) ORDER BY c.created_at
                """;
        try (Connection connection=DatabaseConnection.getConnection(); PreparedStatement statement=connection.prepareStatement(sql)) {
            statement.setLong(1,issueId); statement.setLong(2,userId); try(ResultSet r=statement.executeQuery()){List<IssueComment> list=new ArrayList<>(); while(r.next()) list.add(new IssueComment(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),dateTime(r.getTimestamp(5)))); return list;}
        }
    }

    public List<IssueActivity> activity(long userId, long issueId) throws SQLException {
        String sql = "SELECT a.id,a.user_id,u.username,a.action_type,a.old_value,a.new_value,a.created_at FROM issue_activity a JOIN users u ON u.id=a.user_id WHERE a.issue_id=? AND EXISTS (SELECT 1 FROM project_members pm JOIN issues i ON i.project_id=pm.project_id WHERE i.id=a.issue_id AND pm.user_id=?) ORDER BY a.created_at";
        try (Connection connection=DatabaseConnection.getConnection(); PreparedStatement statement=connection.prepareStatement(sql)) {
            statement.setLong(1,issueId); statement.setLong(2,userId); try(ResultSet r=statement.executeQuery()){List<IssueActivity> list=new ArrayList<>(); while(r.next()) list.add(new IssueActivity(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),dateTime(r.getTimestamp(7)))); return list;}
        }
    }

    public long addComment(long userId, long issueId, String comment) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection()){c.setAutoCommit(false); long commentId; try(PreparedStatement s=c.prepareStatement("INSERT INTO issue_comments(issue_id,user_id,comment) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){s.setLong(1,issueId);s.setLong(2,userId);s.setString(3,comment);s.executeUpdate();try(ResultSet keys=s.getGeneratedKeys()){if(!keys.next())throw new SQLException("Comment ID was not returned.");commentId=keys.getLong(1);}addActivity(c,issueId,userId,"COMMENT_ADDED",null,comment);c.commit();return commentId;}catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}
    }

    private void setIssue(PreparedStatement s, IssueUpdate u, long projectId, long reporterId) throws SQLException { s.setLong(1,projectId);s.setString(2,u.title());s.setString(3,u.description().isBlank()?null:u.description());s.setString(4,u.issueType().name());s.setString(5,u.priority().name());s.setString(6,u.status().name());s.setLong(7,reporterId);if(u.assigneeId()==null)s.setNull(8,Types.BIGINT);else s.setLong(8,u.assigneeId());if(u.dueDate()==null)s.setNull(9,Types.DATE);else s.setDate(9,Date.valueOf(u.dueDate())); }
    private void addActivity(Connection c,long issueId,long userId,String action,String oldValue,String newValue)throws SQLException{try(PreparedStatement s=c.prepareStatement(ADD_ACTIVITY)){s.setLong(1,issueId);s.setLong(2,userId);s.setString(3,action);s.setString(4,oldValue);s.setString(5,newValue);s.executeUpdate();}}
    private void bind(PreparedStatement s,List<Object> values)throws SQLException{for(int i=0;i<values.size();i++){Object v=values.get(i);if(v instanceof Long)s.setLong(i+1,(Long)v);else s.setString(i+1,String.valueOf(v));}}
    private Issue mapIssue(ResultSet r)throws SQLException{return new Issue(r.getLong("id"),r.getLong("project_id"),r.getString("project_name"),r.getString("title"),r.getString("description"),IssueType.valueOf(r.getString("issue_type")),IssuePriority.valueOf(r.getString("priority")),IssueStatus.valueOf(r.getString("status")),r.getLong("reporter_id"),r.getString("reporter_username"),(Long)r.getObject("assignee_id"),r.getString("assignee_username"),r.getDate("due_date")==null?null:r.getDate("due_date").toLocalDate(),dateTime(r.getTimestamp("created_at")),dateTime(r.getTimestamp("updated_at")));}
    private LocalDateTime dateTime(Timestamp t){return t==null?null:t.toLocalDateTime();}
}
