import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue

def userManager = ComponentAccessor.getUserManager()
def groupManager = ComponentAccessor.getGroupManager()
def watcherManager = ComponentAccessor.getWatcherManager()

def issue = event.getIssue()
def users = groupManager.getUsersInGroup('{group-name}')

for (user in users){
    watcherManager.startWatching(user, issue)    
    log.info(user.name + ' added as watcher to issue')    
}
