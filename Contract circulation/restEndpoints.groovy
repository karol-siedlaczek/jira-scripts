import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.ws.rs.core.MediaType
import javax.servlet.http.HttpServletRequest
import javax.xml.bind.ValidationException
import groovy.transform.BaseScript
import groovy.transform.Field
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpPatch
import org.apache.http.entity.StringEntity
import org.apache.http.HttpResponse
import org.apache.http.HttpEntity
import org.apache.commons.httpclient.methods.*
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.UsernamePasswordCredentials
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.attachment.Attachment
import com.atlassian.jira.issue.attachment.FileSystemAttachmentDirectoryAccessor
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.workflow.TransitionOptions
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.plugin.ProjectPermissionKey
import com.atlassian.sal.api.user.UserManager
import com.atlassian.sal.api.UrlMode
import com.atlassian.sal.api.ApplicationProperties
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl

@BaseScript CustomEndpointDelegate delegate

@Field CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
@Field String BASE_MICROSOFT_URL = 'https://graph.microsoft.com/v1.0'
@Field String BASE_FOLDER_ID = '<ONEDRIVE_FOLDER_ID>'
@Field String LOCAL_API_USER = '<USERNAME>'
@Field String LOCAL_API_PASSWORD = '<PASS>'
@Field Integer COMMENTED_BY_CONTRACTOR_TRANSITION_ID = 1
@Field Long NEW_STATUS_ID = 1
@Field Long WORKING_ON_DOCUMENT_STATUS_ID = 
@Field CustomField CONTRACT_ATTACHMENTS_ID_FIELD = customFieldManager.getCustomFieldObject(1)
@Field CustomField ACTIVE_CONTRACT_FIELD = customFieldManager.getCustomFieldObject(1)
@Field CustomField ACTIVE_CONTRACT_ID_FIELD = customFieldManager.getCustomFieldObject(1)
@Field CustomField PERSONS_INVOLVED_FIELD = customFieldManager.getCustomFieldObject(1)
@Field CustomField SUBSTANTIVE_PERSONS_FIELD = customFieldManager.getCustomFieldObject(1)
@Field CustomField DEPARTMENTS_INVOLVED_FIELD = customFieldManager.getCustomFieldObject(1)
@Field CustomField CONTRACT_ITERATION_FIELD = customFieldManager.getCustomFieldObject(1)
@Field CustomField WORKING_FOLDER_FIELD = customFieldManager.getCustomFieldObject(1)

uploadAttachmentFileToOneDriveDialog(httpMethod: 'GET', groups: []) { MultivaluedMap queryParams, HttpServletRequest request -> 
    def remoteUserManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager)
    def dialog =
    """
        <section role="dialog" id="upload-attachment-file-to-onedrive-dialog" class="aui-layer aui-dialog2 aui-dialog2-medium" aria-hidden="true" data-aui-remove-on-hide="true">
            <header class="aui-dialog2-header">
            	<h2 class="aui-dialog2-header-main">Select file/s to attach</h2>
            	<a class="aui-dialog2-header-close">
              		<span class="aui-icon aui-icon-small aui-iconfont-close-dialog" id="close-button">Close</span>
            	</a>
          	</header>
            <div class="aui-dialog2-content">
                <form class="aui" id="upload-attachment-file-to-onedrive-form">
                    <fieldset>
                        <legend><span>File upload</span></legend>
                        <div class="field-group" id="file-upload-field-group">
                            <label for="file-upload-field">Upload file</label>
                            <input class="upfile" type="file" id="file-upload-field" name="file" multiple required>
                        </div>
                    </fieldset>
             	</form>
            </div>
            <footer class="aui-dialog2-footer">
                <div class="aui-dialog2-footer-actions">
                    <div class="buttons-container">
                        <div class="buttons">
                            <input class="aui-button aui-button-primary submit" type="submit" value="Attach to OneDrive" id="create-button">
                            <button type="button" accesskey="`" title="Press Alt+` to cancel" class="aui-button aui-button-link cancel" resolved="" id="cancel-button">Cancel</button>
                        </div>
                    </div>      
                </div>
                <div class="aui-dialog2-footer-hint">
                    <p>Hold Ctrl/Command to select multiple files in explorer</p>
                </div>
            </footer>
        </section>
    """
    log.warn("${remoteUserManager.getRemoteUser(request)?.username as String} triggered dialog")
    Response.ok().type(MediaType.TEXT_HTML).entity(dialog.toString()).build()
}

