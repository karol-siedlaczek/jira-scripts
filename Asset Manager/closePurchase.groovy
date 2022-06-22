// Copyright 2022 Redge Technologies
// Author: K. Siedlaczek

import java.sql.Timestamp
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.MultivaluedMap
import javax.servlet.http.HttpServletRequest
import groovy.transform.BaseScript
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.user.UserManager
import com.atlassian.sal.api.UrlMode
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.label.Label
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.bc.project.component.MutableProjectComponent
import com.atlassian.jira.workflow.TransitionOptions
import com.atlassian.jira.config.IssueTypeManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser

@BaseScript CustomEndpointDelegate delegate

closePurchaseDialog(httpMethod: 'GET', groups: ['jira-core-users', 'jira-software-users', 'jira-servicedesk-users']) { MultivaluedMap queryParams ->
    def issueManager = ComponentAccessor.getIssueManager()

    def issue = issueManager.getIssueObject(queryParams.getFirst('issue') as Long)
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
        <div class='field-group' id="user-field-group" style="display: none">
          <label for="user-field">User</label>
          <input class="text medium-long-field aui-select2" type="text" length="60" id="user-field" name="User" placeholder="Select a user" data-allow-clear=true/>
          <div class="description">Fill to auto assign asset to selected user</div>
        </div>
        <div class='field-group' id="place-field-group" style="display: none">
          <label for="place-field">Place</label>
          <input class="text medium-long-field aui-select2" type="text" length="60" id="place-field" name="Place" placeholder="Select a place" data-allow-clear=true/>
          <div class="description">Fill to auto assign asset to selected collocation</div>
        </div>
        <div class="field-group" id="software-field-group" style="display: none">
          <label for="software-field">Software<span class="aui-icon icon-required"></span></label>
          <input class="text medium-long-field" type="text" length="30" id="software-field" name="Software" placeholder="Select a component/s" multiple="" required/>
        </div>
        <div class="field-group" id="license-type-field-group" style="display: none">
          <label for="license-type-field">License type<span class="aui-icon icon-required"></span></label>
          <input class="text medium-long-field aui-select2" type="text" length="60" id="license-type-field" name="License Type" placeholder="Select a license type"/>
        </div>
        <div class="field-group" id="expire-time-field-group" style="display: none">
          <label for="expire-time-field">Expire time</label>
          <input class="aui-date-picker text medium-long-field" id="expire-time-field" name="Expire time" type="date"/>
        </div>
        <div class="field-group" id="model-field-group" style="display: none">
          <label for="model-field">Model<span class="aui-icon icon-required"></span></label>
          <input class="text medium-long-field" type="text" id="model-field" name="Model" required/>
        </div>
        <div class="field-group" id="serial-number-field-group" style="display: none">
          <label for="serial-number-field">Serial number<span class="aui-icon icon-required"></span></label>
          <input class="text medium-long-field" type="text" id="serial-number-field" name="Serial Number" required/>
        </div>
        <div class="field-group" id="invoice-number-field-group" style="display: none">
          <label for="invoice-number-field">Invoice number</label>
          <input class="text medium-long-field" type="text" id="invoice-number-field" name="Invoice number"/>
        </div>
        <div class="field-group" id="amount-field-group" style="display: none">
          <label for="amount-field">Amount<span class="aui-icon icon-required"></span></label>
          <input class="text medium-long-field" id="amount-field" name="Amount" type="number" min="1" value="1" required/>
        </div>
        <div class="field-group" id="cost-field-group">
          <label for="cost-field">Cost [PLN]</label>
          <input class="text medium-long-field" id="cost-field" name="Cost" type="number" min="1" step="0.01"/>
        </div>
        <div class="field-group" id="cost-field2-group" style="display: none">
          <label for="cost-field">Cost [PLN]</label>
          <input class="text medium-long-field" id="cost-field2" name="Cost" type="number" min="1" step="0.01"/>
        </div>
        <div class="field-group" id="description-field-group" style="display: none">
          <label for="description-field">Description</label>
          <textarea class="textarea medium-long-field" name="Description" id="description-field" placeholder="Your description here..."></textarea>
        </div>
      </form>
    </div>
    <footer class="aui-dialog2-footer">
      <div class="aui-dialog2-footer-actions">
        <aui-spinner id="custom-dialog-spinner" size="small" style="display: none"></aui-spinner>
        <button class="aui-button aui-button-primary submit" type="submit" id="create-button">Finish</button>
        <button type="button" accesskey="`" title="Press Alt+` to cancel" class="aui-button aui-button-link cancel" resolved="" id="cancel-button">Cancel</button>
      </div>
      <div class="aui-dialog2-footer-hint">
        <p id="create-asset-paragraph" style="display: none">Choose this option if you want to create an asset</p>
        <p id="not-create-asset-paragraph" style="display: inline">Choose this option if you do not want to create an asset</p>
      </div>
    </footer>
  </section>
  """
    Response.ok().type(MediaType.TEXT_HTML).entity(dialog.toString()).header('header', 'value').build()
}

closePurchase(httpMethod: 'POST', groups: ['jira-core-users', 'jira-software-users', 'jira-servicedesk-users']) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
    def issueManager = ComponentAccessor.getIssueManager()
    def userManager = ComponentAccessor.getUserManager()
    def remoteUserManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager)
    def applicationProperties = ScriptRunnerImpl.getOsgiService(ApplicationProperties)

    def CLOSE_PURCHASE_TRANSITION_ID = 151 // id of transition "Close"

    def assetCreation = (queryParams.getFirst('createAsset') as String).toBoolean()
    def issue = issueManager.getIssueObject(queryParams.getFirst('issueKey') as String)
    def remoteUser = userManager.getUserByName(remoteUserManager.getRemoteUser(request)?.username as String)
    def baseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)
    def message = ''

    if (assetCreation) {
        def user = userManager.getUserByName(queryParams.getFirst('user') as String)
        def amount = queryParams.getFirst('amount') as Integer
        for (def i = 0; i < amount; i++) {
            Issue assetIssue = createAsset(user, remoteUser, issue, queryParams, issueManager)
            message = message + "Asset <a href='${baseUrl}/browse/${assetIssue.key}' target='_blank'>${assetIssue.key}</a> has been created</br>"
        }
    }
    else {
        if (queryParams.getFirst('cost') != '')
            updateCostField(issue, queryParams.getFirst('cost') as Double)
    }
    message = message + "Issue <a href='${baseUrl}/browse/${issue.key}' target='_blank'>${issue.key}</a> has been closed</br>"
    transistIssue(remoteUser, issue, CLOSE_PURCHASE_TRANSITION_ID) //close curr issue
    UserMessageUtil.success(message as String)
    return Response.ok([success: message]).build()
}

Issue createAsset(ApplicationUser user,
                  ApplicationUser remoteUser,
                  Issue issue,
                  MultivaluedMap queryParams,
                  IssueManager issueManager){
    def projectManager = ComponentAccessor.getProjectManager()
    def projectComponentManager = ComponentAccessor.getProjectComponentManager()
    def customFieldManager = ComponentAccessor.getCustomFieldManager()
    def issueFactory = ComponentAccessor.getIssueFactory()
    def issueTypeManager = ComponentAccessor.getComponent(IssueTypeManager)
    def issueLinkManager = ComponentAccessor.getIssueLinkManager()
    def optionsManager = ComponentAccessor.getOptionsManager()

    def USER_FIELD = customFieldManager.getCustomFieldObject(11701)
    def PLACE_FIELD = customFieldManager.getCustomFieldObject(11509)
    def COST_FIELD = customFieldManager.getCustomFieldObject(11901)
    def SERIAL_NUMBER_FIELD = customFieldManager.getCustomFieldObject(11504)
    def MODEL_FIELD = customFieldManager.getCustomFieldObject(11503)
    def INVOICE_NUMBER_FIELD = customFieldManager.getCustomFieldObject(11900)
    def LICENSE_TYPE_FIELD = customFieldManager.getCustomFieldObject(11902)
    def EXPIRE_TIME_FIELD = customFieldManager.getCustomFieldObject(11712)
    def DEVICE_ISSUE_TYPE = issueTypeManager.getIssueType('11201')
    def LICENSE_ISSUE_TYPE = issueTypeManager.getIssueType('11202')
    def ASSET_PROJECT = projectManager.getProjectByCurrentKey('ASSET')

    def newIssue = issueFactory.getIssue()
    newIssue.setDescription(queryParams.getFirst('description') as String)
    newIssue.setProjectObject(ASSET_PROJECT)
    if (queryParams.getFirst('cost') != '')
        newIssue.setCustomFieldValue(COST_FIELD, queryParams.getFirst('cost') as Double)
    newIssue.setCustomFieldValue(INVOICE_NUMBER_FIELD, queryParams.getFirst('invoiceNumber') as String)
    newIssue.reporter = remoteUser // user whose triggered dialog
    if (queryParams.getFirst('assetType') == 'Device'){
        newIssue.setSummary(queryParams.getFirst('model') as String)
        newIssue.setIssueType(DEVICE_ISSUE_TYPE)
        newIssue.setCustomFieldValue(SERIAL_NUMBER_FIELD, queryParams.getFirst('serialNumber') as String)
        newIssue.setCustomFieldValue(MODEL_FIELD, queryParams.getFirst('model') as String)
        newIssue.setCustomFieldValue(USER_FIELD, user)
        if (queryParams.getFirst('place') != '') {
            def availablePlaces = optionsManager.getOptions(PLACE_FIELD.getRelevantConfig(newIssue))
            def licenseTypeValue = availablePlaces.find { it.value == queryParams.getFirst('place') as String }
            newIssue.setCustomFieldValue(PLACE_FIELD, licenseTypeValue)
        }
        if (((queryParams.getFirst('model') as String).toLowerCase()).contains('monitor')) // at least try
            newIssue.setLabels([new Label(null, null, 'monitor')] as Set)
        else if ((((queryParams.getFirst('model') as String).toLowerCase()).contains('laptop')) || (((queryParams.getFirst('model') as String).toLowerCase()).contains('macbook')))
            newIssue.setLabels([new Label(null, null, 'laptop')] as Set)
        else if ((((queryParams.getFirst('model') as String).toLowerCase()).contains('galaxy')) || (((queryParams.getFirst('model') as String).toLowerCase()).contains('iphone')))
            newIssue.setLabels([new Label(null, null, 'phone')] as Set)
    }
    else if (queryParams.getFirst('assetType') == 'License') {
        def expireTimeValue = ''
        if (queryParams.getFirst('expireTime') != ''){
            expireTimeValue = Timestamp.valueOf("${queryParams.getFirst('expireTime')} 00:00:00.000")
            newIssue.setCustomFieldValue(EXPIRE_TIME_FIELD, expireTimeValue)
        }
        for (software in (queryParams.getFirst('software') as String).split(',')){
            def component = projectComponentManager.findByComponentName(ASSET_PROJECT.id, software)
            if (!component){
                component = projectComponentManager.create(software, 'license', null, 1, ASSET_PROJECT.id)
                log.warn("component ${component['name']} added to component's list in ${ASSET_PROJECT.key}")
            }
            else if (!(component.getDescription()).contains('license')){ // add to desc string to declare component as access
                def mutableComponent = MutableProjectComponent.copy(component)
                mutableComponent.setDescription("${component.getDescription()} license")
                component = projectComponentManager.update(mutableComponent)
                log.warn("component ${component['name']} already exists, only appended 'license' to description")
            }
            newIssue.setComponent(newIssue.getComponents() + component)
        }
        def availableLicenseTypes = optionsManager.getOptions(LICENSE_TYPE_FIELD.getRelevantConfig(newIssue))
        def licenseTypeValue = availableLicenseTypes.find { it.value == queryParams.getFirst('licenseType') as String }
        newIssue.setCustomFieldValue(LICENSE_TYPE_FIELD, licenseTypeValue)
        newIssue.setCustomFieldValue(USER_FIELD, user)
        newIssue.setIssueType(LICENSE_ISSUE_TYPE)
    }
    else
        log.error('not found provided asset type')
    Issue assetIssue = issueManager.createIssueObject(remoteUser, newIssue)
    issueLinkManager.createIssueLink(issue.id, newIssue.id, 10003, null, remoteUser)
    return assetIssue
}

void updateCostField(Issue issue, Double costValue){
    def customFieldManager = ComponentAccessor.getCustomFieldManager()

    def COST_FIELD = customFieldManager.getCustomFieldObject(11901)

    COST_FIELD.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(COST_FIELD), costValue), new DefaultIssueChangeHolder()) //update field
}

void transistIssue(ApplicationUser user, Issue issue, Integer transitionId) {
    def issueService = ComponentAccessor.getIssueService()
    def transitionOptions = new TransitionOptions.Builder().skipConditions().skipPermissions().skipValidators().build()

    def transitionValidationResult = issueService.validateTransition(user, issue.id, transitionId, new IssueInputParametersImpl(), transitionOptions)
    if (transitionValidationResult.isValid())
        issueService.transition(user, transitionValidationResult)
}