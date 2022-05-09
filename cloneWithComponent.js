(function ($) {
  $(function () {
    AJS.dialog2.on('show', function (event) {
      if (event.target.id === 'clone-issues-with-component-dialog')
        AJS.$("#components-select2").auiSelect2();
        $(event.target).find("#close-button").click(function (e){
            e.preventDefault();
            AJS.dialog2(event.target).hide();
            AJS.dialog2(event.target).remove();
        });
        $(event.target).find("#cancel-button").click(function (e){
            e.preventDefault();
            AJS.dialog2(event.target).hide();
            AJS.dialog2(event.target).remove();
        });
        $(event.target).find("#create-button").click(function (e){
            var selectData = $('#components-select2').select2('data')
            if (selectData[0] === undefined || selectData[0] === null)
              AJS.flag({type: 'error', body: 'At least one component need to be selected', close: 'auto'})
            else {
                var components = []
                selectData.forEach(function(elem){
                   components.push(elem.text)
                })
              AJS.$.ajax({
                url: "/rest/scriptrunner/latest/custom/cloneIssueWithComponent?components=" + components.join(',') + "&issueKey=" + JIRA.Issue.getIssueKey(),
                type: 'POST',
                dataType: 'json',
                contentType: 'application/json',
                async: false,
                success: function(response){
                   JIRA.trigger(JIRA.Events.REFRESH_ISSUE_PAGE, [JIRA.Issue.getIssueId()]);
                   AJS.dialog2(event.target).hide();
                   AJS.dialog2(event.target).remove();
                },
                error: function(response){
                   AJS.flag({type: 'error', body: 'Necessary endpoint could not be accessed. Check console logs or contact your Jira Administrator', close: 'auto'});
                }
              });
            }
        });
    });
  });
})(AJS.$);
