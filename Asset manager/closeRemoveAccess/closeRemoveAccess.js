(function($) {
	$(function() {
    	AJS.dialog2.on('show', function(event) {
        	if (event.target.id === 'close-remove-access-dialog') {
            	let usersList
                AJS.$(".aui-select2 select").auiSelect2()
                AJS.$.ajax({
                    url: '/rest/scriptrunner/latest/custom/getActiveUsers' +
                        '?accessToken=' 	+ 'token',
                    type: 'GET',
                    datatype: 'json',
                    async: false,
                    success: function(data){ usersList = data }
                })
                makeFieldPicker(AJS.$("#user-field"), usersList, 'small', 'user', true);
                makeFieldPicker(AJS.$("#issue-field"), [], 'xsmall', 'issue', true);
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
                $(event.target).find("#user-field-search").click(function() {
                    let usersSelected = $(event.target).find('#user-field').select2('data')
                    let usersList = []
                    usersSelected.forEach(function(elem){
                        usersList.push(elem.id)
                    })
                    if (usersList.length === 0) {
                        AJS.flag({
                            type: 'error',
                            body: 'User/s field is empty',
                            close: 'auto'
                        });
                    }
                    else {
                        let accessList
                        AJS.$.ajax({
                            url: '/rest/scriptrunner/latest/custom/getPersonAccesses' +
                                '?username='        + usersList +
                                '&accessToken=' 	+ 'token',
                            type: 'GET',
                            datatype: 'json',
                            async: false,
                            success: function(data){ accessList = data },
                            error: function() {
                                AJS.flag({
                                    type: 'error',
                                    body: 'Necessary endpoint could not be correctly accessed. Check console logs or contact your Jira Administrator',
                                    close: 'auto'
                                });
                            }
                        })
                        if (accessList.length === 0){
                            makeFieldPicker(AJS.$("#issue-field"), accessList, 'xsmall', 'issue', true);
                            AJS.flag({
                                type: 'error',
                                body: 'Not found any access assigned to selected user/s',
                                close: 'auto'
                            });
                        }
                        else {
                            makeFieldPicker(AJS.$("#issue-field"), accessList, 'xsmall', 'issue', true);
                            AJS.flag({
                                type: 'success',
                                body: 'Found accesses for selected user/s',
                                close: 'auto'
                            });
                        }

                    }
                })
                $(event.target).find("#create-button").click(function() {
                    let accessesSelected = $(event.target).find('#issue-field').select2('data')
                    let accessesToRemoveList = []
                    accessesSelected.forEach(function(elem){
                        accessesToRemoveList.push(elem.id)
                    })
                    if (accessesToRemoveList.length !== 0) {
                        AJS.$.ajax({
                            url: "/rest/scriptrunner/latest/custom/closeRemoveAccess" +
                                '?accessIssue='     + accessesToRemoveList +
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
                    else {
                        AJS.$.ajax({
                            url: "/rest/scriptrunner/latest/custom/closeRemoveAccess" +
                                '?onlyClose=' 	    + true +
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
    function formatWithAvatar(opt_data, type) {
        var personName = opt_data.person && opt_data.person.displayName ? opt_data.person.displayName : opt_data.person && opt_data.person.name ? opt_data.person.name : opt_data.unknownName;
        return '<span class="' + opt_data.type + '">' + aui.avatar.avatar({
            size: opt_data.size,
            avatarImageUrl: opt_data.person.avatarUrl
        }) + AJS.escapeHtml(personName) + '</span>';
    }

    function makeFieldPicker($el, usersList, imgSize, type, multiple) {
        $el.auiSelect2({
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
})(AJS.$);
