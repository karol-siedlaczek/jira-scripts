// adds default security level to new task on CreateIssue event
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.security.IssueSecurityLevel
import com.atlassian.jira.issue.security.IssueSecurityLevelManager

def issueManager = ComponentAccessor.getIssueManager()

def issue = event.getIssue()
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

issue.setSecurityLevelId(10301)
issueManager.updateIssue(user,issue,EventDispatchOption.DO_NOT_DISPATCH,true)
