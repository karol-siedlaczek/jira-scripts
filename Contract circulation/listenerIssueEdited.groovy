import groovy.transform.Field
import groovy.json.JsonSlurper
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.Issue
import com.atlassian.sal.api.UrlMode
import com.atlassian.jira.security.plugin.ProjectPermissionKey
import org.apache.commons.httpclient.UsernamePasswordCredentials
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import org.apache.commons.httpclient.auth.AuthScope
import com.atlassian.sal.api.ApplicationProperties
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.*

@Field String LOCAL_API_USER = '<USERNAME>'
@Field String LOCAL_API_PASSWORD = '<PASS>'

Issue issue = event.getIssue()
def changeLog = event.getChangeLog()
def changeItems = changeLog.getRelated("ChildChangeItem")
def fieldsToCheck = ['substantive persons', 'persons involved', 'departments involved']
List<String> usersToAdd = []
List<String> usersToDel = []

changeItems.each {
    def fieldName = (it['field'] as String).toLowerCase()
    if (fieldsToCheck.contains(fieldName)) {
        log.warn(it)
        List userLists
        if (fieldName == 'departments involved')
            userLists = getUsernameListsFromDepartmentField(fieldName, it['oldstring'] as String, it['newstring'] as String, issue)
        else
            userLists = getUsernameListsFromUserField(fieldName, it['oldvalue'] as String, it['newvalue'] as String)
        usersToAdd += userLists[0]
        usersToDel += userLists[1]
    }
    else
        log.warn("'$fieldName' is not field to check")
}

if (usersToAdd && !usersToDel)
    log.warn(sendPostRequest("/rest/scriptrunner/latest/custom/updatePermissionsOneDriveFiles?issueKey=${issue.key}&usersToAdd=${usersToAdd.join(',')}"))
else if(!usersToDel && usersToAdd)
    log.warn(sendPostRequest("/rest/scriptrunner/latest/custom/updatePermissionsOneDriveFiles?issueKey=${issue.key}&usersToDel=${usersToDel.join(',')}"))
else
    log.warn(sendPostRequest("/rest/scriptrunner/latest/custom/updatePermissionsOneDriveFiles?issueKey=${issue.key}&usersToAdd=${usersToAdd.join(',')}&usersToDel=${usersToDel.join(',')}"))

List getUsernameListsFromUserField(String fieldName, String oldValueString, String newValueString) {
    List<String> oldValues
    List<String> newValues
    try {
        oldValues = oldValueString.replace('[', '').replace(']', '').replace(' ', '').split(',') as List<String>
    }
    catch (NullPointerException e) {
        oldValues = []
    }
    try {
        newValues = newValueString.replace('[', '').replace(']', '').replace(' ', '').split(',') as List<String>
    }
    catch (NullPointerException e) {
        newValues = []
    }
    log.warn("changed field: $fieldName; old values: $oldValues; new values: $newValues")
    return getFilteredUsernameLists(oldValues, newValues)
}

List getUsernameListsFromDepartmentField(String fieldName, String oldValue, String newValue, Issue issue) {
    List<String> oldValues
    List<String> newValues
    try {
        oldValues = getUsernamesFromDepartment(oldValue.split(',') as List<String>, issue)
    }
    catch (NullPointerException e) {
        oldValues = []
    }
    try {
        newValues = getUsernamesFromDepartment(newValue.split(',') as List<String>, issue) 
    }
    catch (NullPointerException e) {
        newValues = []
    }
    log.warn("changed field: $fieldName; old values: $oldValues; new values: $newValues")
    return getFilteredUsernameLists(oldValues, newValues)
}

List<String> getUsernamesFromDepartment(List<String> departments, Issue issue) {
    def permissionManager = ComponentAccessor.getPermissionManager()
    def groupManager = ComponentAccessor.getGroupManager()

    List<String> usersInDepartment = []
    for (department in departments) { // get user list to invite, remove users which does not have permission to browse project
        def departmentUsers = groupManager.getUsersInGroup(department.toString().split(':')[0].toLowerCase())
        for (departmentUser in departmentUsers) {
            if (permissionManager.hasPermission(new ProjectPermissionKey('BROWSE_PROJECTS'), issue, departmentUser))
                usersInDepartment.add(departmentUser.username)
            else
                log.warn("$departmentUser deleted from list of users to invite, cause: permission to project denied")
        } 
    }
    return usersInDepartment
}

List getFilteredUsernameLists(List<String> oldValues, List<String> newValues) {
    List<String> usersToAdd = []
    List<String> usersToDel = oldValues
    for (newValue in newValues) {
        if (usersToDel.contains(newValue))
            usersToDel -= newValue
        else if (!usersToDel.contains(newValue))
            usersToAdd.add(newValue)
    }
    log.warn("users to delete: $usersToDel")
    log.warn("users to add: $usersToAdd")
    return [usersToAdd, usersToDel]
}

String sendPostRequest(String url) {
    def applicationProperties = ScriptRunnerImpl.getOsgiService(ApplicationProperties)
    def baseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)
    def client = new HttpClient()
    def method = new PostMethod(baseUrl + url)
    method.setRequestHeader('Content-Type', 'application/json')
    def credentials = new UsernamePasswordCredentials(LOCAL_API_USER, LOCAL_API_PASSWORD)
    client.getParams().setAuthenticationPreemptive(true)
    client.getState().setCredentials(AuthScope.ANY, credentials)
    client.executeMethod(method)
    def httpCode = method.getStatusCode()
    method.releaseConnection()
    if (httpCode.toString().startsWith('20'))
        return "OK: [${method.name} ${httpCode}]: ${method.getURI()} ${method.getResponseBodyAsString()}"
    else
        return "FAIL: [${method.name} ${httpCode}]: ${method.getURI()} ${method.getResponseBodyAsString()}"
}
