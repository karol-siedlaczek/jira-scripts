(function($) {
    $(function() {
        AJS.dialog2.on('show', function(event) {
            if (event.target.id === 'close-purchase-dialog') {
                new AJS.DatePicker($(event.target).find('#expire-time-field')[0], {'overrideBrowserDefault': true})
                createSelectField('/rest/scriptrunner/latest/custom/getActiveUsers?accessToken=dupa', '#user-field', false, true, false)
                createSelectField('/rest/scriptrunner/latest/custom/getProjectComponents?type=license&projectKey=ASSET', '#software-field' ,true, false, false)
                createSelectField('/rest/scriptrunner/latest/custom/getCustomFieldOptionsById?id=11509&contextIssue=ASSET-1', '#place-field' ,false, false, false)
                createSelectField('/rest/scriptrunner/latest/custom/getCustomFieldOptionsById?id=11902&contextIssue=ASSET-1', '#license-type-field' ,false, false, false)

                $(event.target).find("#close-button").click(function(e) {
                    closeDialog(e, event)
                });

                $(event.target).find("#cancel-button").click(function(e) {
                    closeDialog(e, event)
                });

                $(event.target).find('#create-asset-toggle').change(function() {
                    let toggleIsChecked = $(event.target).find('#create-asset-toggle').prop('checked')
                    globalFields(event, toggleIsChecked)
                    if (toggleIsChecked) {
                        $(event.target).find("#device-radio-button").prop("checked", true)
                        deviceFields(event, 'show')
                        $(event.target).find('#asset-type-field-group').change(function() {
                            if ($(event.target).find('#license-radio-field').prop('checked')) {
                                $(event.target).find('#license-type-field input').prop('required', true) // other required fields are managed in .groovy, exepct select2 fields
                                licenseFields(event, 'show')
                                deviceFields(event, 'hide')
                            }
                            else if ($(event.target).find('#device-radio-field').prop('checked')) {
                                licenseFields(event, 'hide')
                                deviceFields(event, 'show')
                            }
                        })
                    }
                    else {
                        deviceFields(event, 'hide')
                        licenseFields(event, 'hide')
                    }
                });

                $(event.target).find("#create-button").click(function(e) {
                    let requiredFieldsFilled = checkRequiredFields(event, '#close-purchase-form')
                    let toggleChecked = $(event.target).find('#create-asset-toggle').prop('checked');
                    if (!requiredFieldsFilled)
                        showErrorMsg('Fill the required fields')
                    else if (toggleChecked && requiredFieldsFilled) {
                        let licenseList = getSelectData(event, '#software-field', 'text', true)
                        let userSelected = getSelectData(event, '#user-field', 'id', false)
                        let placeSelected = getSelectData(event, '#place-field', 'text', false)
                        if (placeSelected !== '' && userSelected !== ''){
                            showErrorMsg('User field and Place field can not be filled in during one execution')
                        }
                        else {
                            AJS.$.ajax({
                                url: "/rest/scriptrunner/latest/custom/closePurchase" +
                                    '?createAsset=' 	+ toggleChecked +
                                    '&summary=' 		+ $(event.target).find('#summary-field').val() +
                                    '&assetType='		+ $(event.target).find('input[name=asset-type-radio]').filter(":checked").val() +
                                    '&serialNumber=' 	+ $(event.target).find('#serial-number-field').val() +
                                    '&model=' 		  	+ $(event.target).find('#model-field').val() +
                                    '&invoiceNumber=' 	+ $(event.target).find('#invoice-number-field').val() +
                                    '&software='		+ licenseList +
                                    '&user='			+ userSelected +
                                    '&place='           + placeSelected +
                                    '&licenseType='		+ $(event.target).find('#license-type-field').val() +
                                    '&expireTime='		+ $(event.target).find('#expire-time-field').val() +
                                    '&description='		+ $(event.target).find('#description-field').val() +
                                    '&issueKey=' 	  	+ JIRA.Issue.getIssueKey(),
                                type: 'POST',
                                dataType: 'json',
                                contentType: 'application/json',
                                async: false,
                                success: function() {
                                    JIRA.trigger(JIRA.Events.REFRESH_ISSUE_PAGE, [JIRA.Issue.getIssueId()]);
                                    AJS.dialog2(event.target).hide();
                                    AJS.dialog2(event.target).remove();
                                },
                                error: function() {
                                    showErrorMsg('Necessary endpoint could not be correctly accessed. Check console logs or contact your Jira Administrator')
                                }
                            });
                        }
                    }
                    else if (!toggleChecked && requiredFieldsFilled){
                        AJS.$.ajax({
                            url: "/rest/scriptrunner/latest/custom/closePurchase" +
                                "?createAsset=" 	+ toggleChecked +
                                "&cost=" 			+ $(event.target).find("#cost-field").val() +
                                "&issueKey=" 		+ JIRA.Issue.getIssueKey(),
                            type: 'POST',
                            dataType: 'json',
                            contentType: 'application/json',
                            async: false,
                            success: function() {
                                JIRA.trigger(JIRA.Events.REFRESH_ISSUE_PAGE, [JIRA.Issue.getIssueId()]);
                                AJS.dialog2(event.target).hide();
                                AJS.dialog2(event.target).remove();
                            },
                            error: function() {
                                showErrorMsg('Necessary endpoint could not be correctly accessed. Check console logs or contact your Jira Administrator')
                            }
                        });
                    }
                });
            }
        });
    });

    function makeFieldPicker($elem, usersList, imgSize, type, multiple) {
        $elem.auiSelect2({
            hasAvatar: true,
            formatResult: function (result) {
                return formatWithAvatar({
                    size: imgSize,
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
                for (let i = 0, ii = usersList.length; i < ii; i++) {
                    let result = usersList[i];
                    if (result.text.toLowerCase().indexOf(query.term.toLowerCase()) > -1) {
                        results.push(result);
                    }
                }
                query.callback({results: results});
            },
            multiple: multiple
        });
    }

    function formatWithAvatar(opt_data) {
        let personName = opt_data.person && opt_data.person.displayName ? opt_data.person.displayName : opt_data.person && opt_data.person.name ? opt_data.person.name : opt_data.unknownName;
        return '<span class="' + opt_data.type + '">' + aui.avatar.avatar({
            size: opt_data.size,
            avatarImageUrl: opt_data.person.avatarUrl
        }) + AJS.escapeHtml(personName) + '</span>';
    }

    function deviceFields (event, oper){
        switch(oper){
            case 'hide':
                $(event.target).find('#place-field-group').hide()
                $(event.target).find('#serial-number-field-group').hide()
                $(event.target).find('#place-field-group').hide()
                $(event.target).find('#model-field-group').hide()
                $(event.target).find('#invoice-number-field-group').hide()
                break
            case 'show':
                $(event.target).find('#place-field-group').show()
                $(event.target).find('#serial-number-field-group').show()
                $(event.target).find('#place-field-group').show()
                $(event.target).find('#model-field-group').show()
                $(event.target).find('#invoice-number-field-group').show()
                break
            default:
                console.error('not found oper')
        }
    }

    function licenseFields (event, oper){
        switch (oper){
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

    function globalFields(event, toggleIsChecked){
        switch (toggleIsChecked){
            case false:
                $(event.target).find('#cost-field-group').show()
                $(event.target).find('#not-create-asset-paragraph').css('display', 'inline')
                $(event.target).find('#asset-type-field-group').hide()
                $(event.target).find('#summary-field-group').hide()
                $(event.target).find('#description-field-group').hide()
                $(event.target).find('#create-asset-paragraph').css('display', 'none')
                $(event.target).find('#user-field-group').hide()
                break
            case true:
                $(event.target).find('#cost-field-group').hide()
                $(event.target).find('#not-create-asset-paragraph').css('display', 'none')
                $(event.target).find('#asset-type-field-group').show()
                $(event.target).find('#summary-field-group').show()
                $(event.target).find('#description-field-group').show()
                $(event.target).find('#create-asset-paragraph').css('display', 'inline')
                $(event.target).find("#device-radio-button").prop("checked", true)
                $(event.target).find('#user-field-group').show()
                break
            default:
                console.error('provided value should be boolean')
        }
    }

    function createSelectField(url, htmlTag, dynamic, fieldPicker, multiple){
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
            makeFieldPicker(AJS.$(htmlTag), dataList, multiple);
        }
        else {
            if (dynamic){
                AJS.$(htmlTag).auiSelect2({
                    tags: dataList,
                    multiple: multiple,
                    tokenSeparators: [','],
                    createTag: function (tag) {
                        return {
                            id: tag.term + ' (New component)',
                            text: tag.term + ' (New component)',
                            newOption: true
                        }
                    },
                })
            }
            else {
                AJS.$(htmlTag).auiSelect2({
                    data: dataList,
                    multiple: multiple
                })
            }
        }
    }

    function closeDialog(e, event){
        e.preventDefault();
        AJS.dialog2(event.target).hide();
        AJS.dialog2(event.target).remove();
    }

    function checkRequiredFields(event, htmlTag){
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
        if (list){
            let dataList = []
            dataSelected.forEach(function(elem){
                dataList.push(elem.dataType)
            })
            return dataList
        }
        else {
            return dataSelected
        }
    }

    function showErrorMsg(body) {
        AJS.flag({
            type: 'error',
            body: body,
            close: 'auto'
        });
    }
})(AJS.$);