uploadContractFileToOneDriveDialog(httpMethod: 'GET', groups: []) { MultivaluedMap queryParams, HttpServletRequest request -> 
    def remoteUserManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager)
    def dialog =
    """
        <section role="dialog" id="upload-contract-file-to-onedrive-dialog" class="aui-layer aui-dialog2 aui-dialog2-medium" aria-hidden="true" data-aui-remove-on-hide="true">
            <header class="aui-dialog2-header">
            	<h2 class="aui-dialog2-header-main">Select file to attach</h2>
            	<a class="aui-dialog2-header-close">
              		<span class="aui-icon aui-icon-small aui-iconfont-close-dialog" id="close-button">Close</span>
            	</a>
          	</header>
            <div class="aui-dialog2-content">
                <form class="aui" id="upload-contract-file-to-onedrive-form">
                    <fieldset>
                        <legend><span>File upload</span></legend>
                        <div class="field-group" id="file-upload-field-group">
                            <label for="file-upload-field">Upload file</label>
                            <input class="upfile" type="file" id="file-upload-field" name="file" required>
                        </div>
                    </fieldset>
             	</form>
            </div>
            <footer class="aui-dialog2-footer">
                <div class="aui-dialog2-footer-actions">
                    <div class="buttons-container">
                        <div class="buttons">
                            <input class="aui-button aui-button-primary submit" type="submit" value="Attach to OneDrive" id="create-button">
                            <button type="button" accesskey="`" title="Press Alt+` to cancel" class="aui-button aui-button-link cancel" resolved="" id="cancel-button">Cancel</button>
                        </div>
                    </div>      
                </div>
                <div class="aui-dialog2-footer-hint">
                    <p>Select contract file with extension .doc, .docx or .docm</p>
                </div>
            </footer>
        </section>
    """
    log.warn("${remoteUserManager.getRemoteUser(request)?.username as String} triggered dialog")
    Response.ok().type(MediaType.TEXT_HTML).entity(dialog.toString()).build()
}

uploadFileToOneDrive(httpMethod: 'POST', groups: []) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
    def attachmentManager = ComponentAccessor.getAttachmentManager()
    def issueManager = ComponentAccessor.getIssueManager()
    def userManager = ComponentAccessor.getUserManager()

    def token = getToken()
    def admin = userManager.getUserByName('admin')
    def fileType = queryParams.getFirst('type') as String
    def attachmentIdList = []
    String filename = ''
    String message = ''
    MutableIssue issue = issueManager.getIssueObject(queryParams.getFirst('issueKey') as String)
    List<ApplicationUser> persons = getPersons(issue)
    log.warn("invite do document $persons")
    for (attachmentId in body.split(',')) {  //body contains jira attachment id's
        def attachment = attachmentManager.getAttachment(attachmentId as Long)
        filename = getFilename(fileType, attachment, issue)
        def results = sendFileToOneDrive(issue, attachment, persons, filename, token)
        issue = results[0]
        attachmentIdList.add(results[1])
        message = "${message}File <strong>$attachment.filename</strong> has been uploaded to OneDrive</br>"
    }
    if (fileType == 'contract') {
        Double nextContractIteration = (issue.getCustomFieldValue(CONTRACT_ITERATION_FIELD) as Double) + 1
        issue.setCustomFieldValue(ACTIVE_CONTRACT_FIELD, filename + ' (DRAFT ' + (int) nextContractIteration + ')')
        issue.setCustomFieldValue(ACTIVE_CONTRACT_ID_FIELD, attachmentIdList[0])
        issue.setCustomFieldValue(CONTRACT_ITERATION_FIELD, nextContractIteration)
    }
    else if (fileType == 'attachment') {
        String contractAttachments = issue.getCustomFieldValue(CONTRACT_ATTACHMENTS_ID_FIELD)
        if (contractAttachments)
            issue.setCustomFieldValue(CONTRACT_ATTACHMENTS_ID_FIELD, contractAttachments + ',' + attachmentIdList.join(','))
        else
            issue.setCustomFieldValue(CONTRACT_ATTACHMENTS_ID_FIELD, attachmentIdList.join(','))
    }
    issueManager.updateIssue(admin, issue, EventDispatchOption.ISSUE_UPDATED, false)
    if (fileType == 'contract') {
	    def remoteUserManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager)
        def remoteUser = userManager.getUserByName(remoteUserManager.getRemoteUser(request)?.username as String)
        transistIssue(remoteUser, issue, COMMENTED_BY_CONTRACTOR_TRANSITION_ID) //transist to Sent to contract -> Working on document if possible
    }   
    UserMessageUtil.success(message)
    return Response.ok([success: message]).build()
}

