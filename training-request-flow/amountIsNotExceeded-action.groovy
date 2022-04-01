import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;

def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def commentManager = ComponentAccessor.getCommentManager()
def userManager = ComponentAccessor.getUserManager()
def groupManager = ComponentAccessor.getGroupManager()

// Map list in format Team:Director
def teamMap = ['team1': 'manager1',
               'team2': 'manager2',
               'team3': 'manager3',
               'team4': 'manager4',]
// Map list in format Department:Director
def departmentMap = ['department1': 'director1',
                     'department2': 'director2',
                     'department3': 'director3',]

def currIssue = issueManager.getIssueObject(issue.key)
def currUser = ComponentAccessor.getJiraAuthenticationContext().loggedInUser
def approvingManagerField = customFieldManager.getCustomFieldObject(11607) // Approving manager field ID
def approvingDirectorField = customFieldManager.getCustomFieldObject(11606) // Approving director field ID
def approvingManager
def approvingDirector
def loggedUserTeam
def loggedUserDepartment

teamMap.each {team, manager -> 
    if (groupManager.getUserNamesInGroup(team).contains(currUser.name)){
        approvingManager = userManager.getUserByName(manager)
        loggedUserTeam = team
    }    	
}

departmentMap.each {department, director -> 
    if (groupManager.getUserNamesInGroup(department).contains(currUser.name)){
        approvingDirector = userManager.getUserByName(director)
        loggedUserDepartment = department
    }   	
}

log.warn('\nDetailed info about reporter: \n' +
    	 '\tUsername: ' + currUser.name + '\n' +
         '\tDepartment: ' + loggedUserDepartment + '\n' +
         '\tTeam: ' + loggedUserTeam + '\n' +
         '\tIssue Key: ' + currIssue.key + '\n')

if (approvingManager == null && approvingDirector != null){ // if there is no team in department or didn't find
    approvingManagerField.updateValue(null, currIssue, new ModifiedValue(currIssue.getCustomFieldValue(approvingManagerField), approvingDirector), new DefaultIssueChangeHolder()) //update field    
    log.warn(approvingDirector.name + " has been assigned as Approving Manager, because did not found related Manager in user's team")
}
else if (approvingManager == null && approvingDirector == null){
    log.error('Did not found related Manager')
}
else {
    approvingManagerField.updateValue(null, currIssue, new ModifiedValue(currIssue.getCustomFieldValue(approvingManagerField), approvingManager), new DefaultIssueChangeHolder()) //update field    
    log.warn(approvingManager.name + ' has been assigned as Approving Manager')
}

if (approvingDirector == null){
    log.error('Did not found related Director')
}
else {
    approvingDirectorField.updateValue(null, currIssue, new ModifiedValue(currIssue.getCustomFieldValue(approvingDirectorField), approvingDirector), new DefaultIssueChangeHolder()) //update field
    log.warn(approvingDirector.name + ' has been assigned as Approving Director')
}
