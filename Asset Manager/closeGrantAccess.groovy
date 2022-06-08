import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.MultivaluedMap
import javax.servlet.http.HttpServletRequest
import groovy.transform.BaseScript
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.config.IssueTypeManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.workflow.TransitionOptions
import com.atlassian.jira.bc.project.component.MutableProjectComponent
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.user.UserManager
import com.atlassian.sal.api.UrlMode

@BaseScript CustomEndpointDelegate delegate

closeGrantAccessDialog(httpMethod: 'GET', groups: ['jira-core-users', 'jira-software-users', 'jira-servicedesk-users']) { MultivaluedMap queryParams ->
    def issueManager = ComponentAccessor.getIssueManager()

    def issue = issueManager.getIssueObject(queryParams.getFirst('issue') as Long)
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
          <input class="text medium-long-field" type="text" id="summary-field" name="Summary" value="${issue.summary}" required>
        </div>
        <div class='field-group' id="user-field-group" style="display: none">
          <label for="user-field">User/s<span class="aui-icon icon-required"></span></label>
          <input class="text medium-long-field aui-select2" type="text" length="60" id="user-field" name="User" placeholder="Select a user/s" required></input>
        </div>
        <div class='field-group' id="environment-field-group" style="display: none">
          <label for="user-field">Environment/s<span class="aui-icon icon-required"></span></label>
          <input class="text medium-long-field aui-select2" type="text" length="60" id="environment-field" name="Environment" placeholder="Select a environment/s" required></input>
        </div>
        <div class="field-group" id="description-field-group" style="display: none">
          <label for="description-field">Description</label>
          <textarea class="textarea medium-long-field" name="Description" id="description-field" placeholder="Describe the details of the above access if necessary"></textarea>
        </div>
      </form>
    </div>
    <footer class="aui-dialog2-footer">
      <div class="aui-dialog2-footer-actions">
        <aui-spinner id="custom-dialog-spinner" size="small" style="display: none"></aui-spinner>
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
    def issueManager = ComponentAccessor.getIssueManager()
    def issueService = ComponentAccessor.getIssueService()
    def userManager = ComponentAccessor.getUserManager()
    def remoteUserManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager)
    def applicationProperties = ScriptRunnerImpl.getOsgiService(ApplicationProperties)

    def CLOSE_GRANT_ACCESS_TRANSITION_ID = 81 // id of transition "Close"

    def assetCreation = (queryParams.getFirst('createAsset') as String).toBoolean()
    def issue = issueManager.getIssueObject(queryParams.getFirst('issueKey') as String)
    def remoteUser = userManager.getUserByName(remoteUserManager.getRemoteUser(request)?.username as String)
    def baseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)
    def message = ''

    if (assetCreation) {
        def users = (queryParams.getFirst('user') as String).split(',')
        def accesses = (queryParams.getFirst('environment') as String).split(',')
        for (user in users) {
            for (access in accesses) {
                Issue accessIssue = createAccessAsset(userManager.getUserByName(user), remoteUser, issue, access, queryParams, issueManager)
                message = message + "Asset <a href='${baseUrl}/browse/${accessIssue.key}' target='_blank'>${accessIssue.key}</a> has been created</br>"
            }
        }
    }
    message = message + "Issue <a href='${baseUrl}/browse/${issue.key}'>${issue.key}</a> has been closed</br>"
    transistIssue(remoteUser, issue, CLOSE_GRANT_ACCESS_TRANSITION_ID)
    UserMessageUtil.success(message as String)
    return Response.ok([success: message]).build()
}

Issue createAccessAsset(ApplicationUser user,
                        ApplicationUser remoteUser,
                        Issue issue,
                        String access,
                        MultivaluedMap queryParams,
                        IssueManager issueManager){
    def customFieldManager = ComponentAccessor.getCustomFieldManager()
    def projectManager = ComponentAccessor.getProjectManager()
    def projectComponentManager = ComponentAccessor.getProjectComponentManager()
    def issueFactory = ComponentAccessor.getIssueFactory()
    def issueTypeManager = ComponentAccessor.getComponent(IssueTypeManager)
    def issueLinkManager = ComponentAccessor.getIssueLinkManager()

    def USER_FIELD = customFieldManager.getCustomFieldObject(11701)
    def ACCESS_ISSUE_TYPE = issueTypeManager.getIssueType('11301')
    def RELATES_RELATION_ID = 10003 as Long
    def ASSET_PROJECT = projectManager.getProjectByCurrentKey('ASSET')

    def newIssue = issueFactory.getIssue()
    def component = projectComponentManager.findByComponentName(ASSET_PROJECT.id, access)
    newIssue.setSummary(queryParams.getFirst('summary') as String)
    newIssue.setProjectObject(ASSET_PROJECT)
    newIssue.setReporter(remoteUser) // user whose triggered dialog
    newIssue.setIssueType(ACCESS_ISSUE_TYPE)
    newIssue.setDescription(queryParams.getFirst('description') as String)
    newIssue.setCustomFieldValue(USER_FIELD, user)
    if (!component) {
        component = projectComponentManager.create(access, 'access', null, 1, ASSET_PROJECT.id)
        log.warn("new component ${component['name']} added to component's list in ${ASSET_PROJECT.key}")
    }
    else if (!(component.getDescription()).contains('access')){ // add to desc string to declare component as access
        def mutableComponent = MutableProjectComponent.copy(component)
        mutableComponent.setDescription("${component.getDescription()} access")
        component = projectComponentManager.update(mutableComponent)
        log.warn("${component} already exists, only appended 'access' to description")
    }
    newIssue.setComponent(newIssue.getComponents() + component) // adds components to existing components instead replacing
    Issue accessIssue = issueManager.createIssueObject(remoteUser, newIssue)
    issueLinkManager.createIssueLink(accessIssue.id, issue.id, RELATES_RELATION_ID, null, remoteUser)
    return accessIssue
}

void transistIssue(ApplicationUser user,
                   Issue issue,
                   Integer transitionId) {
    def issueService = ComponentAccessor.getIssueService()
    def transitionOptions = new TransitionOptions.Builder().skipConditions().skipPermissions().skipValidators().build()

    def transitionValidationResult = issueService.validateTransition(user, issue.id, transitionId, new IssueInputParametersImpl(), transitionOptions)
    if (transitionValidationResult.isValid())
        issueService.transition(user, transitionValidationResult)
}