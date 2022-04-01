import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.link.IssueLinkTypeManager

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager) 
def issueManager = ComponentAccessor.getIssueManager()
def userManager = ComponentAccessor.getUserManager()

if (issue.getIssueType().getName() == 'Lead') {
    def clientKey = customFieldManager.getCustomFieldObject(11610) // 'id of field
	def clientKeyValue = issue.getCustomFieldValue(clientKey).toString()
    def clientIssue = issueManager.getIssueObject(clientKeyValue)
    def issueLinkTypes = issueLinkTypeManager.issueLinkTypes
    def linkType = issueLinkTypes.findByName('Assign a Lead')
    log.warn(issue.key + ' linked to ' + clientIssue.key + ' using "' + linkType.getName() + '"')
    ComponentAccessor.issueLinkManager.createIssueLink(issue.id, clientIssue.id, linkType.id, 1L, userManager.getUserByName('admin'))
} else {
    log.warn(issue.key + ' is "' + issue.getIssueType().getName() + '", not relating to any issue')
}
