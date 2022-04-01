import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager;

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()

def currIssue = issueManager.getIssueObject(issue.key)
def excedeedAmount = customFieldManager.getCustomFieldObject(11605) // Excedeed Amount field ID
def excedeedAmountValue = currIssue.getCustomFieldValue(excedeedAmount) as float

excedeedAmountValue > 0
