import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.jira.project.Project
import com.atlassian.sal.api.user.UserManager
import groovy.transform.BaseScript
import com.atlassian.sal.api.UrlMode
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.MediaType
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import groovy.json.JsonBuilder
import java.sql.Timestamp

@BaseScript CustomEndpointDelegate delegate

def projectManager = ComponentAccessor.getProjectManager()
def issueManager = ComponentAccessor.getIssueManager()
def remoteUserManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager)
def userManager = ComponentAccessor.getUserManager()
def issueFactory = ComponentAccessor.getIssueFactory()
def projectComponentManager = ComponentAccessor.getProjectComponentManager()
def applicationProperties = ScriptRunnerImpl.getOsgiService(ApplicationProperties)
def baseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)

cloneWithComponentDialog(httpMethod: "GET") { MultivaluedMap queryParams, HttpServletRequest request ->
    def project = projectManager.getProjectObj(queryParams.getFirst('projectId') as Long)
    if (project == null)
    	return Response.ok([error: 'project id not provided']).build()
    def components = project.getComponents()['name']
    def options = ''
    for (component in components)
        options = options + "<option value=${component}>${component}</option>"
 	def dialog =
        """
      	<section role="dialog" id="clone-issues-with-component-dialog" class="aui-layer aui-dialog2 aui-dialog2-medium" aria-hidden="true" data-aui-remove-on-hide="true">
        	<header class="aui-dialog2-header">
            	<h2 class="aui-dialog2-header-main">Select component/s</h2>
            	<a class="aui-dialog2-header-close">
              		<span class="aui-icon aui-icon-small aui-iconfont-close-dialog" id="close-button">Close</span>
            	</a>
          	</header>
          	<div class="aui-dialog2-content">
            	<form class="aui">
              		<div class="field-group">
                		<label for="components-select2">Component/s<span class="aui-icon icon-required">required</span> </label>
                		<select id="components-select2" multiple="">
                  			${options}
                		</select>
              		</div>
             	</form>
            </div>
            <footer class="aui-dialog2-footer">
            	<div class="aui-dialog2-footer-actions">
                	<div class="buttons-container">
                  		<div class="buttons">
                      		<input class="aui-button aui-button-primary submit" type="submit" value="Create" id="create-button">
                      		<button type="button" accesskey="`" title="Press Alt+` to cancel" class="aui-button aui-button-link cancel" resolved="" id="cancel-button">Cancel</button>
                    	</div>
                	</div>      
                </div>
        	</footer>
        </section>
        """
    log.warn("${remoteUserManager.getRemoteUser(request)?.username as String} triggered dialog")
    Response.ok().type(MediaType.TEXT_HTML).entity(dialog.toString()).build()
}

cloneWithComponent(httpMethod: "POST") { MultivaluedMap queryParams, body, HttpServletRequest request ->
    def issue = issueManager.getIssueObject(queryParams.getFirst('issueKey') as String)
    def remoteUserName = remoteUserManager.getRemoteUser(request)?.username as String
    def remoteUser = userManager.getUserByName(remoteUserName)
    def components = queryParams.getFirst('components') as String
    def project = issue.getProjectObject()
    def message = ''
    def newIssue
    def component
    for (componentName in components.split(',')) {
        newIssue = issueFactory.cloneIssue(issue)
        component = projectComponentManager.findByComponentName(project.getId(), componentName)
        newIssue.setComponent([component])
        newIssue.setSummary("${componentName}: ${issue.summary}")
        newIssue.setReporter(remoteUser)  // set reporter to user which execute request
        newIssue.setAssignee(userManager.getUserByKey(component.getLead()))
        newIssue.setUpdated(new Timestamp(new Date().getTime())) // change updated date to now
    	newIssue.setCreated(new Timestamp(new Date().getTime())) // change created date to now
        newIssue.setOriginalEstimate(null)  // clear estimated time
  		newIssue.setEstimate(null)  // clear remaining time
        newIssue.setTimeSpent(null) // clear worklogs
        issueManager.createIssueObject(remoteUser, newIssue)
        message = message + "Issue <a href='${baseUrl}/browse/${newIssue.key}' target='_blank'>${newIssue.key}</a> has been created</br>"
        log.warn("issue created ${newIssue.key}")
    }
    UserMessageUtil.success(message)
    return Response.ok([success: message]).build()
}
