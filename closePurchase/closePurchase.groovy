import java.sql.Timestamp
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.MultivaluedMap
import javax.servlet.http.HttpServletRequest
import groovy.transform.BaseScript
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.workflow.TransitionOptions
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.user.UserManager
import com.atlassian.sal.api.UrlMode

@BaseScript CustomEndpointDelegate delegate

def issueManager = ComponentAccessor.getIssueManager()
def projectManager = ComponentAccessor.getProjectManager()
def projectComponentManager = ComponentAccessor.getProjectComponentManager()
def remoteUserManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager)
def userManager = ComponentAccessor.getUserManager()
def issueService = ComponentAccessor.getIssueService()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def optionsManager = ComponentAccessor.getOptionsManager()
def issueFactory = ComponentAccessor.getIssueFactory()
def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def applicationProperties = ScriptRunnerImpl.getOsgiService(ApplicationProperties)
def baseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)

closePurchaseDialog(httpMethod: 'GET', groups: ['jira-core-users', 'jira-software-users', 'jira-servicedesk-users']) { MultivaluedMap queryParams ->
    def currIssue = issueManager.getIssueObject(queryParams.getFirst('issue') as Long)  
    def issue = issueManager.getIssueObject('ASSET-1')  // to load available options related to project
    def licenseTypeField = customFieldManager.getCustomFieldObject(11902) // License Type Custom Field
    def licenseTypeFieldConfig = licenseTypeField.getRelevantConfig(issue)   
    def licenseTypes = optionsManager.getOptions(licenseTypeFieldConfig)
    def licenseTypeOptions = ''
    for (licenseType in licenseTypes)
    	licenseTypeOptions = "${licenseTypeOptions}<aui-option>${licenseType}</aui-option>"
 	def dialog =
     """
      <section role="dialog" id="close-purchase-dialog" class="aui-layer aui-dialog2 aui-dialog2-medium" aria-hidden="true" data-aui-remove-on-hide="true">
          <header class="aui-dialog2-header">
            <h2 class="aui-dialog2-header-main">Close Purchase</h2>
            <a class="aui-dialog2-header-close">
              <span class="aui-icon aui-icon-small aui-iconfont-close-dialog" id="close-button">Close</span>
            </a>
          </header>
          <div class="aui-dialog2-content">
            <form class="aui" id='close-purchase-form'>
              
              <div class="field-group">
                <label for="create-asset-toggle">Create asset?</label>
				<aui-toggle label="toggle button" id="create-asset-toggle"></aui-toggle>
              </div>
              
              <div style="border-bottom: 1px solid #ddd; margin: 15px 0 15px 0"></div>
              
              <div class="field-group" id="summary-field-group" style="display: none">
              	<label for="summary-field">Summary<span class="aui-icon icon-required">(required)</span></label>
    			<input class="text" type="text" id="summary-field" name="Summary" value="${currIssue.summary}">
              </div>
              
              <div class="field-group" id="asset-type-field-group" style="display: none">
              		<label for='radio'>Asset type<span class="aui-icon icon-required">(required)</span></label>
              		<div class="radio">
            			<input class="radio" value="Device" type="radio" checked="checked" name="asset-type-radio" id="device-radio-field">
                    	<label for="device-radio-field">Device</label>
        			</div>
        			<div class="radio">
            			<input class="radio" value="License" type="radio" name="asset-type-radio" id="license-radio-field">
            			<label for="license-radio-field">License</label>
        			</div>
              </div>
              
              <div class="field-group" id="software-field-group" style="display: none">
              	<label for="software-field">Software<span class="aui-icon icon-required"></span></label>
    			<input class="text" type="text" length="30" id="software-field" name="Software" placeholder="Select a component/s" multiple=""/>
              </div>
             
              <div class="field-group" id="license-type-field-group" style="display: none">
              	 <label for="license-type-field">License type<span class="aui-icon icon-required">(required)</span></label>
                 <aui-select id="license-type-field" name="License Type" placeholder="Select a license type">
                 	${licenseTypeOptions}
                 </aui-select>
              </div>
              
              <div class="field-group" id="expire-time-field-group" style="display: none">
              	<label for="expire-time-field">Expire time<span class="aui-icon icon-required">(required)</span></label>
                <input class="aui-date-picker text" id="expire-time-field" name="Expire time" type="date"/>
              </div>
              
              <div class="field-group" id="service-tag-field-group" style="display: none">
              	<label for="service-tag-field">Service Tag<span class="aui-icon icon-required">(required)</span></label>
    			<input class="text" type="text" id="service-tag-field" name="Service Tag">
              </div>
              
              <div class="field-group" id="model-field-group" style="display: none">
              	<label for="model-field">Model<span class="aui-icon icon-required">(required)</span></label>
    			<input class="text" type="text" id="model-field" name="Model">
              </div>
              
              <div class="field-group" id="invoice-number-field-group" style="display: none">
              	<label for="invoice-number-field">Invoice number<span class="aui-icon icon-required">(required)</span></label>
    			<input class="text" type="text" id="invoice-number-field" name="Invoice number">
              </div>
              
              <div class="field-group" id="cost-field-group">
                <label for="cost-field">Cost [PLN]<span class="aui-icon icon-required">(required)</span></label>
    			<input class="text" id="cost-field" name="Cost" type="number" min="1" step="0.01">
              </div> 
              
              <div class="field-group" id="description-field-group" style="display: none">
            	<label for="description-field">Description</label>
            	<textarea class="textarea" name="Description" id="description-field" placeholder="Your description here..."></textarea>
        	 </div>
              
            </form>
            </div>
              <footer class="aui-dialog2-footer">
                <div class="aui-dialog2-footer-actions">
                	<input class="aui-button aui-button-primary submit" type="submit" value="Finish" id="create-button">
                    <button type="button" accesskey="`" title="Press Alt+` to cancel" class="aui-button aui-button-link cancel" resolved="" id="cancel-button">Cancel</button>     
                </div>
                <div class="aui-dialog2-footer-hint">
              		<p id="create-asset-paragraph" style="display: none">Choose this option if you want to create asset</p>
            		<p id="not-create-asset-paragraph">Choose this option if you do not want to create asset</p>
                </div>
              </footer>
        </section>
      """
    Response.ok().type(MediaType.TEXT_HTML).entity(dialog.toString()).header('header', 'value').build()
}

