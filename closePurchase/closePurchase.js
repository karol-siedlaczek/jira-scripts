(function($) {
    $(function() {
        AJS.dialog2.on('show', function(event) {
            if (event.target.id === 'close-purchase-dialog') {
                const expireTimeField = $(event.target).find('#expire-time-field')[0]
                const controller = new AJS.DatePicker(expireTimeField, {'overrideBrowserDefault': true})
                let usersList
                let licenseComponents
                AJS.$.ajax({
                    url: '/rest/scriptrunner/latest/custom/getActiveUsers' +
                         '?accessToken=' 	+ 'token',
                    type: 'GET',
                    datatype: 'json',
                    async: false,
                    success: function(data){ usersList = data }
                })
                AJS.$(".aui-select2 select").auiSelect2()
                makeUserPicker(AJS.$("#user-field"), usersList);
                AJS.$.ajax({
                    url: '/rest/scriptrunner/latest/custom/getAssetComponents?type=license',
                    type: 'GET',
                    datatype: 'json',
                    async: false,
                    success: function(data){licenseComponents = data}
                })
                AJS.$("#software-field").auiSelect2({
                    tags: licenseComponents,
                    tokenSeparators: [','],
                    createTag: function (tag) {
                        return {
                            id: tag.term + ' (New component)',
                            text: tag.term + ' (New component)',
                            newOption: true
                        }
                    },
                })
                $(event.target).find("#close-button").click(function(e) {
                    e.preventDefault();
                    AJS.dialog2(event.target).hide();
                    AJS.dialog2(event.target).remove();
                });
                $(event.target).find("#cancel-button").click(function(e) {
                    e.preventDefault();
                    AJS.dialog2(event.target).hide();
                    AJS.dialog2(event.target).remove();
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
                    let toggleIsChecked = $(event.target).find('#create-asset-toggle').prop('checked');
                    let requiredFieldsFilled = true
                    $(event.target).find('#close-purchase-form input').each(function (){
                        if ($(this).is(':visible') && this.value === '' && $(this).prop('required') && !(this.id).contains('s2id_autogen')){
                            requiredFieldsFilled = false
                            console.error(this.id + ' is empty')
                        }
                    })
                    if (!requiredFieldsFilled){
                        AJS.flag({
                            type: 'error',
                            body: 'Fill the required fields',
                            close: 'auto'
                        });
                    }
                    if (toggleIsChecked && requiredFieldsFilled) {
                        let licenseComponentsSelected = $(event.target).find('#software-field').select2('data')
                        let licenseComponentsList = []
                        licenseComponentsSelected.forEach(function(elem){
                            licenseComponentsList.push(elem.text)
                        })
                        let userSelected = $(event.target).find('#user-field').select2('data')
                        if (userSelected)
                            userSelected = userSelected.id
                        AJS.$.ajax({
                            url: "/rest/scriptrunner/latest/custom/closePurchase" +
                                '?createAsset=' 	+ toggleIsChecked +
                                '&summary=' 		+ $(event.target).find('#summary-field').val() +
                                '&assetType='		+ $(event.target).find('input[name=asset-type-radio]').filter(":checked").val() +
                                '&serviceTag=' 	  	+ $(event.target).find('#service-tag-field').val() +
                                '&model=' 		  	+ $(event.target).find('#model-field').val() +
                                '&invoiceNumber=' 	+ $(event.target).find('#invoice-number-field').val() +
                                '&software='		+ licenseComponentsList +
                                '&user='			+ userSelected +
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
                                AJS.flag({
                                    type: 'error',
                                    body: 'Necessary endpoint could not be correctly accessed. Check console logs or contact your Jira Administrator',
                                    close: 'auto'
                                });
                            }
                        });
                    }
                    else if (!toggleIsChecked && requiredFieldsFilled){
                        AJS.$.ajax({
                            url: "/rest/scriptrunner/latest/custom/closePurchase" +
                                "?createAsset=" 	+ toggleIsChecked +
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
                                AJS.flag({
                                    type: 'error',
                                    body: 'Necessary endpoint could not be correctly accessed. Check console logs or contact your Jira Administrator',
                                    close: 'auto'
                                });
                            }
                        });
                    }
                });
            }
        });
    });
    function formatWithAvatar(opt_data) {
        var personName = opt_data.person && opt_data.person.displayName ? opt_data.person.displayName : opt_data.person && opt_data.person.name ? opt_data.person.name : opt_data.unknownName;
        return '<span>' + aui.avatar.avatar({
            size: opt_data.size,
            avatarImageUrl: opt_data.person.avatarUrl
        }) + AJS.escapeHtml(personName) + '</span>';
    }

    function makeUserPicker($el, usersList, multiple) {
        $el.auiSelect2({
            hasAvatar: true,
            formatResult: function (result) {
                return formatWithAvatar({
                    size: 'small',
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
                var results = [];
                for (var i = 0, ii = usersList.length; i < ii; i++) {
                    var result = usersList[i];
                    if (result.text.toLowerCase().indexOf(query.term.toLowerCase()) > -1) {
                        results.push(result);
                    }
                }
                query.callback({results: results});
            },
            multiple: multiple
        });
    }

    function deviceFields (event, oper){
        switch(oper){
            case 'hide':
                $(event.target).find('#service-tag-field-group').hide()
                $(event.target).find('#place-field-group').hide()
                $(event.target).find('#model-field-group').hide()
                $(event.target).find('#invoice-number-field-group').hide()
                break
            case 'show':
                $(event.target).find('#service-tag-field-group').show()
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
})(AJS.$);