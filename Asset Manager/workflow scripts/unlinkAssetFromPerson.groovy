import com.atlassian.jira.component.ComponentAccessor

def PERSON_ISSUETYPE_ID = ''
def issueLinkManager = ComponentAccessor.getIssueLinkManager()

def currentUser = ComponentAccessor.getJiraAuthenticationContext().loggedInUser
def issueInwardLinks = issueLinkManager.getInwardLinks(issue.id)
def linkedIssue

for (issueLink in issueInwardLinks){
	linkedIssue = issueLink.getSourceObject() 
    if (linkedIssue.getIssueTypeId() == PERSON_ISSUETYPE_ID) { // id of issuetype 'Person'
    	issueLinkManager.removeIssueLink(issueLink, currentUser)
    	log.warn("Person ${linkedIssue.key} unlinked from ${issue.key}")
	}
}
