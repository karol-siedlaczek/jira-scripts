import com.atlassian.jira.component.ComponentAccessor

def userManager = ComponentAccessor.getUserManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def commentManager = ComponentAccessor.getCommentManager()

def USER_FIELD_ID = 123
def PLACE_FIELD_ID = 123

def userField = customFieldManager.getCustomFieldObject(USER_FIELD_ID)
def placeField = customFieldManager.getCustomFieldObject(PLACE_FIELD_ID)
def adminUser = userManager.getUserByName('admin')

def userFieldValue = issue.getCustomFieldValue(userField)['displayName']
def placeFieldValue = issue.getCustomFieldValue(placeField)
def assetName
def comment

if (issue.issueType.name == 'License')
    assetName = 'License'
else
    assetName = 'Device'

if (issue.status.name == 'On loan')
    comment = "on loan in ${placeFieldValue}"
else if(issue.status.name == 'Collocation')
    comment = "placed in ${placeFieldValue}"
else  
	comment = "assigned to ${userFieldValue}"

comment = "${assetName} ${comment}"
commentManager.create(issue, adminUser, comment, null, null, new Date(), null, false) //issue, user which commented, comment body, restricted group, restricted role, notifications
