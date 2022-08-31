import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.workflow.TransitionOptions

def userManager = ComponentAccessor.getUserManager()

def admin = userManager.getUserByName('<ADMIN_USER>')
def parentIssue = issue.getParentObject()
def subIssues = parentIssue.getSubTaskObjects()
def assertTable = []

for (subIssue in subIssues) {
    if (subIssue.getResolution()){
        log.warn("${subIssue} of ${parentIssue} is resolved")
        assertTable.add(true)
    }
    else {
        log.warn("${subIssue} of ${parentIssue} not resolved")
        assertTable.add(false)
    }
}

if (!assertTable.contains(false)){ // all Approvements are resolved
    log.warn("${parentIssue} go to Final acceptance")
    transistIssue(admin, parentIssue, 11)
}

void transistIssue(ApplicationUser user,  Issue issue, Integer transitionId) {
    def issueService = ComponentAccessor.getIssueService()
    def transitionOptions = new TransitionOptions.Builder().skipConditions().skipPermissions().skipValidators().build()

    def transitionValidationResult = issueService.validateTransition(user, issue.id, transitionId, new IssueInputParametersImpl(), transitionOptions)
    if (transitionValidationResult.isValid())
        issueService.transition(user, transitionValidationResult)
}
