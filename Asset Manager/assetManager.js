(function($) {
    $(function() {
        AJS.dialog2.on('show', function(event) {
            if (event.target.id === 'close-purchase-dialog') {
                new AJS.DatePicker($(event.target).find('#expire-time-field')[0], {'overrideBrowserDefault': true})
                createSelectField('/rest/scriptrunner/latest/custom/getActiveUsers?accessToken=', '#user-field', false, false, true, true, 'user', 'small')
                createSelectField('/rest/scriptrunner/latest/custom/getProjectComponents?type=license&projectKey=ASSET', '#software-field' ,true, true, false, false)
                createSelectField('/rest/scriptrunner/latest/custom/getCustomFieldOptionsById?id=11509&contextIssue=ASSET-1', '#place-field' ,false, false, true, false)
                createSelectField('/rest/scriptrunner/latest/custom/getCustomFieldOptionsById?id=11902&contextIssue=ASSET-1', '#license-type-field' ,false, false,false, false)

                $(event.target).find("#close-button").click(function(e) {
                    closeDialog(e, event)
                });

                $(event.target).find("#cancel-button").click(function(e) {
                    closeDialog(e, event)
                });

                $(event.target).find('#create-asset-toggle').change(function() {
                    let toggleIsChecked = $(event.target).find('#create-asset-toggle').prop('checked')
                    purchaseFormGlobalFields(event, toggleIsChecked)
                    if (toggleIsChecked) {
                        $(event.target).find("#device-radio-button").prop("checked", true)
                        purchaseFormDeviceFields(event, 'show')
                        $(event.target).find('#asset-type-field-group').change(function() {
                            if ($(event.target).find('#license-radio-field').prop('checked')) {
                                $(event.target).find('#license-type-field input').prop('required', true) // other required fields are managed in .groovy, exepct select2 fields
                                purchaseFormLicenseFields(event, 'show')
                                purchaseFormDeviceFields(event, 'hide')
                            }
                            else if ($(event.target).find('#device-radio-field').prop('checked')) {
                                purchaseFormLicenseFields(event, 'hide')
                                purchaseFormDeviceFields(event, 'show')
                            }
                        })
                    }
                    else {
                        purchaseFormDeviceFields(event, 'hide')
                        purchaseFormLicenseFields(event, 'hide')
                    }
                });
                $(event.target).find("#create-button").click(function(e) {
                    let requiredFieldsFilled = checkRequiredFieldsInForm(event, '#close-purchase-form')
                    let toggleChecked = $(event.target).find('#create-asset-toggle').prop('checked');
                    if (!requiredFieldsFilled)
                        showErrorMsg('Fill the required fields')
                    else if (toggleChecked && requiredFieldsFilled) {
                        let licenseList = getSelectData(event, '#software-field', 'text', true)
                        let userSelected = getSelectData(event, '#user-field', 'id', false)
                        let placeSelected = getSelectData(event, '#place-field', 'text', false)
                        if (placeSelected !== null && userSelected !== null)
                            showErrorMsg('User field and Place field can not be filled in during one execution')
                        else {
                            sendPostRequest(event,
                                "/rest/scriptrunner/latest/custom/closePurchase" +
                                '?createAsset=' 	+ toggleChecked +
                                '&summary=' 		+ $(event.target).find('#summary-field').val() +
                                '&assetType='		+ $(event.target).find('input[name=asset-type-radio]').filter(":checked").val() +
                                '&serialNumber=' 	+ $(event.target).find('#serial-number-field').val() +
                                '&model=' 		  	+ $(event.target).find('#model-field').val() +
                                '&invoiceNumber=' 	+ $(event.target).find('#invoice-number-field').val() +
                                '&software='		+ licenseList +
                                '&user='			+ userSelected +
                                '&place='           + placeSelected +
                                "&cost=" 			+ $(event.target).find("#cost-field2").val() +
                                '&licenseType='		+ $(event.target).find('#license-type-field').val() +
                                '&expireTime='		+ $(event.target).find('#expire-time-field').val() +
                                '&description='		+ $(event.target).find('#description-field').val() +
                                '&issueKey=' 	  	+ JIRA.Issue.getIssueKey()
                            )
                        }
                    }
                    else if (!toggleChecked && requiredFieldsFilled){
                        sendPostRequest(event,
                            "/rest/scriptrunner/latest/custom/closePurchase" +
                            "?createAsset=" 	+ toggleChecked +
                            "&cost=" 			+ $(event.target).find("#cost-field").val() +
                            "&issueKey=" 		+ JIRA.Issue.getIssueKey()
                        )
                    }
                });
            }
            else if (event.target.id === 'close-grant-access-dialog') {
                createSelectField('/rest/scriptrunner/latest/custom/getActiveUsers?accessToken=', '#user-field' ,false, true, false,true, 'user', 'small')
                createSelectField('/rest/scriptrunner/latest/custom/getProjectComponents?type=access&projectKey=ASSET', '#environment-field' ,true, true, false,false)
                $(event.target).find("#close-button").click(function(e) {
                    closeDialog(e, event)
                });
                $(event.target).find("#cancel-button").click(function(e) {
                    closeDialog(e, event)
                });
                $(event.target).find('#create-asset-toggle').change(function() {
                    let toggleIsChecked = $(event.target).find('#create-asset-toggle').prop('checked')
                    if (toggleIsChecked)
                        grantAccessFormGlobalFields(event, toggleIsChecked)
                    else
                        grantAccessFormGlobalFields(event, toggleIsChecked)
                });
                $(event.target).find("#create-button").click(function() {
                    let toggleChecked = $(event.target).find('#create-asset-toggle').prop('checked');
                    let requiredFieldsFilled = checkRequiredFieldsInForm(event, '#close-grant-access-form')
                    if (!requiredFieldsFilled)
                        showErrorMsg('Fill the required fields')
                    else if (toggleChecked && requiredFieldsFilled) {
                        let userList = getSelectData(event, '#user-field', 'id', true)
                        let accessList = getSelectData(event, '#environment-field', 'id', true)
                        sendPostRequest(event,
                            '/rest/scriptrunner/latest/custom/closeGrantAccess' +
                            '?createAsset=' 	+ toggleChecked +
                            '&summary=' 		+ $(event.target).find('#summary-field').val() +
                            '&user=' 			+ userList +
                            '&environment=' 	+ accessList +
                            '&description='	    + $(event.target).find('#description-field').val() +
                            '&issueKey=' 	  	+ JIRA.Issue.getIssueKey())
                    }
                    else if (!toggleChecked && requiredFieldsFilled) {
                        sendPostRequest(event,
                            "/rest/scriptrunner/latest/custom/closeGrantAccess" +
                            '?createAsset=' 	+ toggleChecked +
                            '&issueKey=' 	  	+ JIRA.Issue.getIssueKey())
                    }
                });
            }
            else if (event.target.id === 'close-remove-access-dialog') {
                createSelectField('/rest/scriptrunner/latest/custom/getActiveUsers?accessToken=', '#user-field', false, true, true, true, 'user', 'small' )
                makeFieldPicker(AJS.$("#issue-field"), [], 'issue', 'xsmall', true);
                $(event.target).find("#close-button").click(function(e) {
                    closeDialog(e, event)
                });
                $(event.target).find("#cancel-button").click(function(e) {
                    closeDialog(e, event)
                });
                $(event.target).find("#user-field-search").click(function() {
                    let userList = getSelectData(event, '#user-field', 'id', true)
                    if (userList.length === 0)
                        showErrorMsg('User/s field is empty')
                    else {
                        let accessOptions = createSelectField('/rest/scriptrunner/latest/custom/getPersonAccesses?username=' + userList + '&accessToken=', '#issue-field', false, true,true,true,  'issue', 'xsmall' )
                        if (accessOptions.length === 0)
                            showErrorMsg('Not found any access assigned to selected user/s')
                        else
                            showSuccessMsg('Found accesses for selected user/s')
                    }
                })
                $(event.target).find("#create-button").click(function() {
                    let accessList = getSelectData(event, '#issue-field', 'id', true)
                    if (accessList.length !== 0) {
                        sendPostRequest(event,
                            '/rest/scriptrunner/latest/custom/closeRemoveAccess' +
                            '?accessIssue='     + accessList +
                            '&issueKey=' 	  	+ JIRA.Issue.getIssueKey()
                        )
                    }
                    else {
                        sendPostRequest(event,
                            '/rest/scriptrunner/latest/custom/closeRemoveAccess' +
                            '?onlyClose='       + true +
                            '&issueKey='        + JIRA.Issue.getIssueKey()
                        )
                    }
                });
            }
            else if (event.target.id === 'close-beginning-of-cooperation-dialog') {
                createSelectField('/rest/scriptrunner/latest/custom/getActiveUsers?accessToken=', '#user-field', false, false, false,true, 'user', 'small')
                createSelectField('/rest/scriptrunner/latest/custom/getDevicesOnStock?accessToken=', '#device-field', false, true, true, true, 'issue', 'xsmall')
                createSelectField('/rest/scriptrunner/latest/custom/getProjectComponents?type=access&projectKey=ASSET', '#access-field', true, true, true, false)
                $(event.target).find("#close-button").click(function(e) {
                    closeDialog(e, event)
                });
                $(event.target).find("#cancel-button").click(function(e) {
                    closeDialog(e, event)
                });
                $(event.target).find("#create-button").click(function() {
                    let requiredFieldsFilled = checkRequiredFieldsInForm(event, '#close-beginning-of-cooperation-form')
                    if (!requiredFieldsFilled)
                        showErrorMsg('Fill the required fields')
                    else {
                        let userSelected = getSelectData(event, '#user-field', 'id', false)
                        sendGetRequestCustomBeginningOfCooperation(event,
                            '/rest/scriptrunner/latest/custom/getPersonAsset' +
                            '?username=', userSelected
                        )
                    }
                });
            }
            else if (event.target.id === 'create-asset-dialog') {
                console.log('TO DO')
            }
        });
    });
    function makeFieldPicker($elem, data, type, size, multiple, allowClear) {
        let minInputLength
        if (type === 'user')
            minInputLength = 2
        else
            minInputLength = 0
        $elem.auiSelect2({
            hasAvatar: true,
            formatResult: function (result) {
                return formatWithAvatar({
                    size: size,
                    type: type,
                    person: {
                        displayName: result.text,
                        name: result.id,
                        avatarUrl: result.imgSrc
                    }
                });
            },
            formatSelection: function(result) {
                return formatWithAvatar({
                    size: 'xsmall',
                    person: {
                        displayName: result.text,
                        name: result.id,
                        avatarUrl: result.imgSrc
                    }
                });
            },
            query: function (query) {
                let results = [];
                for (let i = 0, ii = data.length; i < ii; i++) {
                    let result = data[i];
                    if (result.text.toLowerCase().indexOf(query.term.toLowerCase()) > -1) {
                        results.push(result);
                    }
                }
                query.callback({results: results});
            },
            multiple: multiple,
            minimumInputLength: minInputLength,
            allowClear: allowClear
        });
    }
    function formatWithAvatar(opt_data) {
        let personName = opt_data.person && opt_data.person.displayName ? opt_data.person.displayName : opt_data.person && opt_data.person.name ? opt_data.person.name : opt_data.unknownName; //if user
        return '<span class="' + opt_data.type + '">' + aui.avatar.avatar({
            size: opt_data.size,
            avatarImageUrl: opt_data.person.avatarUrl
        }) + AJS.escapeHtml(personName) + '</span>';
    }
    function purchaseFormDeviceFields (event, operation){
        switch(operation){
            case 'hide':
                $(event.target).find('#place-field-group').hide()
                $(event.target).find('#serial-number-field-group').hide()
                $(event.target).find('#model-field-group').hide()
                break
            case 'show':
                $(event.target).find('#place-field-group').show()
                $(event.target).find('#serial-number-field-group').show()
                $(event.target).find('#model-field-group').show()
                break
            default:
                console.error('not found oper')
        }
    }
    function purchaseFormLicenseFields (event, operation){
        switch (operation){
            case 'hide':
                $(event.target).find('#software-field-group').hide()
                $(event.target).find('#expire-time-field-group').hide()
                $(event.target).find('#license-type-field-group').hide()
                $(event.target).find('#expire-time-field-group').hide()
                break
            case 'show':
                $(event.target).find('#software-field-group').show()
                $(event.target).find('#license-type-field-group').show()
                $(event.target).find('#license-type-field').change(function() {
                    if ($(event.target).find('#license-type-field').val() === 'Subscription')
                        $(event.target).find('#expire-time-field-group').show()
                    else
                        $(event.target).find('#expire-time-field-group').hide()
                })
                break
            default:
                console.error('not found oper')
        }
    }
    function purchaseFormGlobalFields(event, toggleIsChecked){
        switch (toggleIsChecked){
            case false:
                $(event.target).find('#cost-field-group').show()
                $(event.target).find('#not-create-asset-paragraph').css('display', 'inline')
                $(event.target).find('#asset-type-field-group').hide()
                $(event.target).find('#summary-field-group').hide()
                $(event.target).find('#invoice-number-field-group').hide()
                $(event.target).find('#cost-field2-group').hide()
                $(event.target).find('#description-field-group').hide()
                $(event.target).find('#create-asset-paragraph').css('display', 'none')
                $(event.target).find('#user-field-group').hide()
                break
            case true:
                $(event.target).find('#cost-field-group').hide()
                $(event.target).find('#not-create-asset-paragraph').css('display', 'none')
                $(event.target).find('#asset-type-field-group').show()
                $(event.target).find('#summary-field-group').show()
                $(event.target).find('#invoice-number-field-group').show()
                $(event.target).find('#cost-field2-group').show()
                $(event.target).find('#description-field-group').show()
                $(event.target).find('#create-asset-paragraph').css('display', 'inline')
                $(event.target).find("#device-radio-button").prop("checked", true)
                $(event.target).find('#user-field-group').show()
                break
            default:
                console.error('provided value should be boolean')
        }
    }
    function grantAccessFormGlobalFields (event, toggleIsChecked){
        switch(toggleIsChecked){
            case false:
                $(event.target).find('#summary-field-group').hide()
                $(event.target).find('#user-field-group').hide()
                $(event.target).find('#environment-field-group').hide()
                $(event.target).find('#description-field-group').hide()
                $(event.target).find('#not-create-asset-paragraph').css('display', 'inline')
                $(event.target).find('#create-asset-paragraph').css('display', 'none')
                break
            case true:
                $(event.target).find('#summary-field-group').show()
                $(event.target).find('#user-field-group').show()
                $(event.target).find('#environment-field-group').show()
                $(event.target).find('#description-field-group').show()
                $(event.target).find('#not-create-asset-paragraph').css('display', 'none')
                $(event.target).find('#create-asset-paragraph').css('display', 'inline')
                break
            default:
                log.error('provided value should be boolean')
        }
    }
    function loading(event, operation) {
        switch (operation){
            case 'hide':
                $(event.target).css('display', 'block')
                break
            case 'show':
                $(event.target).css('display', 'none')
                break
            default:
                console.error('not found oper')
        }
    }
    function createSelectField(url, htmlTag, dynamic, multiple, allowClear, fieldPicker, type, size){ //type and size to fill only if fieldPicker=true
        let dataList
        AJS.$.ajax({
            url: url,
            type: 'GET',
            datatype: 'json',
            async: false,
            success: function(data){dataList = data},
            error: function() {
                showErrorMsg('Necessary endpoint could not be correctly accessed. Check console logs or contact your Jira Administrator')
            }
        })
        if (fieldPicker){
            AJS.$(".aui-select2 select").auiSelect2()
            makeFieldPicker(AJS.$(htmlTag), dataList, type, size, multiple, allowClear);
        }
        else {
            if (dynamic){
                AJS.$(htmlTag).auiSelect2({
                    tags: dataList,
                    multiple: multiple,
                    allowClear: allowClear,
                    tokenSeparators: [','],
                    createTag: function (tag) {
                        return {
                            id: tag.term + ' (New component)',
                            text: tag.term + ' (New component)',
                            newOption: true
                        }
                    }
                })
            }
            else {
                AJS.$(htmlTag).auiSelect2({
                    data: dataList,
                    multiple: multiple,
                    allowClear: allowClear
                })
            }
        }
        return dataList
    }
    function closeDialog(e, event){
        e.preventDefault();
        AJS.dialog2(event.target).hide();
        AJS.dialog2(event.target).remove();
    }
    function closeDialogAlt(event){
        JIRA.trigger(JIRA.Events.REFRESH_ISSUE_PAGE, [JIRA.Issue.getIssueId()]);
        AJS.dialog2(event.target).hide();
        AJS.dialog2(event.target).remove();
    }
    function checkRequiredFieldsInForm(event, htmlTag){
        let fieldsFilled = true
        $(event.target).find(htmlTag + ' input').each(function (){
            if ($(this).is(':visible') && this.value === '' && $(this).prop('required') && !(this.id).contains('s2id_autogen')){
                console.error(this.id + ' is empty')
                fieldsFilled = false
            }
        })
        return fieldsFilled
    }
    function getSelectData(event, htmlTag, dataType, list) {
        let dataSelected = $(event.target).find(htmlTag).select2('data')
        try {
            if (list){
                let dataList = []
                if (dataType === 'id'){
                    dataSelected.forEach(function(elem){
                        dataList.push(elem.id)
                    })
                }
                else if (dataType === 'text'){
                    dataSelected.forEach(function(elem){
                        dataList.push(elem.text)
                    })
                }
                return dataList
            }
            else
            if (dataType === 'id')
                return dataSelected.id
            else if (dataType === 'text')
                return dataSelected.text
        }
        catch (error) {
            if (error instanceof TypeError)
                return null
        }
    }
    function showErrorMsg(body) {
        AJS.flag({
            type: 'error',
            body: body,
            close: 'auto'
        });
    }
    function showSuccessMsg(body) {
        AJS.flag({
            type: 'success',
            body: body,
            close: 'auto'
        });
    }
    function sendPostRequest(event, url){
        AJS.$.ajax({
            url: url,
            type: 'POST',
            dataType: 'json',
            contentType: 'application/json',
            async: false,
            success: function() {
                closeDialogAlt(event)
            },
            error: function() {
                showErrorMsg('Necessary endpoint could not be correctly accessed. Check console logs or contact your Jira Administrator')
            }
        });
    }
    function sendGetRequestCustomBeginningOfCooperation(event, url, userSelected) { // for beginning of cooperation usage
        AJS.$.ajax({
            url: url + userSelected,
            type: 'GET',
            dataType: 'json',
            async: false,
            success: function() {
                let deviceList = getSelectData(event, '#device-field', 'id', true)
                let accessList = getSelectData(event, '#access-field', 'id', true)
                sendPostRequest(event,
                    '/rest/scriptrunner/latest/custom/closeBeginningOfCooperation' +
                    '?user='            + userSelected +
                    '&device='          + deviceList +
                    '&access='          + accessList +
                    '&issueKey=' 	  	+ JIRA.Issue.getIssueKey()
                )
            },
            error: function(response) {
                showErrorMsg('Person <strong>' + response.responseText + '</strong> for selected user already exists.' +
                    '<ul class="aui-nav-actions-list">' +
                    '<li><a class="aui-button aui-button-link" target="_blank" href="https://jira.redge.com/browse/' + response.responseText + '">View issue</a></li></ul>'
                )
            }
        });
    }
})(AJS.$);