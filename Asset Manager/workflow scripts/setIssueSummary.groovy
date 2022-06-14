import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser

def customFieldManager = ComponentAccessor.getCustomFieldManager()

if (issue.issueType.name == 'Device' | issue.issueType.name == 'Sub-Device') {
    def MODEL_FIELD = customFieldManager.getCustomFieldObject(11503)
    def modelFieldValue = issue.getCustomFieldValue(MODEL_FIELD)
    log.warn(modelFieldValue)
    log.warn("test: " + modelFieldValue)
    if (modelFieldValue != null)
        issue.summary = modelFieldValue as String
}
else if (issue.issueType.name == 'Person') {
    def userField = customFieldManager.getCustomFieldObject(11701)
    def userFieldValue = issue.getCustomFieldValue(userField)
    if (userFieldValue != null || userFieldValue != '')
        issue.summary = (userFieldValue as ApplicationUser).displayName
}
else { // License, Sub-License, Access
    def components = issue.getComponents()

    def i = 0
    for (component in components){
        if (i == 0)
            issue.summary = component.name
        else
            issue.summary = issue.summary + ' | ' + component.name
        i++
    }
}