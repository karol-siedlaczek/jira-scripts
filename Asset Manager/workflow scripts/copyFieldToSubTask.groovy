import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.ModifiedValue

def customFieldManager = ComponentAccessor.getCustomFieldManager()

def USER_FIELD = customFieldManager.getCustomFieldObject(11701)
def PLACE_FIELD = customFieldManager.getCustomFieldObject(11509)

if (issue.subTask)
    return

def subTasks = issue.getSubTaskObjects()
def parentUserFieldValue = issue.getCustomFieldValue(USER_FIELD)
def parentPlaceFieldValue = issue.getCustomFieldValue(PLACE_FIELD)

for (subTask in subTasks){
    log.warn(subTask.key)
    USER_FIELD.updateValue(null, subTask, new ModifiedValue(issue.getCustomFieldValue(USER_FIELD), parentUserFieldValue), new DefaultIssueChangeHolder())
    PLACE_FIELD.updateValue(null, subTask, new ModifiedValue(issue.getCustomFieldValue(PLACE_FIELD), parentPlaceFieldValue), new DefaultIssueChangeHolder())
}