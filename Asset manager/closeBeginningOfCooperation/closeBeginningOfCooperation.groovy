import java.util.LinkedList
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.MultivaluedMap
import javax.servlet.http.HttpServletRequest
import groovy.transform.BaseScript
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.bc.project.component.MutableProjectComponent
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.IssueTypeManager
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.project.Project
import com.atlassian.jira.workflow.TransitionOptions
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.user.UserManager
import com.atlassian.sal.api.UrlMode

@BaseScript CustomEndpointDelegate delegate

closeBeginningOfCooperationDialog(httpMethod: 'GET', groups: ['jira-core-users', 'jira-software-users', 'jira-servicedesk-users']) { MultivaluedMap queryParams ->
    def dialog =
            """
      <section role="dialog" id="close-beginning-of-cooperation-dialog" class="aui-layer aui-dialog2 aui-dialog2-medium" aria-hidden="true" data-aui-remove-on-hide="true">
          <header class="aui-dialog2-header">
            <h2 class="aui-dialog2-header-main">Close Beginning of cooperation</h2>
            <a class="aui-dialog2-header-close">
              <span class="aui-icon aui-icon-small aui-iconfont-close-dialog" id="close-button">Close</span>
            </a>
          </header>
          <div class="aui-dialog2-content">
            <form class="aui" id='close-beginning-of-cooperation-form'>
              
              <div class='field-group' id="user-field-group">
              	<label for="user-field">User<span class="aui-icon icon-required"></span></label>
                <input class="text medium-long-field aui-select2" type="text" length="60" id="user-field" name="User" placeholder="Select a user"></input>
              </div>
              
              <div class='field-group' id="access-field-group">
              	<label for="access-field">Access/s</label>
                <input class="text medium-long-field aui-select2" type="text" length="60" id="access-field" name="Access" placeholder="Select a access/s"></input>
              </div>
              
              <div class='field-group' id="device-field-group">
              	<label for="device-field">Device/s</label>
                <input class="text long-field aui-select2" type="text" length="60" id="device-field" name="Device" placeholder="Select a device/s from stock"></input>
              </div>
   			  
              <div class="field-group" id="description-field-group">
            	<label for="description-field">Description</label>
            	<textarea class="textarea long-field" name="Description" id="description-field" placeholder="Description of Person asset"></textarea>
        	 </div>
             
            </form>
            </div>
              <footer class="aui-dialog2-footer">
                <div class="aui-dialog2-footer-actions">
                	<aui-spinner id="custom-dialog-spinner" size="small" style="display: none"></aui-spinner>
                	<input class="aui-button aui-button-primary submit" type="submit" value="Finish" id="create-button">
                    <button type="button" accesskey="`" title="Press Alt+` to cancel" class="aui-button aui-button-link cancel" resolved="" id="cancel-button">Cancel</button>     
                </div>
                <div class="aui-dialog2-footer-hint">
              		<p>Test</p>
                </div>
              </footer>
        </section>
      """
    Response.ok().type(MediaType.TEXT_HTML).entity(dialog.toString()).header('header', 'value').build()
}


