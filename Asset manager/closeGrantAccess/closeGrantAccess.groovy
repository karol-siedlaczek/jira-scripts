import java.sql.Timestamp
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.MultivaluedMap
import javax.servlet.http.HttpServletRequest
import groovy.transform.BaseScript
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.workflow.TransitionOptions
import com.atlassian.jira.bc.project.component.MutableProjectComponent
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.user.UserManager
import com.atlassian.sal.api.UrlMode

@BaseScript CustomEndpointDelegate delegate

def projectManager = ComponentAccessor.getProjectManager()
def projectComponentManager = ComponentAccessor.getProjectComponentManager()
def remoteUserManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager)
def issueManager = ComponentAccessor.getIssueManager()
def userManager = ComponentAccessor.getUserManager()
def issueService = ComponentAccessor.getIssueService()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueFactory = ComponentAccessor.getIssueFactory()
def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def applicationProperties = ScriptRunnerImpl.getOsgiService(ApplicationProperties)
def baseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)

closeGrantAccessDialog(httpMethod: 'GET', groups: ['jira-core-users', 'jira-software-users', 'jira-servicedesk-users']) { MultivaluedMap queryParams ->
    def currIssue = issueManager.getIssueObject(queryParams.getFirst('issue') as Long)  
    def issue = issueManager.getIssueObject('ASSET-1')  // to load available options related to project
 	def dialog =
     """
      <section role="dialog" id="close-grant-access-dialog" class="aui-layer aui-dialog2 aui-dialog2-medium" aria-hidden="true" data-aui-remove-on-hide="true">
          <header class="aui-dialog2-header">
            <h2 class="aui-dialog2-header-main">Close Grant access</h2>
            <a class="aui-dialog2-header-close">
              <span class="aui-icon aui-icon-small aui-iconfont-close-dialog" id="close-button">Close</span>
            </a>
          </header>
          <div class="aui-dialog2-content">
            <form class="aui" id='close-grant-access-form'>
              
              <div class="field-group">
                <label for="create-asset-toggle">Create asset?</label>
				<aui-toggle label="toggle button" id="create-asset-toggle"></aui-toggle>
              </div>
              
              <div style="border-bottom: 1px solid #ddd; margin: 15px 0 15px 0"></div>
                
              <div class="field-group" id="summary-field-group" style="display: none">
              	<label for="summary-field">Summary<span class="aui-icon icon-required">(required)</span></label>
    			<input class="text medium-long-field" type="text" id="summary-field" name="Summary" value="${currIssue.summary}">
              </div>
              
              <div class='field-group' id="user-field-group" style="display: none">
              	<label for="user-field">User/s<span class="aui-icon icon-required"></span></label>
                <input class="text medium-long-field aui-select2" type="text" length="60" id="user-field" name="User" placeholder="Select a user/s"></input>
              </div>
              
              <div class='field-group' id="environment-field-group" style="display: none">
              	<label for="user-field">Environment/s<span class="aui-icon icon-required"></span></label>
                <input class="text medium-long-field aui-select2" type="text" length="60" id="environment-field" name="Environment" placeholder="Select a environment/s"></input>
              </div>
              
              <div class="field-group" id="description-field-group" style="display: none" style="display: none">
            	<label for="description-field">Description</label>
            	<textarea class="textarea medium-long-field" name="Description" id="description-field" placeholder="Describe the details of the above access if necessary"></textarea>
        	 </div>
   
            </form>
            </div>
              <footer class="aui-dialog2-footer">
                <div class="aui-dialog2-footer-actions">
                	<button class="aui-button aui-button-primary submit" type="submit" id="create-button">Finish</button>
                    <button type="button" accesskey="`" title="Press Alt+` to cancel" class="aui-button aui-button-link cancel" resolved="" id="cancel-button">Cancel</button>     
                </div>
                <div class="aui-dialog2-footer-hint">
              		<p id="create-asset-paragraph" style="display: none">Choose this option if you want to create an asset</p>
            		<p id="not-create-asset-paragraph" style="display: inline">Choose this option if you do not want to create an asset</p>
                </div>
              </footer>
        </section>
      """
    Response.ok().type(MediaType.TEXT_HTML).entity(dialog.toString()).header('header', 'value').build()
}

closeGrantAccess(httpMethod: 'POST', groups: ['jira-core-users', 'jira-software-users', 'jira-servicedesk-users']) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
	def transitionId = 81 // id of transition "Close"
    def createAsset = (queryParams.getFirst('createAsset') as String).toBoolean()
    def issue = issueManager.getIssueObject(queryParams.getFirst('issueKey') as String)
    def remoteUser = userManager.getUserByName(remoteUserManager.getRemoteUser(request)?.username as String)
    def message = ''
    if (createAsset) {
        def summaryParam = queryParams.getFirst('summary') as String
        def descriptionParam = queryParams.getFirst('description') as String
        def usersParam = queryParams.getFirst('user') as String
        def environmentParam = queryParams.getFirst('environment') as String
        def assetProject = projectManager.getProjectByCurrentKey('ASSET')
        for (user in usersParam.split(',')){
            def userField = customFieldManager.getCustomFieldObject(11701)
            def newIssue = issueFactory.getIssue()
            newIssue.setSummary(summaryParam)
            newIssue.setDescription(descriptionParam)
            newIssue.setProjectObject(assetProject)
            newIssue.reporter = remoteUser // user whose triggered dialog
            newIssue.issueTypeId = 11301
            newIssue.setCustomFieldValue(userField, userManager.getUserByName(user))
            for (environment in environmentParam.split(',')){
                def component = projectComponentManager.findByComponentName(assetProject.id, environment)
                if (!component){
                    component = projectComponentManager.create(environment, 'access', null, 1, assetProject.id)
                    log.warn("new component ${component['name']} added to component's list in ${assetProject.key}")
                }
                else if (!(component.getDescription()).contains('access')){ // add to desc string to declare component as access
                    def mutableComponent = MutableProjectComponent.copy(component)
                    mutableComponent.setDescription("${component.getDescription()} access")
                    component = projectComponentManager.update(mutableComponent)
                    log.warn("${component} already exists, only appended 'access' to description")
                }   
                newIssue.setComponent(newIssue.getComponents() + component)
            }
            issueManager.createIssueObject(remoteUser, newIssue)
            issueLinkManager.createIssueLink(issue.id, newIssue.id, 10003, null, remoteUser)
            message = message + "Asset <a href='${baseUrl}/browse/${newIssue.key}' target='_blank'>${newIssue.key}</a> has been created</br>"
        }	
    	//has access to 10802
    }
    else {
        message = "Issue <a href='${baseUrl}/browse/${issue.key}'>${issue.key}</a> has been closed</br>"
    }
    def transitionOptions = new TransitionOptions.Builder()
        .skipConditions()
        .skipPermissions()
        .skipValidators()
        .build()
    def transitionValidationResult = issueService.validateTransition(remoteUser, issue.id, transitionId, new IssueInputParametersImpl(), transitionOptions)
    if(transitionValidationResult.isValid()) // if this transition is valid, issue it
    	issueService.transition(remoteUser, transitionValidationResult)
    UserMessageUtil.success(message as String)
    return Response.ok([success: message]).build()
}