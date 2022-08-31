import groovy.transform.Field
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import org.apache.commons.httpclient.UsernamePasswordCredentials
import com.atlassian.sal.api.UrlMode
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField
import org.apache.commons.httpclient.auth.AuthScope
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.security.plugin.ProjectPermissionKey
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.*

@Field CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
@Field String LOCAL_API_USER = '<USERNAME>'
@Field String LOCAL_API_PASSWORD = '<PASS>'
@Field CustomField PERSONS_INVOLVED_FIELD = customFieldManager.getCustomFieldObject(1)
@Field CustomField SUBSTANTIVE_PERSONS_FIELD = customFieldManager.getCustomFieldObject(1)
@Field CustomField DEPARTMENTS_INVOLVED_FIELD = customFieldManager.getCustomFieldObject(1)

List<String> usersToChange = getPersonUsernames(issue)

log.warn(sendPostRequest("/rest/scriptrunner/latest/custom/updatePermissionsOneDriveFiles?issueKey=${issue.key}&permission=read&usersToChange=${usersToChange.join(',')}"))

List<String> getPersonUsernames(Issue issue) {  
    def permissionManager = ComponentAccessor.getPermissionManager()
    def groupManager = ComponentAccessor.getGroupManager()

    def departments = issue.getCustomFieldValue(DEPARTMENTS_INVOLVED_FIELD)
    List<String> personsSubstantive = []
    List<String> personsInvolved = []
    List<String> personsInDepartmentsInvolved = []
    for (person in (issue.getCustomFieldValue(PERSONS_INVOLVED_FIELD) as List<ApplicationUser>))
        personsInvolved.push(person.username)
    for (person in (issue.getCustomFieldValue(SUBSTANTIVE_PERSONS_FIELD) as List<ApplicationUser>))
        personsSubstantive.push(person.username)
    for (department in departments) { // get user list to invite
        def departmentUsers = groupManager.getUsersInGroup(department.toString().split(':')[0].toLowerCase())
        for (departmentUser in departmentUsers) {
            log.warn(departmentUser)
            if (permissionManager.hasPermission(new ProjectPermissionKey('BROWSE_PROJECTS'), issue, departmentUser))
                personsInDepartmentsInvolved.add(departmentUser.username)
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