closeBeginningOfCooperation(httpMethod: 'POST', groups: ['jira-core-users', 'jira-software-users', 'jira-servicedesk-users']) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
    def projectManager = ComponentAccessor.getProjectManager()
    def issueManager = ComponentAccessor.getIssueManager()
    def issueLinkManager = ComponentAccessor.getIssueLinkManager()
    def userManager = ComponentAccessor.getUserManager()
    def customFieldManager = ComponentAccessor.getCustomFieldManager()
    def remoteUserManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager)
    def applicationProperties = ScriptRunnerImpl.getOsgiService(ApplicationProperties)

    def CLOSE_BEGINNING_OF_COOPERATION_TRANSITION_ID = 221
    def USER_FIELD = customFieldManager.getCustomFieldObject(11701)
    def ASSET_PROJECT = projectManager.getProjectByCurrentKey('ASSET')

    def issue = issueManager.getIssueObject(queryParams.getFirst('issueKey') as String)
    def user = userManager.getUserByName(queryParams.getFirst('user') as String)
    def remoteUser = userManager.getUserByName(remoteUserManager.getRemoteUser(request)?.username as String)
    def baseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)
    def message = ''

    Issue personIssue = createPersonAsset(user, remoteUser, issue, ASSET_PROJECT, USER_FIELD, issueManager, customFieldManager, issueLinkManager)
    message = message + "Person <a href='${baseUrl}/browse/${personIssue.key}' target='_blank'>${personIssue.key}</a> has been created</br>"
    if (!(queryParams.getFirst('device') as String).isEmpty()) {
        //LinkedList<String> devices = new LinkedList<String>(Arrays.asList(queryParams.getFirst('device')))
        //for (device in devices){
        for (device in (queryParams.getFirst('device') as String).split(',')){
            def deviceIssue = issueManager.getIssueByCurrentKey(device)
            assignPersonAssetToDeviceAsset(user, remoteUser, deviceIssue, USER_FIELD)
            message = message + "Device <a href='${baseUrl}/browse/${deviceIssue.key}' target='_blank'>${deviceIssue.key}</a> has been linked</br>"
        }
    }
    if (!(queryParams.getFirst('access') as String).isEmpty()) {
        //LinkedList<String> accesses = new LinkedList<String>(Arrays.asList(queryParams.getFirst('device')))
        //for (access in accesses){
        for (access in (queryParams.getFirst('access') as String).split(',')){
            Issue accessIssue = createAccessAsset(user, remoteUser, personIssue, access, ASSET_PROJECT, USER_FIELD, issueManager, customFieldManager, issueLinkManager)
            message = message + "Access <a href='${baseUrl}/browse/${accessIssue.key}' target='_blank'>${accessIssue.key}</a> has been created</br>"
        }
    }
    transistIssue(remoteUser, issue, CLOSE_BEGINNING_OF_COOPERATION_TRANSITION_ID) //close curr issue
    UserMessageUtil.success(message as String)
    return Response.ok([success: message]).build()
}

Issue createPersonAsset(ApplicationUser user, // creates issue with issuetype Person, also importes selected field from Beginning of cooperation
                        ApplicationUser remoteUser,
                        MutableIssue issue,
                        Project assetProject,
                        CustomField userField,
                        IssueManager issueManager,
                        CustomFieldManager customFieldManager,
                        IssueLinkManager issueLinkManager){
    def issueFactory = ComponentAccessor.getIssueFactory()
    def issueTypeManager = ComponentAccessor.getComponent(IssueTypeManager)

    def PERSON_ISSUE_TYPE = issueTypeManager.getIssueType('11200')
    def POSITION_FIELD = customFieldManager.getCustomFieldObject(10501)
    def DEPARTMENT_FIELD = customFieldManager.getCustomFieldObject(10502)
    def TEAM_FIELD = customFieldManager.getCustomFieldObject(10503)
    def ROOM_FIELD = customFieldManager.getCustomFieldObject(10504)
    def FORM_OF_COOPERATION_FIELD = customFieldManager.getCustomFieldObject(10607)
    def SHARING_IMAGE_ON_SOCIAL_MEDIA_FIELD = customFieldManager.getCustomFieldObject(11706)
    def SHARING_IMAGE_IN_INTRANET_FIELD = customFieldManager.getCustomFieldObject(11707)
    def SHARING_PRIVATE_PHONE_NUMBER_FIELD = customFieldManager.getCustomFieldObject(11708)
    def RELATES_RELATION_ID = 10003 as Long
    //def PERSON_ACTIVE_TRANSITION_ID = 11

    def newIssue = issueFactory.getIssue()
    newIssue.setSummary(user.displayName)
    newIssue.setProjectObject(assetProject)
    newIssue.setIssueType(PERSON_ISSUE_TYPE)
    newIssue.setReporter(remoteUser)
    newIssue.setCustomFieldValue(userField, user)
    newIssue.setCustomFieldValue(POSITION_FIELD, issue.getCustomFieldValue(POSITION_FIELD))
    newIssue.setCustomFieldValue(DEPARTMENT_FIELD, issue.getCustomFieldValue(DEPARTMENT_FIELD))
    newIssue.setCustomFieldValue(TEAM_FIELD, issue.getCustomFieldValue(TEAM_FIELD))
    newIssue.setCustomFieldValue(ROOM_FIELD, issue.getCustomFieldValue(ROOM_FIELD))
    newIssue.setCustomFieldValue(FORM_OF_COOPERATION_FIELD, issue.getCustomFieldValue(FORM_OF_COOPERATION_FIELD))
    newIssue.setCustomFieldValue(SHARING_IMAGE_ON_SOCIAL_MEDIA_FIELD, issue.getCustomFieldValue(SHARING_IMAGE_ON_SOCIAL_MEDIA_FIELD))
    newIssue.setCustomFieldValue(SHARING_IMAGE_IN_INTRANET_FIELD, issue.getCustomFieldValue(SHARING_IMAGE_IN_INTRANET_FIELD))
    newIssue.setCustomFieldValue(SHARING_PRIVATE_PHONE_NUMBER_FIELD, issue.getCustomFieldValue(SHARING_PRIVATE_PHONE_NUMBER_FIELD))
    Issue personIssue = issueManager.createIssueObject(remoteUser, newIssue)
    issueLinkManager.createIssueLink(issue.id, personIssue.id, RELATES_RELATION_ID, null, remoteUser)
    //def transitionValidationResult = issueService.validateTransition(remoteUser, personIssue.id, PERSON_ACTIVE_TRANSITION_ID, new IssueInputParametersImpl(), transitionOptions)
    //if (transitionValidationResult.isValid())
    //	issueService.transition(remoteUser, transitionValidationResult)
    return personIssue
}

