import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.UserLocaleStore

JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()
UserLocaleStore localeStore = ComponentAccessor.getComponentOfType(UserLocaleStore.class)
ApplicationUser loggedUser = jiraAuthenticationContext.getLoggedInUser()
Locale locale = localeStore.getLocale(loggedUser)
String language = locale.getDisplayLanguage()
log.warn(language)