updatePermissionsOneDriveFiles(httpMethod: 'POST', groups: ['jira-api-users']) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
    def issueManager = ComponentAccessor.getIssueManager()

    MutableIssue issue = issueManager.getIssueObject(queryParams.getFirst('issueKey') as String)
    List<ApplicationUser> usersToAdd = getUserObjectList(queryParams.getFirst('usersToAdd') as String)
    List<ApplicationUser> usersToDel = getUserObjectList(queryParams.getFirst('usersToDel') as String)
    List<ApplicationUser> usersToChange = getUserObjectList(queryParams.getFirst('usersToChange') as String)
    def contractAttachments = null
    def activeContract = null
    List<String> allRelatedFiles = []
    try {
        contractAttachments = (issue.getCustomFieldValue(CONTRACT_ATTACHMENTS_ID_FIELD) as String).split(',') as List<String>
    }
    catch (NullPointerException e) {
        log.warn("'${CONTRACT_ATTACHMENTS_ID_FIELD.getFieldName()}' field is empty")
    } 
    try {
        activeContract = issue.getCustomFieldValue(ACTIVE_CONTRACT_ID_FIELD) as String
        if (activeContract == '' || !activeContract)
            throw new IllegalArgumentException()
    }
    catch (NullPointerException | IllegalArgumentException e) {
        log.warn("'${ACTIVE_CONTRACT_ID_FIELD.getFieldName()}' field is empty")
    }
    if (contractAttachments && !activeContract)
        allRelatedFiles = contractAttachments
    else if (!contractAttachments && activeContract)
        allRelatedFiles.push(activeContract)
    else if (!contractAttachments && !activeContract) {
        log.warn('not found any file on OneDrive')
        return Response.ok([success: 'not found any file on OneDrive']).build()
    }
    else
        allRelatedFiles = contractAttachments + [activeContract]  // attachments + active contract file ID
    def permissionValue = queryParams.getFirst('permission') as String
    if (!permissionValue || permissionValue == '')
        permissionValue = getPermissionValue(issue)
    updatePermissions(permissionValue, allRelatedFiles, usersToChange, usersToDel)
    if (usersToAdd) {
        for (relatedFile in allRelatedFiles) {
            log.warn("$usersToAdd added to document id '$relatedFile' with permission '$permissionValue'")
            invitePersonsToDocument(usersToAdd, relatedFile, permissionValue, token)
        }
    }
    else
        log.warn("no new users to add")
    return Response.ok([success: 'ok']).build()
}

void updatePermissions(String permissionValue, List<String> files, List<ApplicationUser> usersToChange, List<ApplicationUser> usersToDel) {
    def userManager = ComponentAccessor.getUserManager()

    for (file in files) {
        def urlGetPermission = "$BASE_MICROSOFT_URL/me/drive/items/$file/permissions"
        Map permissions = sendRequest(urlGetPermission, 'GET', null, 'application/json', token)
        for (permission in permissions.value) {
            def urlPostPermission = "$BASE_MICROSOFT_URL/me/drive/items/$file/permissions/${permission.id}"
            ApplicationUser currUser
            try {
                currUser = userManager.getUserByName((permission.grantedTo.user.email).toString().split('@')[0])
            }
            catch (NullPointerException e) {
                currUser = userManager.getUserByName((permission.grantedToIdentitiesV2.user[0].email).toString().split('@')[0])
            }
            if (usersToDel && usersToDel.contains(currUser)) {
                log.warn("$currUser.username permission deleted in file id '$file'")
                log.warn(sendRequest(urlPostPermission, 'DELETE', null, 'application/json', token))
            }
            if (usersToChange && usersToChange.contains(currUser)) {
                log.warn("$currUser.username permission changed to '$permissionValue' in file id '$file")
                log.warn(sendRequest(urlPostPermission, 'PATCH', "{'roles': ['${permissionValue}']}", 'application/json', token))
            }
            else
                log.error("not found $currUser on list, no changes in permission")
        }
    }
}