closePurchase(httpMethod: 'POST', groups: ['jira-core-users', 'jira-software-users', 'jira-servicedesk-users']) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
	def transitionId = 151 // id of transition "Close"
    def createAsset = queryParams.getFirst('createAsset') as Boolean
    def issue = issueManager.getIssueObject(queryParams.getFirst('issueKey') as String)
    def remoteUser = userManager.getUserByName(remoteUserManager.getRemoteUser(request)?.username as String)
    def message = ''
    if (createAsset) {
        def summaryParam = queryParams.getFirst('summary') as String
        def descriptionParam = queryParams.getFirst('description') as String
        def assetTypeParam = queryParams.getFirst('assetType')
    	def newIssue = issueFactory.getIssue()
        def assetProject = projectManager.getProjectByCurrentKey('ASSET')
    	newIssue.setSummary(summaryParam)
        newIssue.setDescription(descriptionParam)
    	newIssue.setProjectObject(assetProject)
        newIssue.reporter = remoteUser // user whose triggered dialog
        if (assetTypeParam == 'Device'){
            def serviceTagParam = queryParams.getFirst('serviceTag') as String
            def modelParam = queryParams.getFirst('model') as String
            def invoiceNumberParam = queryParams.getFirst('invoiceNumber') as String
            def serviceTagField = customFieldManager.getCustomFieldObject(11709) 
    		def modelField = customFieldManager.getCustomFieldObject(11503)
        	def invoiceNumberField = customFieldManager.getCustomFieldObject(11900)
            newIssue.issueTypeId = 11201 // 'Device'
        	newIssue.setCustomFieldValue(serviceTagField, serviceTagParam)
        	newIssue.setCustomFieldValue(modelField, modelParam)
        	newIssue.setCustomFieldValue(invoiceNumberField, invoiceNumberParam)
        }
        else {
            def softwareParam = queryParams.getFirst('software') as String
            def licenseTypeParam = queryParams.getFirst('licenseType') as String
            def expireTimeParam 
            //def softwareField = customFieldManager.getCustomFieldObject(11709)
    		def licenseTypeField = customFieldManager.getCustomFieldObject(11902)
            def expireTimeField = customFieldManager.getCustomFieldObject(11712)
            if (queryParams.getFirst('expireTime') != '')
                expireTimeParam = Timestamp.valueOf("${queryParams.getFirst('expireTime')} 00:00:00.000")
            log.warn(expireTimeParam)
            for (software in softwareParam.split(',')){
                def component = projectComponentManager.findByComponentName(assetProject.id, software)
                if (!component){
                    component = projectComponentManager.create(software, null, null, 1, assetProject.id)
                    log.warn("new component ${component['name']} added to component's list in ${assetProject.key}")
                }       	
                newIssue.setComponent(newIssue.getComponents() + component)
            }
            def availableLicenseTypes = optionsManager.getOptions(licenseTypeField.getRelevantConfig(newIssue))
			def licenseTypeValue = availableLicenseTypes.find { it.value == licenseTypeParam }
            newIssue.setCustomFieldValue(licenseTypeField, licenseTypeValue)  
            newIssue.setCustomFieldValue(expireTimeField, expireTimeParam)
            newIssue.issueTypeId = 11202
        }
        issueManager.createIssueObject(remoteUser, newIssue)
        issueLinkManager.createIssueLink(issue.id, newIssue.id, 10003, null, remoteUser)
        message = "Asset <a href='${baseUrl}/browse/${newIssue.key}' target='_blank'>${newIssue.key}</a> has been created and current issue has been closed</br>"
    }
    else {
        def costField = customFieldManager.getCustomFieldObject(11901)
        costField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(costField), queryParams.getFirst('cost') as Double), new DefaultIssueChangeHolder()) //update field
        message = "Issue <a href='${baseUrl}/browse/${issue.key}' target='_blank'>${issue.key}</a> has been closed</br>"
    }
    def transitionOptions = new TransitionOptions.Builder()
        .skipConditions()
        .skipPermissions()
        .skipValidators()
        .build()
    def transitionValidationResult = issueService.validateTransition(remoteUser, issue.id, transitionId, new IssueInputParametersImpl(), transitionOptions)
    if(transitionValidationResult.isValid()) // if this transition is valid, issue it
    	issueService.transition(remoteUser, transitionValidationResult)
    UserMessageUtil.success(message as String)
    return Response.ok([success: message]).build()
}

getSoftwareOptions(httpMethod: 'GET', groups: ['jira-core-users', 'jira-software-users', 'jira-servicedesk-users']) { MultivaluedMap queryParams, body ->
	def project = issueManager.getIssueObject('ASSET-1').getProjectObject()  // to load available options related to project
    def components = project.getComponents()['name']
    return Response.ok(components).build()
}
