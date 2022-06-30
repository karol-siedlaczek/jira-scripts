import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.IssueTypeManager
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.user.UserManager
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import com.atlassian.sal.api.UrlMode
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.servlet.http.HttpServletRequest
import groovy.transform.BaseScript
import java.sql.Timestamp

@BaseScript CustomEndpointDelegate delegate

cloneToEpic(httpMethod: "GET") { MultivaluedMap queryParams, body, HttpServletRequest request ->    
  def issueManager = ComponentAccessor.getIssueManager()
  def remoteUserManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager)
  def userManager = ComponentAccessor.getUserManager()
  def customFieldManager = ComponentAccessor.getCustomFieldManager()
  def issueTypeManager = ComponentAccessor.getComponent(IssueTypeManager)
  def issueFactory = ComponentAccessor.getIssueFactory()
  def applicationProperties = ScriptRunnerImpl.getOsgiService(ApplicationProperties)
  
  def baseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)
  def issue = issueManager.getIssueObject(queryParams.getFirst('issueKey') as String)
  def remoteUsername = remoteUserManager.getRemoteUser(request)?.username as String
  def remoteUser = userManager.getUserByName(remoteUsername)
  def epicIssueType = issueTypeManager.getIssueType('10000')  // Epic issue type
  def epicLinkField = customFieldManager.getCustomFieldObject(10000) // Epic Link field
  def epicNameField = customFieldManager.getCustomFieldObject(10002) // Epic Name field
  def epicColorField = customFieldManager.getCustomFieldObject(10003) // Epic Color field
  def sprintField = customFieldManager.getCustomFieldObject(10004) // Sprint field   
  def newIssue = issueFactory.cloneIssueWithAllFields(issue)
	newIssue.setIssueTypeObject(epicIssueType)
  newIssue.setCustomFieldValue(sprintField, null) // clear Sprint
  newIssue.setCustomFieldValue(epicNameField, issue.summary)  // copy summary of issue as an epic name
  newIssue.setCustomFieldValue(epicColorField, "ghx-label-${(new Random().nextInt(11)+1)}" as String) // draw an epic color
  newIssue.setReporter(remoteUser)  // set reporter to user which execute request
  newIssue.setAssignee(null)  // clear assignee person
  newIssue.setOriginalEstimate(null)  // clear estimated time
  newIssue.setEstimate(null)  // clear remaining time
  newIssue.setUpdated(new Timestamp(new Date().getTime())) // change updated date to now
  newIssue.setCreated(new Timestamp(new Date().getTime())) // change created date to now
  issueManager.createIssueObject(remoteUser, newIssue) // finally create copied issue with modified fields
  epicLinkField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(epicLinkField), newIssue), new DefaultIssueChangeHolder()) // link old issue as epic link to epic issue
  UserMessageUtil.success("Epic ${newIssue.key} has been created")
  Response.temporaryRedirect(URI.create("${baseUrl}/browse/${newIssue.key}")).build()
}
