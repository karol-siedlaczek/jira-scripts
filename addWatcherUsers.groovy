import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue

def userManager = ComponentAccessor.getUserManager()
def watcherManager = ComponentAccessor.getWatcherManager()
def projectManager = ComponentAccessor.getProjectManager()

def dicts = [
    [
        'users': ['user_1'], 
        'projects': ['project_key_1', 'project_key_2']
    ],
    [
        'users': ['user_2', 'user_3'], 
        'projects': ['project_key3']
    ]
]

def eventIssue = event.getIssue()
def eventProject = eventIssue.getProjectObject()

for (dict in dicts) {
    def projects = dict.get('projects')
    for (project in projects) {
        if (eventProject.key == project) {
            def users = dict.get('users')
            for (user in users) {
                def userObj = userManager.getUserByName(user)
                watcherManager.startWatching(userObj, eventIssue)
                log.warn("${user} has been added as watcher to ${project}")
            }
        }
    }          
}
