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
              AJS.flag({type: 'error', body: 'Need to select at least one component', close: 'auto'})
            else {             
            	var data = []
            	selectData.forEach(function(elem){
                data.push(elem.text)
            	})
              AJS.$.ajax({
                url: "/rest/scriptrunner/latest/custom/cloneIssueWithComponent?components=" + data.join(','),
                type: 'GET',
                dataType: 'json',
                async: false,
                success: function(response) {
            			AJS.flag({type: 'success', body: response, close: 'auto'})
        				},
                error: function(response) {
                	AJS.flag({type: 'error', body: response, close: 'auto'})
                }
              });
            }
        });
    });
  });
