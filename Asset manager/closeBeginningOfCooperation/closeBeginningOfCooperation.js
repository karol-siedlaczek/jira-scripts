(function($) {
    $(function() {
        AJS.dialog2.on('show', function(event) {
            if (event.target.id === 'close-beginning-of-cooperation-dialog') {
                AJS.$(".aui-select2 select").auiSelect2()
                let usersList
                let devicesList
                let accessesList
                AJS.$.ajax({
                    url: '/rest/scriptrunner/latest/custom/getActiveUsers' +
                        '?accessToken=' + '',
                    type: 'GET',
                    datatype: 'json',
                    async: false,
                    success: function (data) {
                        usersList = data
                    }
                })
                makeFieldPicker(AJS.$("#user-field"), usersList, 'small', 'user');

                AJS.$.ajax({
                    url: '/rest/scriptrunner/latest/custom/getDevicesOnStock' +
                        '?accessToken=' 	+ '',
                    type: 'GET',
                    datatype: 'json',
                    async: false,
                    success: function(data){ devicesList = data }
                })
                makeFieldPicker(AJS.$("#device-field"), devicesList, 'xsmall', 'issue', true);

                AJS.$.ajax({
                    url: '/rest/scriptrunner/latest/custom/getAssetComponents' +
                         '?type='       + 'access',
                    type: 'GET',
                    datatype: 'json',
                    async: false,
                    success: function(data){accessesList = data}
                })
                AJS.$("#access-field").auiSelect2({
                    tags: accessesList,
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
                $(event.target).find("#create-button").click(function() {
                    spinner(event, 'show')
                    let userSelected = $(event.target).find('#user-field').select2('data')
                    if (userSelected) {
                        AJS.$.ajax({
                            url: "/rest/scriptrunner/latest/custom/getPersonAsset" +
                                 '?username='            + userSelected.id,
                            type: 'GET',
                            datatype: 'json',
                            async: false,
                            success: function() {
                                userSelected = userSelected.id
                                let devicesSelected = $(event.target).find('#device-field').select2('data')
                                let accessesSelected = $(event.target).find('#access-field').select2('data')
                                let devicesSelectedList = []
                                let accessesSelectedList = []

                                devicesSelected.forEach(function(elem){
                                    devicesSelectedList.push(elem.id)
                                })
                                accessesSelected.forEach(function(elem){
                                    accessesSelectedList.push(elem.id)
                                })
                                AJS.$.ajax({
                                    url: "/rest/scriptrunner/latest/custom/closeBeginningOfCooperation" +
                                        '?user='            + userSelected +
                                        '&device='          + devicesSelectedList +
                                        '&access='          + accessesSelectedList +
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
                                        spinner(event, 'hide')
                                        AJS.flag({
                                            type: 'error',
                                            body: 'Necessary endpoint could not be correctly accessed. Check console logs or contact your Jira Administrator',
                                            close: 'auto'
                                        });
                                    }
                                })
                            },
                            error: function(response) {
                                spinner(event, 'hide')
                                AJS.flag({
                                    type: 'error',
                                    body: 'Person <strong>' + response.responseText + '</strong> for selected user already exists.' +
                                          '<ul class="aui-nav-actions-list">' +
                                          '<li><a class="aui-button aui-button-link" target="_blank" href="https://jira.redge.com/browse/' + response.responseText + '">View issue</a></li>' +
                                          '</ul>',
                                    close: 'auto'
                                });
                            }
                        })
                    }
                    else {
                        spinner(event, 'hide')
                        AJS.flag({
                            type: 'error',
                            body: 'Fill user field',
                            close: 'auto'
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

    async function spinner(event, oper) {
        switch (oper){
            case 'show':
                $(event.target).find('#custom-dialog-spinner').css('display', 'inline')
                break
            case 'hide':
                $(event.target).find('#custom-dialog-spinner').css('display', 'none')
                break
            default:
                console.error('not found oper')
        }
    }
})(AJS.$);