List sendFileToOneDrive(MutableIssue issue, Attachment attachment, List<ApplicationUser> persons, String filename, String token) {
    def attachmentManager = ComponentAccessor.getAttachmentManager()
    
    def attachmentFile = getAttachmentFile(issue, attachment.id) 
    def url = "$BASE_MICROSOFT_URL/me/drive/items/$BASE_FOLDER_ID:/$issue.key/$filename:/content"
    def documentId
    Long currStatusId = issue.getStatusId() as Long
    Double currIteration = issue.getCustomFieldValue(CONTRACT_ITERATION_FIELD) as Double
    log.warn("upload '$attachment.filename' to OneDrive")
    try {
        Map response = sendRequest(url, 'PUT', attachmentFile.getText('ISO-8859-1'), 'text/plain', token) // request to upload
        documentId = response.id.toString()
        def permission = getPermissionValue(issue)
        log.warn("add '$permission' permission to '$attachment.filename' for $persons")
        if (currStatusId == NEW_STATUS_ID && currIteration >= 1) {
            log.warn("curr status of issue is 'New', old contract file change to 'read'")
            String oldActiveContractId = issue.getCustomFieldValue(ACTIVE_CONTRACT_ID_FIELD) as String
            updatePermissions('read', [oldActiveContractId], persons, null)
        }
        invitePersonsToDocument(persons, documentId, permission, token)
        //attachmentManager.deleteAttachment(attachment)
    }
    catch(ValidationException errorMsg) {
        log.error(errorMsg)
    }
    if (issue.getCustomFieldValue(WORKING_FOLDER_FIELD) == null)
        issue.setCustomFieldValue(WORKING_FOLDER_FIELD, getWorkingFolder(issue.key, token))
    return [issue, documentId]
}
Map sendRequest(String url, String methodName, String requestBodyValue, String contentTypeValue, String token) {
    def client = new HttpClient()
    def parser = new JsonSlurper()
    def method
    if (methodName == 'PUT')
        method = new PutMethod(url)
    else if (methodName == 'POST')
        method = new PostMethod(url)
    else if (methodName == 'GET')
        method = new GetMethod(url)
    else if (methodName == 'PATCH')
        return sendPatchRequest(url, requestBodyValue, contentTypeValue, token)
    else if (methodName == 'DELETE')
        method = new DeleteMethod(url)
    else
        throw new ValidationException('wrong method name')
    method.setRequestHeader('Authorization', 'Bearer ' + token)
    method.setRequestHeader('Content-Type', contentTypeValue)
    if (methodName == 'PUT' || methodName == 'POST')
        method.setRequestBody(requestBodyValue)
    log.warn("${method.name}: ${method.getURI()}")
    client.executeMethod(method)
    if (methodName == 'DELETE')
        return log.warn('no response content, DELETE method')
    Map response = parser.parseText(method.getResponseBodyAsString()) as Map
    log.warn(method.getResponseBodyAsString())
    def httpCode = method.getStatusCode()
    method.releaseConnection()
    if (httpCode.toString().startsWith('20')) {  
        log.warn("OK: ${httpCode} ${method.getURI()}")
        return response
    }
    else
        return log.error("FAIL: ${httpCode} [${response.error.code}] ${response.error.message}") //skip error
}

Map sendPatchRequest(String url, String requestBodyValue, String contentTypeValue, String token) {
    def client = new DefaultHttpClient()
    def parser = new JsonSlurper()
    def method = new HttpPatch(url)
    method.setHeader('Authorization', 'Bearer ' + token)
    method.setHeader('Content-Type', contentTypeValue)
    method.setEntity(new StringEntity(requestBodyValue))
    log.warn("PATCH: ${method.getURI()}")
    HttpResponse response = client.execute(method)
    HttpEntity entity = response.getEntity()
    log.warn(entity.getContent().toString())
    method.releaseConnection()
}

void invitePersonsToDocument(List<ApplicationUser> persons, String documentId, String permission, String token) {
    def invitationRecipients = ''
    def url = "$BASE_MICROSOFT_URL/me/drive/items/$documentId/invite"
    for (person in persons)
        invitationRecipients = invitationRecipients + '{"email": "' + person.emailAddress + '"},'
    def requestBody = 
    """
        {
            "recipients": [
                ${invitationRecipients}
            ],
            "roles": [ "$permission" ],
            "message": "optional",
            "sendInvitation": false,
            "requireSignIn": true,
        }
    """
    try {
        Map response = sendRequest(url, 'POST', requestBody, 'application/json', token)
    }
    catch(ValidationException errorMsg) {
        log.error(errorMsg)
    }
}

File getAttachmentFile(Issue issue, Long attachmentId){
    def fileSystemAttachmentDirectoryAccessor = ComponentAccessor.getComponent(FileSystemAttachmentDirectoryAccessor.class)
    return fileSystemAttachmentDirectoryAccessor.getAttachmentDirectory(issue).listFiles().find({
        File it->
            it.getName().equals(attachmentId as String)
    })
}

String getFilename(String fileType, Attachment attachment, Issue issue) {
    def filename = attachment.filename.take(attachment.filename.lastIndexOf('.'))
    def fileExtension = attachment.filename.split('\\.')[-1]  
    if (fileType == 'contract') {
        Double contractNextIteration = (issue.getCustomFieldValue(CONTRACT_ITERATION_FIELD) as Double) + 1
        return "${transformFilename(filename)}_v${(int)contractNextIteration}.$fileExtension"
    }
    else if (fileType == 'attachment')
        return "${transformFilename(filename)}.$fileExtension"
}

