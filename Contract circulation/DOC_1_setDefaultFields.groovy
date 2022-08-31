/* Karol Siedlaczek Redge Technologies 2022
    domyslne pola
*/

import groovy.transform.Field
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.user.ApplicationUser

def issueManager = ComponentAccessor.getIssueManager()
@Field UserManager userManager = ComponentAccessor.getUserManager()
@Field CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
@Field CustomField SUBSTANTIVE_PERSONS_FIELD = customFieldManager.getCustomFieldObject(1)
@Field CustomField APPROVER_FIELD = customFieldManager.getCustomFieldObject(1)

def admin = userManager.getUserByName('<ADMIN_USER>')
issue = setDefaultSubstantivePerson(issue)
issue = setDefaultApprovers(issue)
issue = setDefaultSupervisoryApprovers(issue)
issueManager.updateIssue(admin, issue, EventDispatchOption.ISSUE_UPDATED, false)

MutableIssue setDefaultSubstantivePerson(MutableIssue issue) {
    List<ApplicationUser> currSubstantivePersons = issue.getCustomFieldValue(SUBSTANTIVE_PERSONS_FIELD) as List<ApplicationUser>
    if (currSubstantivePersons)
        issue.setCustomFieldValue(SUBSTANTIVE_PERSONS_FIELD, currSubstantivePersons + [issue.getReporter()])
    else
        issue.setCustomFieldValue(SUBSTANTIVE_PERSONS_FIELD, [issue.getReporter()])
    return issue
}

MutableIssue setDefaultApprovers(MutableIssue issue) {
    def groupManager = ComponentAccessor.getGroupManager()
    def boardMembers = groupManager.getUsersInGroup('board')
    issue.setCustomFieldValue(APPROVER_FIELD, boardMembers)
    return issue
}

MutableIssue setDefaultSupervisoryApprovers(MutableIssue issue) {
    //TO DO
    return issue
}
