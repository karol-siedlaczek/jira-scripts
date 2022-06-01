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
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.user.UserManager
import com.atlassian.sal.api.UrlMode

@BaseScript CustomEndpointDelegate delegate

def projectManager = ComponentAccessor.getProjectManager()
def remoteUserManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager)
def issueManager = ComponentAccessor.getIssueManager()
def userManager = ComponentAccessor.getUserManager()
def issueService = ComponentAccessor.getIssueService()
def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def applicationProperties = ScriptRunnerImpl.getOsgiService(ApplicationProperties)
def baseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)

closeRemoveAccessDialog(httpMethod: 'GET', groups: ['jira-core-users', 'jira-software-users', 'jira-servicedesk-users']) { MultivaluedMap queryParams ->
    def dialog =
            """
      <section role="dialog" id="close-remove-access-dialog" class="aui-layer aui-dialog2 aui-dialog2-medium" aria-hidden="true" data-aui-remove-on-hide="true">
          <header class="aui-dialog2-header">
            <h2 class="aui-dialog2-header-main">Close Remove access</h2>
            <a class="aui-dialog2-header-close">
              <span class="aui-icon aui-icon-small aui-iconfont-close-dialog" id="close-button">Close</span>
            </a>
          </header>
          <div class="aui-dialog2-content">
            <form class="aui" id='close-remove-access-form'>

              <div class='field-group' id="user-field-group">
              	<label for="user-field">User/s</label>
                <input class="text  aui-select2" type="text" length="60" id="user-field" name="User" placeholder="Select a user/s"></input>
                <input type="button" class="aui-button" id="user-field-search" value="Search"/>
              </div>

              <div class='field-group' id="issue-field-group">
              	<label for="issue-field">Access/s</label>
                <input class="text medium-long-field aui-select2" type="text" length="60" id="issue-field" name="Access" placeholder="Select a access/s"></input>
              </div>

              <p>Search for a user if you want to remove any access from his list, if not just quit</p>
            </form>
            </div>
              <footer class="aui-dialog2-footer">
                <div class="aui-dialog2-footer-actions">
                	<aui-spinner id="custom-dialog-spinner" size="small" style="display: none"></aui-spinner>
                	<input class="aui-button aui-button-primary submit" type="submit" value="Finish" id="create-button">
                    <button type="button" accesskey="`" title="Press Alt+` to cancel" class="aui-button aui-button-link cancel" resolved="" id="cancel-button">Cancel</button>
                </div>
                <div class="aui-dialog2-footer-hint">
              		<p>Search to update access list based on provided user/s</p>
                </div>
              </footer>
        </section>
      """
    Response.ok().type(MediaType.TEXT_HTML).entity(dialog.toString()).header('header', 'value').build()
}

closeRemoveAccess(httpMethod: 'POST', groups: ['jira-core-users', 'jira-software-users', 'jira-servicedesk-users']) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
    def CLOSE_TRANSITION_ID = 81 // id of transition "Close"

    def issueParam = queryParams.getFirst('issueKey') as String
    def onlyCloseParam = queryParams.getFirst('onlyClose') as Boolean
    def accessIssuesParam = queryParams.getFirst('accessIssue') as String
    def remoteUser = userManager.getUserByName(remoteUserManager.getRemoteUser(request)?.username as String)
    def issue = issueManager.getIssueObject(issueParam)
    def transitionOptions = new TransitionOptions.Builder()
            .skipConditions()
            .skipPermissions()
            .skipValidators()
            .build()
    def message = ''
    if (!onlyCloseParam) {
        log.warn(onlyCloseParam)
        def REMOVE_ACCESS_TRANSITION_ID = 11
        def CLOSE_ACCESS_TRANSITION_ID = 21
        def assetProject = projectManager.getProjectByCurrentKey('ASSET')
        def accessIssue
        for (accessIssueParam in accessIssuesParam.split(',')){
            accessIssue = issueManager.getIssueObject(accessIssueParam)
            def removeAccessTransitionValidationResult = issueService.validateTransition(remoteUser, accessIssue.id, REMOVE_ACCESS_TRANSITION_ID, new IssueInputParametersImpl(), transitionOptions)
            if(removeAccessTransitionValidationResult.isValid()) // if this transition is valid, issue it
                issueService.transition(remoteUser, removeAccessTransitionValidationResult)
            def closeAccessTransitionValidationResult = issueService.validateTransition(remoteUser, accessIssue.id, CLOSE_ACCESS_TRANSITION_ID, new IssueInputParametersImpl(), transitionOptions)
            if(closeAccessTransitionValidationResult.isValid()) // if this transition is valid, issue it
                issueService.transition(remoteUser, closeAccessTransitionValidationResult)
            issueLinkManager.createIssueLink(issue.id, accessIssue.id, 10003, null, remoteUser)
            message = message + "Access <a href='${baseUrl}/browse/${accessIssue.key}' target='_blank'>${accessIssue.key}</a> has been removed</br>"
        }
    }
    else {
        def closeTransitionValidationResult = issueService.validateTransition(remoteUser, issue.id, CLOSE_TRANSITION_ID, new IssueInputParametersImpl(), transitionOptions)
        if(closeTransitionValidationResult.isValid()) // if this transition is valid, issue it
            issueService.transition(remoteUser, closeTransitionValidationResult)
        message = "Issue <a href='${baseUrl}/browse/${issue.key}'>${issue.key}</a> has been closed</br>"
    }
    UserMessageUtil.success(message as String)
    return Response.ok([success: message]).build()
}