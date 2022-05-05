import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.servicedesk.api.requesttype.RequestTypeService
import com.onresolve.scriptrunner.runner.customisers.WithPlugin

def issueManager = ComponentAccessor.getIssueManager()

def issue = issueManager.getIssueObject('TEST-6254')
def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

@WithPlugin("com.atlassian.servicedesk")
def requestTypeService = ComponentAccessor.getOSGiComponentInstanceOfType(RequestTypeService)
def reqQ = requestTypeService.newQueryBuilder().issue(issue.id).build()
def reqT = requestTypeService.getRequestTypes(currentUser, reqQ)
def requestType = reqT.results[0].getName()
log.warn("REQUEST TYPE " + requestType)
