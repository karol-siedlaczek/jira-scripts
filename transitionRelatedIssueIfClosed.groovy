import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.issue.IssueInputParametersImpl

def userManager = ComponentAccessor.getUserManager()
def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def issueManager = ComponentAccessor.getIssueManager()
def issueService = ComponentAccessor.getIssueService()
def commentManager = ComponentAccessor.getCommentManager()
def eventTypeManager = ComponentAccessor.getEventTypeManager()

def issue = event.getIssue()
def eventName = eventTypeManager.getEventType(event.eventTypeId).getName()
def eventLabel = (eventName.split(' ')[1]).toLowerCase()
def transitionId = 61 // id of transition
def issueLinks = issueLinkManager.getOutwardLinks(issue.getId())
def comment
log.warn("sourceObject: ${issue.key}; eventName: ${eventName}")

for (issueLink in issueLinks){
    def linkedIssueProjectType = issueLink.getDestinationObject().getProjectObject().getProjectTypeKey()['key']
    def linkedIssue = issueLink.getDestinationObject()
    if (linkedIssueProjectType == 'service_desk'){
        try { // if related issue is closed
            if (!linkedIssue.getResolution())
                throw new NullPointerException("${linkedIssue.key} not resolved")
           	else
                log.warn("${linkedIssue.key} is already resolved, script aborted")
    	} catch(NullPointerException) { // if related issue is not closed
        	def adminUser = userManager.getUserByName('admin')
            if (linkedIssue.getStatus()['name'] == 'On 1st LS'){
                log.warn("Linked ${issue.key} has been ${eventLabel}, status did not change, already has '${linkedIssue.getStatus()['name']}'")
                comment = "Linked ${issue.key} has been ${eventLabel}"
            }
            else{
                def transitionValidationResult = issueService.validateTransition(adminUser, linkedIssue.id, transitionId, new IssueInputParametersImpl())
                if(transitionValidationResult.isValid()){ // if this transition is valid, issue it
                    issueService.transition(adminUser, transitionValidationResult)
                    log.warn("Linked ${issue.key} has been ${eventLabel}, ${linkedIssue.key} transitioned on 1LS")
                    comment = "Linked ${issue.key} has been ${eventLabel}, current issue has been transitioned on 1LS"
                }
                else{ // if won't find selected transition
                   log.warn("linked ${issue.key} has been ${eventLabel}, but ${linkedIssue.key} not transitioned on 1LS!")
                   comment = "Linked ${issue.key} has been ${eventLabel}, this issue should also be transitioned on 1LS, notify your JIRA administrator"
                }
                final SD_PUBLIC_COMMENT = "sd.public.comment"
                def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true])]
            }
        	commentManager.create(linkedIssue, adminUser, comment, null, null, new Date(), properties, true) //issue, user which commented, comment body, restricted group, restricted role, notifications
        }
    }
    else {
        log.warn("linked ${linkedIssue.key} is not from service desk project, script aborted")
    }  
}
