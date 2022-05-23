
(function($) {
	$(function() {
    	AJS.dialog2.on('show', function(event) {
        	if (event.target.id === 'close-grant-access-dialog') {
            	let usersList
                let accessComponents
                AJS.$.ajax({
                    url: '/rest/scriptrunner/latest/custom/getPersonAccesses' +
                        '?accessToken=' 	+ 'token',
                    type: 'GET',
                    datatype: 'json',
                    async: false,
                    success: function(data){ usersList = data }
                })
                AJS.$(".aui-select2 select").auiSelect2()
                makeUserPicker(AJS.$("#user-field"), usersList, true);
                AJS.$.ajax({
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
                });
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
                    if (toggleIsChecked) {
                    	globalFields(event, toggleIsChecked)
                    }
                    else {
                    	globalFields(event, toggleIsChecked)
                    }
                }); 
                $(event.target).find("#create-button").click(function(e) {
                    let toggleIsChecked = $(event.target).find('#create-asset-toggle').prop('checked');
                    let requiredFieldsFilled = true
                    $(event.target).find('#close-grant-access-form input').each(function (){
                        if ($(this).is(':visible') && this.value === '' && !(this.id).contains('s2id_autogen')){ 
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
                        let usersSelected = $(event.target).find('#user-field').select2('data')
                        let usersList = []
                        usersSelected.forEach(function(elem){
                            usersList.push(elem.id)
                        })
                        let accessComponentsSelected = $(event.target).find('#environment-field').select2('data')
                        let accessComponentsList = []
                        accessComponentsSelected.forEach(function(elem){
                            accessComponentsList.push(elem.id)
                        })
                        AJS.$.ajax({
                            url: "/rest/scriptrunner/latest/custom/closeGrantAccess" + 
                            '?createAsset=' 	+ toggleIsChecked + 
                            '&summary=' 		+ $(event.target).find('#summary-field').val() +
                            '&user=' 			+ usersList +
                            '&environment=' 	+ accessComponentsList +
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
                            url: "/rest/scriptrunner/latest/custom/closeGrantAccess" + 
                            '?createAsset=' 	+ toggleIsChecked + 
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
})(AJS.$);
