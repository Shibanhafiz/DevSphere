package com.devsphere.service;

import com.devsphere.dao.IssueDao;
import com.devsphere.dao.ProjectDao;
import com.devsphere.dao.UserDao;
import com.devsphere.model.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class IssueService {
    private final IssueDao issueDao; private final ProjectDao projectDao; private final UserDao userDao; private final ContributionService contributionService;
    public IssueService(IssueDao issueDao, ProjectDao projectDao, UserDao userDao){this(issueDao,projectDao,userDao,new ContributionService(new com.devsphere.dao.ContributionDao()));}
    public IssueService(IssueDao issueDao, ProjectDao projectDao, UserDao userDao, ContributionService contributionService){this.issueDao=issueDao;this.projectDao=projectDao;this.userDao=userDao;this.contributionService=contributionService;}
    public Issue create(Session s,long projectId,IssueUpdate u)throws SQLException,ProjectAccessException{requireMember(s,projectId);IssueUpdate v=validate(u);if(v.assigneeId()!=null)requireMember(s,projectId,v.assigneeId());Issue issue=issueDao.create(s.userId(),projectId,v);contributionService.issueCreated(s.userId(),projectId,issue.id());return issue;}
    public List<Issue> list(Session s,IssueFilter f)throws SQLException{return issueDao.findForUser(s.userId(),f);}
    public List<Issue> listForProject(Session s,long projectId)throws SQLException,ProjectAccessException{requireMember(s,projectId);return issueDao.findForProject(s.userId(),projectId);}
    public Issue get(Session s,long id)throws SQLException,ProjectAccessException{Issue i=issueDao.findById(s.userId(),id);if(i==null)throw new ProjectAccessException("You are not a member of this issue's project.");return i;}
    public List<IssueComment> comments(Session s,long id)throws SQLException,ProjectAccessException{get(s,id);return issueDao.comments(s.userId(),id);}
    public List<IssueActivity> activity(Session s,long id)throws SQLException,ProjectAccessException{get(s,id);return issueDao.activity(s.userId(),id);}
    public void update(Session s,long id,IssueUpdate u)throws SQLException,ProjectAccessException{Issue old=get(s,id);if(u.status()!=old.status()&&!validTransition(old.status(),u.status()))throw new IllegalArgumentException("Invalid status transition.");if(u.assigneeId()!=null)requireMember(s,old.projectId(),u.assigneeId());issueDao.update(s.userId(),id,validate(u),old);if(u.status()!=old.status())contributionService.issueStatusChanged(s.userId(),old.projectId(),id,u.status());}
    public void comment(Session s,long id,String text)throws SQLException,ProjectAccessException{Issue issue=get(s,id);if(text==null||text.isBlank())throw new IllegalArgumentException("Comment cannot be empty.");if(text.trim().length()>4000)throw new IllegalArgumentException("Comment cannot exceed 4000 characters.");long commentId=issueDao.addComment(s.userId(),id,text.trim());contributionService.commentAdded(s.userId(),issue.projectId(),id,commentId);}
    public List<ProjectMember> members(Session s,long projectId)throws SQLException,ProjectAccessException{requireMember(s,projectId);return projectDao.findMembers(projectId,s.userId());}
    private void requireMember(Session s,long projectId)throws SQLException,ProjectAccessException{if(!projectDao.isMember(projectId,s.userId()))throw new ProjectAccessException("Only project members can access issues.");}
    private void requireMember(Session s,long projectId,long userId)throws SQLException,ProjectAccessException{requireMember(s,projectId);if(!projectDao.isMember(projectId,userId))throw new ProjectAccessException("Assignee must be a member of this project.");}
    private IssueUpdate validate(IssueUpdate u){if(u==null||u.title()==null||u.title().isBlank())throw new IllegalArgumentException("Issue title cannot be empty.");if(u.title().trim().length()>200)throw new IllegalArgumentException("Issue title cannot exceed 200 characters.");if(u.description()!=null&&u.description().length()>10000)throw new IllegalArgumentException("Issue description cannot exceed 10000 characters.");if(u.issueType()==null||u.priority()==null||u.status()==null)throw new IllegalArgumentException("Issue type, priority, and status are required.");return new IssueUpdate(u.title().trim(),u.description()==null?"":u.description().trim(),u.issueType(),u.priority(),u.status(),u.assigneeId(),u.dueDate());}
    private boolean validTransition(IssueStatus from,IssueStatus to){if(from==to)return true;if(from==IssueStatus.OPEN&&to==IssueStatus.IN_PROGRESS)return true;if(from==IssueStatus.IN_PROGRESS&&to==IssueStatus.RESOLVED)return true;if(from==IssueStatus.RESOLVED&&to==IssueStatus.CLOSED)return true;return (from==IssueStatus.RESOLVED||from==IssueStatus.CLOSED)&&to==IssueStatus.OPEN;}
}
