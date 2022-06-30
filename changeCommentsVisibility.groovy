import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.web.bean.PagerFilter

def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchService = ComponentAccessor.getComponent(SearchService)
def commentManager = ComponentAccessor.getCommentManager()
def userManager = ComponentAccessor.getUserManager()

def projects = ['project_key_1', 'project_key_2']
def admin = userManager.getUserByName('user_admin')

projects.each { project ->
    def query = jqlQueryParser.parseQuery("project = $project")
    def results = searchService.search(admin, query, PagerFilter.getUnlimitedFilter())
    log.warn("${project}:")
    results.getResults().each { issue ->
        def comments = commentManager.getComments(issue)
        if (comments.size() > 0){
            log.warn("  ${issue.key}:")
        	comments.each{ comment ->
        		def mutableComment = commentManager.getMutableComment(comment.id)
            	mutableComment.setRoleLevelId(10300)
            	commentManager.update(mutableComment, false)
            	log.warn("    comment ${comment.id} visibility changed")
        	}
        }
    }
}
