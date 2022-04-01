import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.issue.IssueManager;

def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def commentManager = ComponentAccessor.getCommentManager()
def userManager = ComponentAccessor.getUserManager()

def currIssue = issueManager.getIssueObject(issue.key)
def excedeedAmount = customFieldManager.getCustomFieldObject(11605) // Excedeed Amount field ID
def excedeedAmountValue = currIssue.getCustomFieldValue(excedeedAmount)

def comment = "Issue has been automatically rejected, because your budget for this year's training has been exceeded\nExceeded amount is $excedeedAmountValue PLN\n"
commentManager.create(issue, userManager.getUserByName('admin'), comment, false)
log.warn('Issue has been rejected, details in comment section')
