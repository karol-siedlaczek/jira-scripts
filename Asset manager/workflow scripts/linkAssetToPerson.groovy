import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.web.bean.PagerFilter;
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.jira.config.IssueTypeManager
import com.atlassian.sal.api.UrlMode
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl

def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def userManager = ComponentAccessor.getUserManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueFactory = ComponentAccessor.getIssueFactory()
def issueManager = ComponentAccessor.getIssueManager()
def projectManager = ComponentAccessor.getProjectManager()
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchService = ComponentAccessor.getComponent(SearchService)
def issueTypeManager = ComponentAccessor.getComponent(IssueTypeManager)
def applicationProperties = ScriptRunnerImpl.getOsgiService(ApplicationProperties)
def baseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)

def ACCESS_LINK_ID = 123
def USED_LINK_ID = 123

def linkId
def userField = customFieldManager.getCustomFieldObject(123)
def userFieldValue = issue.getCustomFieldValue(userField)
def userFieldName = userFieldValue['username']
def currentUser = ComponentAccessor.getJiraAuthenticationContext().loggedInUser
def query = jqlQueryParser.parseQuery("project = $issue.projectObject.key AND issuetype = 'Person' AND 'User' = $userFieldName")
log.warn(query)
def results = searchService.search(userManager.getUserByName('admin'), query, PagerFilter.getUnlimitedFilter())

if(issue.issueType.name == 'Access')
    	linkId =  ACCESS_LINK_ID// relation 'Access'
    else
        linkId =  USED_LINK_ID//relation 'Use'

if (results.getTotal() > 0){
	results.getResults().each { personIssue ->
        log.warn("${personIssue.key} linked to ${issue.key}")
    	issueLinkManager.createIssueLink(personIssue.id, issue.id, linkId, null, currentUser)
	}
}
else { // issue does not exists - create
    def newIssue = issueFactory.getIssue() // creating Person Issue
    newIssue.setSummary(userFieldValue['displayName'] as String)
    newIssue.setProjectObject(projectManager.getProjectByCurrentKey(issue.projectObject.key))
    newIssue.setIssueType(issueTypeManager.getIssueType('11200'))
    newIssue.setCustomFieldValue(userField, userFieldValue)
    newIssue.setReporter(currentUser)
    issueManager.createIssueObject(currentUser, newIssue)
	log.warn("created ${newIssue.key} and linked to ${issue.key}")
    issueLinkManager.createIssueLink(newIssue.id, issue.id, linkId, null, currentUser)
    UserMessageUtil.warning("<a href='${baseUrl}/browse/${newIssue.key}' target='_blank'>${newIssue.key}</a> has been created, because not found any related Person to selected user")
}
