import com.atlassian.jira.component.ComponentAccessor

def userManager = ComponentAccessor.getUserManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def commentManager = ComponentAccessor.getCommentManager()

def userField = customFieldManager.getCustomFieldObject(11701)
def placeField = customFieldManager.getCustomFieldObject(11509)
def adminUser = userManager.getUserByName('admin')

def assetName
def comment

if (issue.issueType.name == 'License')
    assetName = 'License'
else
    assetName = 'Device'

if (issue.status.name == 'On loan')
    comment = "on loan in ${issue.getCustomFieldValue(placeField)}"
else if(issue.status.name == 'Collocation')
    comment = "placed in ${issue.getCustomFieldValue(placeField)}"
else  
	comment = "assigned to ${issue.getCustomFieldValue(userField)['displayName']}"

comment = "${assetName} ${comment}"
log.warn("'${comment}' added to ${issue.key}")
commentManager.create(issue, adminUser, comment, 'groupname', null, new Date(), null, false) //issue, user which commented, comment body, restricted group, restricted role, notifications
