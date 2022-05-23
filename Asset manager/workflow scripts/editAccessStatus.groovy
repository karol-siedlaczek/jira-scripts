import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.workflow.TransitionOptions

def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def issueManager = ComponentAccessor.getIssueManager()
def userManager = ComponentAccessor.getUserManager()
def issueService = ComponentAccessor.getIssueService()
def currentUser = ComponentAccessor.getJiraAuthenticationContext().loggedInUser

def TRANSITION_ID = 11 // id of transition "Remove access"
def LINK_ID = 10802
def issueOutwardLinks = issueLinkManager.getOutwardLinks(issue.getId())
def linkedIssue

for (issueLink in issueOutwardLinks){
	linkedIssue = issueLink.getDestinationObject() 
    if (issueLink.getLinkTypeId() == LINK_ID){
        def transitionOptions = new TransitionOptions.Builder()
        	.skipConditions()
        	.skipPermissions()
        	.skipValidators()
        	.build()
    	def transitionValidationResult = issueService.validateTransition(currentUser, linkedIssue.id, TRANSITION_ID, new IssueInputParametersImpl(), transitionOptions)
        if(transitionValidationResult.isValid()) { // if this transition is valid, issue it
            issueService.transition(currentUser, transitionValidationResult)
            log.warn("${issue}: passed to remove ${linkedIssue} access")
        } 
        else {
            log.warn("${issue}: status of ${linkedIssue} access did not changed, probably status != Active and current status does not allow to this transition")
        }
    }
}
