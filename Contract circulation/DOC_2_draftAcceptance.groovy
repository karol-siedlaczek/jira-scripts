import java.lang.String
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.workflow.TransitionOptions
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.IssueTypeManager
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.security.plugin.ProjectPermissionKey
import com.atlassian.jira.security.JiraAuthenticationContext

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def groupManager = ComponentAccessor.getGroupManager()
def userManager = ComponentAccessor.getUserManager()
def issueManager = ComponentAccessor.getIssueManager()
def jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()

def PERSONS_INVOLVED_FIELD = customFieldManager.getCustomFieldObject(1)
def DEPARTMENTS_INVOLVED_FIELD = customFieldManager.getCustomFieldObject(1)
def SUBSTANTIVE_PERSONS_FIELD = customFieldManager.getCustomFieldObject(1)
def INVOLVED_INSTANCE_DATA_FIELD = customFieldManager.getCustomFieldObject(1)

def admin = userManager.getUserByName('<ADMIN_USER>')
def loggedUser = jiraAuthenticationContext.getLoggedInUser()
List<Issue> updatedSubIssues = []
List<ApplicationUser> personsInvolved = issue.getCustomFieldValue(PERSONS_INVOLVED_FIELD) //persons involved (skip error)
List<ApplicationUser> personsSubstantive = issue.getCustomFieldValue(SUBSTANTIVE_PERSONS_FIELD)
List<ApplicationUser> persons
if (personsInvolved && !personsSubstantive)
    persons = personsInvolved
else if (!personsInvolved && personsSubstantive)
    persons = personsSubstantive
else
    persons = personsInvolved + personsSubstantive
def departments = issue.getCustomFieldValue(DEPARTMENTS_INVOLVED_FIELD) //deparments involved
def subTaskIssues = issue.getSubTaskObjects()

for (person in persons) {
    try {
        List result = existApprovementSubIssue(subTaskIssues, person.username, loggedUser, INVOLVED_INSTANCE_DATA_FIELD, customFieldManager)
        boolean subIssueExists = result[0]
        updatedSubIssues.push(result[1])
        if (!subIssueExists)
            createSubIssue(issue, person.username, person.displayName, loggedUser, [person], INVOLVED_INSTANCE_DATA_FIELD, customFieldManager)  
    }
    catch (NullPointerException e) {
        log.warn("$e, instance skipped")
    }
}

for (department in departments) {
    List result = existApprovementSubIssue(subTaskIssues, department.toString(), loggedUser, INVOLVED_INSTANCE_DATA_FIELD, customFieldManager)
    boolean subIssueExists = result[0]
    updatedSubIssues.push(result[1])
    if (!subIssueExists) {
        def departmentUsers = groupManager.getUsersInGroup(department.toString().split(':')[0].toLowerCase())
        createSubIssue(issue, department.toString(), department.toString().split(':')[-1], loggedUser, departmentUsers, INVOLVED_INSTANCE_DATA_FIELD, customFieldManager)
    }
}

for (subTaskIssue in subTaskIssues){
    if (!updatedSubIssues.contains(subTaskIssue)) {
        issueManager.deleteIssue(admin, subTaskIssue, EventDispatchOption.ISSUE_DELETED, false)
        log.warn("$subTaskIssue has been deleted")
    }
        
}

void createSubIssue(Issue parentIssue, 
                    String instanceData, 
                    String subSummary, 
                    ApplicationUser loggedUser, 
                    Collection<ApplicationUser> usersToApprove, 
                    CustomField involvedInstanceDataField, 
                    CustomFieldManager customFieldManager) {
    def issueFactory = ComponentAccessor.getIssueFactory()
    def issueManager = ComponentAccessor.getIssueManager()
    def issueTypeManager = ComponentAccessor.getComponent(IssueTypeManager)
    def subTaskManager = ComponentAccessor.getSubTaskManager()
    def permissionManager = ComponentAccessor.getPermissionManager()

    def APPROVEMENT_ISSUE_TYPE = issueTypeManager.getIssueType('11502')
    def APPROVER_FIELD = customFieldManager.getCustomFieldObject(10107)

    Collection<ApplicationUser> approvers = []
    Issue newSubIssue = issueFactory.getIssue()
    newSubIssue.setProjectObject(parentIssue.getProjectObject())
    newSubIssue.setIssueType(APPROVEMENT_ISSUE_TYPE)
    newSubIssue.setPriority(parentIssue.getPriority())
    newSubIssue.setReporter(loggedUser)
    newSubIssue.setSummary("Accept: ${subSummary}")
    newSubIssue.setCustomFieldValue(involvedInstanceDataField, instanceData)
    for (user in usersToApprove) {
        if (permissionManager.hasPermission(new ProjectPermissionKey('BROWSE_PROJECTS'), parentIssue, user))
            approvers.add(user)
        else
            log.warn("${user} deleted from list of approvers in ${newSubIssue}, because does not have permission to view ${parentIssue}")
    }
    newSubIssue.setCustomFieldValue(APPROVER_FIELD, approvers)
    issueManager.createIssueObject(loggedUser, newSubIssue)
    subTaskManager.createSubTaskIssueLink(parentIssue, newSubIssue, loggedUser)
    log.warn("${newSubIssue} created as child of ${parentIssue}")
}

List existApprovementSubIssue(Collection<Issue> subTaskIssues, 
                                 String instanceData,
                                 ApplicationUser loggedUser, 
                                 CustomField involvedInstanceDataField,
                                 CustomFieldManager customFieldManager) {
    Issue existingSubTask = getExistingApprovementSubIssue(subTaskIssues, instanceData, involvedInstanceDataField, customFieldManager)
    if (existingSubTask && existingSubTask.getStatusId() != '10908'){ // Status ID - Waiting for approval
        transistIssue(loggedUser, existingSubTask, 51) // transition ID - Waiting for approval
        log.warn("${existingSubTask} transitioned to starting status")
        return [true, existingSubTask]
    }
    else if (existingSubTask)
        return [true, existingSubTask]
    else
        return [false, existingSubTask]
}

Issue getExistingApprovementSubIssue(Collection<Issue> subTaskIssues, 
                                     String instanceData, 
                                     CustomField involvedInstanceDataField, 
                                     CustomFieldManager customFieldManager) {
    for (subTaskIssue in subTaskIssues){
        def involvedInstanceData = subTaskIssue.getCustomFieldValue(involvedInstanceDataField).toString()
        if (involvedInstanceData == instanceData){
            log.warn("${subTaskIssue} already exists with instance data ${involvedInstanceData}")
            return subTaskIssue
        }        
    }
    return null
}

void transistIssue(ApplicationUser user,  Issue issue, Integer transitionId) {
  def issueService = ComponentAccessor.getIssueService()
  def transitionOptions = new TransitionOptions.Builder().skipConditions().skipPermissions().skipValidators().build()

  def transitionValidationResult = issueService.validateTransition(user, issue.id, transitionId, new IssueInputParametersImpl(), transitionOptions)
  if (transitionValidationResult.isValid())
    issueService.transition(user, transitionValidationResult)
}
