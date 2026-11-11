package com.devsphere.dao;

import com.devsphere.DatabaseConnection;
import com.devsphere.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

public class ContributionDao {
    private static final Map<String,Integer> POINTS=Map.of("PROJECT_CREATED",15,"ISSUE_CREATED",5,"ISSUE_RESOLVED",15,"ISSUE_CLOSED",5,"COMMENT_ADDED",2,"PROJECT_COMPLETED",20,"ISSUE_STATUS_CHANGED",2);
    public boolean record(long userId,Long projectId,Long issueId,String type,String eventKey)throws SQLException{
        String sql="INSERT IGNORE INTO contribution_activities(user_id,project_id,issue_id,activity_type,points,event_key) VALUES(?,?,?,?,?,?)";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,userId);if(projectId==null)s.setNull(2,Types.BIGINT);else s.setLong(2,projectId);if(issueId==null)s.setNull(3,Types.BIGINT);else s.setLong(3,issueId);s.setString(4,type);s.setInt(5,POINTS.getOrDefault(type,0));s.setString(6,eventKey);return s.executeUpdate()>0;}
    }
    public ContributionSummary summary(long userId)throws SQLException{String sql="SELECT activity_type,SUM(points) points,COUNT(*) count FROM contribution_activities WHERE user_id=? GROUP BY activity_type";Map<String,Integer> map=new LinkedHashMap<>();int score=0,count=0;try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,userId);try(ResultSet r=s.executeQuery()){while(r.next()){int p=r.getInt("points"),n=r.getInt("count");map.put(r.getString("activity_type"),p);score+=p;count+=n;}}}return new ContributionSummary(score,count,map);}
    public List<ContributionActivity> recent(long userId,int limit)throws SQLException{String sql="SELECT id,user_id,project_id,issue_id,activity_type,points,created_at FROM contribution_activities WHERE user_id=? ORDER BY created_at DESC LIMIT ?";try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,userId);s.setInt(2,limit);try(ResultSet r=s.executeQuery()){List<ContributionActivity> list=new ArrayList<>();while(r.next())list.add(map(r));return list;}}}
    public Map<LocalDateTime,Integer> daily(long userId,LocalDateTime from,LocalDateTime to)throws SQLException{String sql="SELECT created_at,COUNT(*) count FROM contribution_activities WHERE user_id=? AND created_at>=? AND created_at<? GROUP BY DATE(created_at) ORDER BY created_at";Map<LocalDateTime,Integer> result=new LinkedHashMap<>();try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,userId);s.setTimestamp(2,Timestamp.valueOf(from));s.setTimestamp(3,Timestamp.valueOf(to));try(ResultSet r=s.executeQuery()){while(r.next())result.put(r.getTimestamp("created_at").toLocalDateTime(),r.getInt("count"));}}return result;}
    public List<ContributionDay> heatmapDays(long userId,LocalDate from,LocalDate to)throws SQLException{
        String sql="SELECT DATE(created_at) activity_date,COUNT(*) activity_count,COALESCE(SUM(points),0) total_points,GROUP_CONCAT(DISTINCT activity_type ORDER BY activity_type SEPARATOR ', ') activity_types FROM contribution_activities WHERE user_id=? AND created_at>=? AND created_at<? GROUP BY DATE(created_at) ORDER BY activity_date";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){
            s.setLong(1,userId);s.setTimestamp(2,Timestamp.valueOf(from.atStartOfDay()));s.setTimestamp(3,Timestamp.valueOf(to.atStartOfDay()));
            try(ResultSet r=s.executeQuery()){
                List<ContributionDay> result=new ArrayList<>();
                while(r.next()){
                    String types=r.getString("activity_types");
                    result.add(new ContributionDay(r.getDate("activity_date").toLocalDate(),r.getInt("activity_count"),r.getInt("total_points"),types==null||types.isBlank()?List.of():List.of(types.split(", "))));
                }
                return result;
            }
        }
    }
    public List<LeaderboardRow> leaderboard(Long projectId)throws SQLException{String sql="SELECT u.full_name,u.username,COALESCE(SUM(c.points),0) score,COUNT(c.id) activity_count FROM users u LEFT JOIN contribution_activities c ON c.user_id=u.id "+(projectId==null?"":"AND c.project_id=? ")+" GROUP BY u.id,u.full_name,u.username ORDER BY score DESC,activity_count DESC,u.username";try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){if(projectId!=null)s.setLong(1,projectId);try(ResultSet r=s.executeQuery()){List<LeaderboardRow> list=new ArrayList<>();int rank=0;while(r.next())list.add(new LeaderboardRow(++rank,r.getString(1),r.getString(2),r.getInt(3),r.getInt(4)));return list;}}}
    public ProjectAnalytics analytics(long projectId)throws SQLException{String sql="SELECT COUNT(*),COUNT(DISTINCT user_id),SUM(activity_type='ISSUE_CREATED'),SUM(activity_type='ISSUE_RESOLVED') FROM contribution_activities WHERE project_id=?";try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,projectId);try(ResultSet r=s.executeQuery()){r.next();String member="";try(PreparedStatement m=c.prepareStatement("SELECT u.username FROM contribution_activities a JOIN users u ON u.id=a.user_id WHERE a.project_id=? GROUP BY a.user_id,u.username ORDER BY COUNT(*) DESC LIMIT 1")){m.setLong(1,projectId);try(ResultSet mr=m.executeQuery()){if(mr.next())member=mr.getString(1);}}return new ProjectAnalytics(projectId,r.getInt(1),r.getInt(2),r.getInt(3),r.getInt(4),member);}}}
    private ContributionActivity map(ResultSet r)throws SQLException{return new ContributionActivity(r.getLong(1),r.getLong(2),(Long)r.getObject(3),(Long)r.getObject(4),r.getString(5),r.getInt(6),r.getTimestamp(7).toLocalDateTime());}
}
