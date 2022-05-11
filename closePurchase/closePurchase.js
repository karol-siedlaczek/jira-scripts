(function($) {
	$(function() {
    	AJS.dialog2.on('show', function(event) {
        	if (event.target.id === 'close-purchase-dialog') {
                const expireTimeField = $(event.target).find('#expire-time-field')[0]
                const controller = new AJS.DatePicker(expireTimeField, {'overrideBrowserDefault': true})
                var softwareOptions = []
                AJS.$.ajax({
                    url: '/rest/scriptrunner/latest/custom/getSoftwareOptions',
                    type: 'GET',
                    datatype: 'json',
                    async: false,
                    success: function(data){softwareOptions = data}
                })
                AJS.$("#software-field").auiSelect2({
                    tags: softwareOptions,
                    tokenSeparators: [','],
                    placeholder: 'Select a component/s',
                    createTag: function (params, term) {
                        var term = $.trim(params.term);
                        if (term === '')
                            return null
                        return {
                            id: term + ' (new)',
                            text: term + ' (new)',
                            newTag: true // add additional parameters
                        }
                    },
                    /*createTag: function (params, term) {
                                    	return {
                                        	id: params.term + ' new',
                                        	text: params.term + ' new',
                                        	newOption: true
                                        }
                                    }
                             formatNoMatches: function(term) {
        								return '<strong>' + term + '</strong> (New Component)';
    						}*/
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
                    globalFields(event, toggleIsChecked)
                  	if (toggleIsChecked) {
                        deviceFields(event, 'show')
                        $(event.target).find('#asset-type-field-group').change(function() {
                        	if ($(event.target).find('#license-radio-field').prop('checked')) {
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
                  		let toggleIsChecked = $('#create-asset-toggle').prop('checked');
                        var requiredFieldsFilled = true
                        $(event.target).find('#close-purchase-form input').each(function (){
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
                        	var softwareFieldData = $(event.target).find('#software-field').select2('data')
                            var softwareFieldChoices = []
                        	softwareFieldData.forEach(function(elem){
                   				softwareFieldChoices.push(elem.text)
                			})
                            AJS.$.ajax({
                                url: "/rest/scriptrunner/latest/custom/closePurchase" + 
                                    '?createAsset=' 	+ toggleIsChecked + 
                                    '&summary=' 		+ $(event.target).find('#summary-field').val() + 
                                    '&assetType='		+ $(event.target).find('input[name=asset-type-radio]').filter(":checked").val() +
                                    '&serviceTag=' 	  	+ $(event.target).find('#service-tag-field').val() + 
                                    '&model=' 		  	+ $(event.target).find('#model-field').val() + 
                                    '&invoiceNumber=' 	+ $(event.target).find('#invoice-number-field').val() + 
                                    '&software='		+ softwareFieldChoices.join(',') +
                                    '&licenseType='		+ $(event.target).find('#license-type-field').val() +
                                    '&expireTime='		+ $(event.target).find('#expire-time-field').val() +
                                    '&description='		+ $(event.target).find('#description-field').val() +
                                    '&issueKey=' 	  	+ JIRA.Issue.getIssueKey(),
                                type: 'POST',
                                dataType: 'json',
                                contentType: 'application/json',
                                async: false,
                                /*data: {
                                    createAsset: isChecked,
                                    serviceTag: $("#service-tag-field").val(),
                                    model: $("#model-field").val(),
                                    place: $("#place").val(),
                                    issueKey: JIRA.Issue.getIssueKey(),
                                    test: JIRA.Issue.getIssueKey()
                                },*/
                                success: function(response) {
                                    JIRA.trigger(JIRA.Events.REFRESH_ISSUE_PAGE, [JIRA.Issue.getIssueId()]);
                                    AJS.dialog2(event.target).hide();
                                    AJS.dialog2(event.target).remove();
                                },
                                error: function(response) {
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
                                /*data: {
                                    asset: toggleIsChecked,
                                    test: JIRA.Issue.getIssueKey()
                                },*/
                                success: function(response, data) {
                                	//data = JSON.stringify(data)
                                    JIRA.trigger(JIRA.Events.REFRESH_ISSUE_PAGE, [JIRA.Issue.getIssueId()]);
                                    AJS.dialog2(event.target).hide();
                                    AJS.dialog2(event.target).remove();
                                },
                                error: function(response) {
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
                    $(event.target).find('#not-create-asset-paragraph').show() 
                	$(event.target).find('#asset-type-field-group').hide()
                    $(event.target).find('#summary-field-group').hide()
                    $(event.target).find('#description-field-group').hide()
                    $(event.target).find('#create-asset-paragraph').hide()
                	break
                case true:
                    $(event.target).find('#cost-field-group').hide()
                    $(event.target).find('#not-create-asset-paragraph').hide() 
                	$(event.target).find('#asset-type-field-group').show()
                    $(event.target).find('#summary-field-group').show()
                    $(event.target).find('#description-field-group').show()
                    $(event.target).find('#create-asset-paragraph').show()
					$(event.target).find("#device-radio-button").prop("checked", true);
                    break
                default:
                	console.error('provided value should be boolean')
            }
        }
})(AJS.$);