String getPermissionValue(Issue issue) {
    def currStatusId = issue.getStatusId() as Long
    log.warn("current status of issue: ${issue.getStatus().getName()}")
    if (currStatusId == WORKING_ON_DOCUMENT_STATUS_ID || currStatusId == NEW_STATUS_ID)
        return 'write'
    else
        return 'read'
}

List<ApplicationUser> getUserObjectList(String usersString) {
    if (!usersString)
        return null
    else {
        def userManager = ComponentAccessor.getUserManager()
        List<String> usersStringList = usersString.split(',') as List<String>
        List<ApplicationUser> users = []
        for (username in usersStringList) {
            ApplicationUser user = userManager.getUserByName(username)
            if (!users.contains(user))
                users.push(user)
        }
        log.warn("users given in request: $users")
        return users
    }
}

String getWorkingFolder(String issueKey, String token) {
    def url = "$BASE_MICROSOFT_URL/me/drive/items/$BASE_FOLDER_ID/children"
    Map response = sendRequest(url, 'GET', null, 'application/json', token)
    for (folder in response.value) {
        if (folder['name'] == issueKey)
            return folder['webUrl']    
    }
    return 'RELATED_DIR_NOT_FOUND'
}

List<ApplicationUser> getPersons(Issue issue) {  
    def permissionManager = ComponentAccessor.getPermissionManager()
    def groupManager = ComponentAccessor.getGroupManager()

    def departments = issue.getCustomFieldValue(DEPARTMENTS_INVOLVED_FIELD)
    List<ApplicationUser> personsInvolved = issue.getCustomFieldValue(PERSONS_INVOLVED_FIELD) as List<ApplicationUser>
    List<ApplicationUser> personsSubstantive = issue.getCustomFieldValue(SUBSTANTIVE_PERSONS_FIELD) as List<ApplicationUser>
    List<ApplicationUser> personsInDepartmentsInvolved = []
    for (department in departments) { // get user list to invite
        def departmentUsers = groupManager.getUsersInGroup(department.toString().split(':')[0].toLowerCase())
        for (departmentUser in departmentUsers) {
            log.warn(departmentUser)
            if (permissionManager.hasPermission(new ProjectPermissionKey('BROWSE_PROJECTS'), issue, departmentUser))
                personsInDepartmentsInvolved.add(departmentUser)
            else
                log.warn("$departmentUser deleted from list of users to invite, cause: permission to project denied")
        } 
    }
    if (personsInvolved && !personsInDepartmentsInvolved)
        return personsInvolved + personsSubstantive
    else if (!personsInvolved && personsInDepartmentsInvolved)
        return personsInDepartmentsInvolved + personsSubstantive
    else if (!personsInvolved && !personsInDepartmentsInvolved)
        return personsSubstantive
    else
        return personsInvolved + personsSubstantive + personsInDepartmentsInvolved
}

String getToken(){
    def applicationProperties = ScriptRunnerImpl.getOsgiService(ApplicationProperties)
    def jiraBaseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)
    def client = new HttpClient()
    def method = new GetMethod("$jiraBaseUrl/rest/scriptrunner/latest/custom/getMicrosoftGraphToken")
    def credentials = new UsernamePasswordCredentials(LOCAL_API_USER, LOCAL_API_PASSWORD)
    client.getParams().setAuthenticationPreemptive(true)
    client.getState().setCredentials(AuthScope.ANY, credentials)
    client.executeMethod(method)
    return method.getResponseBodyAsString()
}

void transistIssue(ApplicationUser user, Issue issue, Integer transitionId) {
  def issueService = ComponentAccessor.getIssueService()
  def transitionOptions = new TransitionOptions.Builder().skipConditions().skipPermissions().skipValidators().build()

  def transitionValidationResult = issueService.validateTransition(user, issue.id, transitionId, new IssueInputParametersImpl(), transitionOptions)
  if (transitionValidationResult.isValid())
    issueService.transition(user, transitionValidationResult)
}

String transformFilename(String filename){  // replace polish dictratic characters
    def output = filename.toString().replace(' ', '_')
    output = output.replace('ó', 'o')
    output = output.replace('ą', 'a')
    output = output.replace('ż', 'z')
    output = output.replace('ź', 'z')
    output = output.replace('ć', 'c')
    output = output.replace('ś', 's')
    return output
}
