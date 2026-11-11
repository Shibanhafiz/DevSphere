package com.devsphere.service;

import com.devsphere.dao.ContributionDao;
import com.devsphere.model.*;
import java.sql.SQLException;
import java.time.*;
import java.util.List;

public class ContributionService {
    private final ContributionDao dao;
    public ContributionService(ContributionDao dao){this.dao=dao;}
    public void projectCreated(long userId,long projectId)throws SQLException{dao.record(userId,projectId,null,"PROJECT_CREATED","project-created:"+projectId);}
    public void projectCompleted(long userId,long projectId)throws SQLException{dao.record(userId,projectId,null,"PROJECT_COMPLETED","project-completed:"+projectId);}
    public void issueCreated(long userId,long projectId,long issueId)throws SQLException{dao.record(userId,projectId,issueId,"ISSUE_CREATED","issue-created:"+issueId);}
    public void issueStatusChanged(long userId,long projectId,long issueId,IssueStatus status)throws SQLException{dao.record(userId,projectId,issueId,"ISSUE_STATUS_CHANGED","issue-status:"+issueId+":"+status);if(status==IssueStatus.RESOLVED)dao.record(userId,projectId,issueId,"ISSUE_RESOLVED","issue-resolved:"+issueId);if(status==IssueStatus.CLOSED)dao.record(userId,projectId,issueId,"ISSUE_CLOSED","issue-closed:"+issueId);}
    public void commentAdded(long userId,long projectId,long issueId,long commentId)throws SQLException{dao.record(userId,projectId,issueId,"COMMENT_ADDED","comment:"+commentId);}
    public ContributionSummary summary(long userId)throws SQLException{return dao.summary(userId);}
    public List<ContributionActivity> recent(long userId)throws SQLException{return dao.recent(userId,20);}
    public java.util.Map<java.time.LocalDateTime,Integer> daily(long userId,YearMonth month)throws SQLException{return dao.daily(userId,month.atDay(1).atStartOfDay(),month.plusMonths(1).atDay(1).atStartOfDay());}
    public List<ContributionDay> heatmap(long userId,java.time.LocalDate from,java.time.LocalDate to)throws SQLException{return dao.heatmapDays(userId,from,to);}
    public List<LeaderboardRow> leaderboard(Long projectId)throws SQLException{return dao.leaderboard(projectId);}
    public ProjectAnalytics analytics(long projectId)throws SQLException{return dao.analytics(projectId);}
}
