(function($) {
	$(function() {
    	AJS.dialog2.on('show', function(event) {
        	if (event.target.id === 'close-grant-access-dialog') {
                /*AJS.$.ajax({
                    url: '/rest/scriptrunner/latest/custom/getActiveUsers?accessToken=token1',
                    type: 'GET',
                    datatype: 'json',
                    async: false,
                    success: function(data){ usersList = data }
                })
                AJS.$(".aui-select2 select").auiSelect2()
                makeUserPicker(AJS.$("#user-field"), usersList, true);*/
                /*AJS.$.ajax({
                    url: '/rest/scriptrunner/latest/custom/getAssetComponents?type=access',
                    type: 'GET',
                    datatype: 'json',
                    async: false,
                    success: function(data){accessComponents = data}
                })
                AJS.$("#environment-field").auiSelect2({
                    tags: accessComponents,
                    tokenSeparators: [','],
                    placeholder: 'Select a component/s',
                    createTag: function (params, term) {
                        term = $.trim(params.term);
                        if (term === '')
                            return null
                        return {
                            id: term + ' (New component)',
                            text: term + ' (New Component)',
                            newTag: true // add additional parameters
                        }
                    },
                });*/
                createSelectField('/rest/scriptrunner/latest/custom/getActiveUsers?accessToken=token1', '#user-field' ,false, true, true)
                createSelectField('/rest/scriptrunner/latest/custom/getAssetComponents?type=access', '#environment-field' ,true, false, true)
                $(event.target).find("#close-button").click(function(e) {
                	closeDialog(e, event)
              	});

              	$(event.target).find("#cancel-button").click(function(e) {
                    closeDialog(e, event)
              	});

              	$(event.target).find('#create-asset-toggle').change(function() {
                	let toggleIsChecked = $(event.target).find('#create-asset-toggle').prop('checked')
                    if (toggleIsChecked)
                    	globalFields(event, toggleIsChecked)
                    else
                    	globalFields(event, toggleIsChecked)
                });

                $(event.target).find("#create-button").click(function(e) {
                    let toggleChecked = $(event.target).find('#create-asset-toggle').prop('checked');
                    let requiredFieldsFilled = checkRequiredFields(event, '#close-grant-access-form')
                    if (!requiredFieldsFilled)
                        showErrorMsg('Fill the required fields')
                    else if (toggleChecked && requiredFieldsFilled) {
                        let userList = getSelectData(event, '#user-field', 'id', true)
                        let accessList = getSelectData(event, '#environment-field', 'id', true)
                        /*let usersSelected = $(event.target).find('#user-field').select2('data')
                        let usersList = []
                        usersSelected.forEach(function(elem){
                            usersList.push(elem.id)
                        })
                        let accessComponentsSelected = $(event.target).find('#environment-field').select2('data')
                        let accessComponentsList = []
                        accessComponentsSelected.forEach(function(elem){
                            accessComponentsList.push(elem.id)
                        })*/
                        AJS.$.ajax({
                            url: "/rest/scriptrunner/latest/custom/closeGrantAccess" + 
                                 '?createAsset=' 	+ toggleChecked +
                                 '&summary=' 		+ $(event.target).find('#summary-field').val() +
                                 '&user=' 			+ userList +
                                 '&environment=' 	+ accessList +
                                 '&description='	+ $(event.target).find('#description-field').val() +
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
                    else if (!toggleChecked && requiredFieldsFilled){
                        AJS.$.ajax({
                            url: "/rest/scriptrunner/latest/custom/closeGrantAccess" + 
                                 '?createAsset=' 	+ toggleChecked +
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

    function globalFields (event, toggleIsChecked){
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
