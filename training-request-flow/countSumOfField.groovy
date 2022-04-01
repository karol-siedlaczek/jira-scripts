import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder

def userManager = ComponentAccessor.getUserManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchService = ComponentAccessor.getComponent(SearchService)

def TrainingCost = customFieldManager.getCustomFieldObject(11600) //Training Cost [PLN] field ID
def excedeedAmount = customFieldManager.getCustomFieldObject(11605) //Excedeed Amount [PLN] field ID
def maxTrainingCost = 5000 // Maximum amount allocated to training per year for a single employee
def sumTrainingCost = 0
def excessValue = 0
def issueTrainingCostValue

def currentUser = ComponentAccessor.getJiraAuthenticationContext().loggedInUser
def query = jqlQueryParser.parseQuery("project = $issue.projectObject.key AND issuetype = 'Training Request' AND createdDate > startOfYear() AND createdDate < EndOfYear() AND reporter = $currentUser.name AND Resolution = Zamknięty")
def results = searchService.search(userManager.getUserByName('admin'), query, PagerFilter.getUnlimitedFilter())
def currTrainingCostValue = issue.getCustomFieldValue(TrainingCost)

results.getResults().each { currIssue ->
    issueTrainingCostValue = currIssue.getCustomFieldValue(TrainingCost)
    if (issueTrainingCostValue == null)
    	issueTrainingCostValue = 0.0
    log.warn(currIssue.key + " | Training Cost: " + issueTrainingCostValue)
    sumTrainingCost += issueTrainingCostValue as float
}
sumTrainingCost += currTrainingCostValue as float
excessValue = sumTrainingCost - maxTrainingCost // amount above 0 is exceeded amount
log.warn("Sum: " + excessValue)
excedeedAmount.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(excedeedAmount), excessValue), new DefaultIssueChangeHolder()) //update field