void assignPersonAssetToDeviceAsset(ApplicationUser user,
                                    ApplicationUser remoteUser,
                                    MutableIssue deviceIssue,
                                    CustomField userField){
    def DEVICE_IN_USED_TRANSITION_ID = 81
    //def DEVICE_RELATION_ID = 10800 as Long

    userField.updateValue(null, deviceIssue, new ModifiedValue(deviceIssue.getCustomFieldValue(userField), user), new DefaultIssueChangeHolder())
    //issueLinkManager.createIssueLink(personIssue.id, deviceIssue.id, DEVICE_RELATION_ID, null, remoteUser)
    transistIssue(remoteUser, deviceIssue, DEVICE_IN_USED_TRANSITION_ID) // other operations will be executed in post workflow functions
}

Issue createAccessAsset(ApplicationUser user,
                        ApplicationUser remoteUser,
                        Issue personIssue,
                        String access,
                        Project assetProject,
                        CustomField userField,
                        IssueManager issueManager) {
    def issueTypeManager = ComponentAccessor.getComponent(IssueTypeManager)
    def issueFactory = ComponentAccessor.getIssueFactory()
    def projectComponentManager = ComponentAccessor.getProjectComponentManager()

    def ACCESS_ISSUE_TYPE = issueTypeManager.getIssueType('11301')
    //def ACCESS_RELATION_ID = 10802 as Long
    //def ACCESS_ACTIVE_TRANSITION_ID = 41

    def newIssue = issueFactory.getIssue()
    def component = projectComponentManager.findByComponentName(assetProject.id, access)
    newIssue.setSummary(access)
    newIssue.setProjectObject(assetProject)
    newIssue.reporter = remoteUser // user whose triggered dialog
    newIssue.issueTypeId = ACCESS_ISSUE_TYPE.id
    newIssue.setCustomFieldValue(userField, user)
    if (!component){ // if totally new component
        component = projectComponentManager.create(access, 'access', null, 1, assetProject.id)
        log.warn("new component ${component['name']} added to component's list in ${assetProject.key}")
    }
    else if (!(component.getDescription()).contains('access')){ // if component exists but not yet labeled to usage as 'access'
        def mutableComponent = MutableProjectComponent.copy(component)
        mutableComponent.setDescription("${component.getDescription()} access")
        component = projectComponentManager.update(mutableComponent)
        log.warn("${component} already exists, only appended 'access' to description")
    }
    newIssue.setComponent(newIssue.getComponents() + component)
    Issue accessIssue = issueManager.createIssueObject(remoteUser, newIssue)
    //issueLinkManager.createIssueLink(personIssue.id, accessIssue.id, ACCESS_RELATION_ID, null, remoteUser)
    //def transitionValidationResult = issueService.validateTransition(remoteUser, accessIssue.id, ACCESS_ACTIVE_TRANSITION_ID, new IssueInputParametersImpl(), transitionOptions)
    //if (transitionValidationResult.isValid())
    //	issueService.transition(remoteUser, transitionValidationResult)
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
