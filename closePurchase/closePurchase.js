(function($) {
	$(function() {
    	AJS.dialog2.on('show', function(event) {
        	if (event.target.id === 'close-purchase-dialog') {
            	const expireTimeField = document.getElementById('expire-time-field')
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
                    }
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
                	let toggleIsChecked = $('#create-asset-toggle').prop('checked')
                    globalFields(toggleIsChecked)
                  	if (toggleIsChecked) {
                        deviceFields('show')
                        $(event.target).find('#asset-type-field-group').change(function() {
                        	if ($('#license-radio-field').prop('checked')) {
                                licenseFields('show')
                                deviceFields('hide')
                    		}
                     		else if ($('#device-radio-field').prop('checked')) {
                                licenseFields('hide')
                                deviceFields('show')
                            }
                        })
                    } 
                    else {
                        deviceFields('hide')
                      	licenseFields('hide')
                    }
                  }); 
                  $(event.target).find("#create-button").click(function(e) {
                  		let toggleIsChecked = $('#create-asset-toggle').prop('checked');
                        var requiredFieldsFilled = true
                        $('#close-purchase-form input').each(function (){
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
                        	var softwareFieldData = $('#software-field').select2('data')
                            var softwareFieldChoices = []
                        	softwareFieldData.forEach(function(elem){
                   				softwareFieldChoices.push(elem.text)
                			})
                            AJS.$.ajax({
                                url: "/rest/scriptrunner/latest/custom/closePurchase" + 
                                    '?createAsset=' 	+ toggleIsChecked + 
                                    '&summary=' 		+ $('#summary-field').val() + 
                                    '&assetType='		+ $('input[name=asset-type-radio]').filter(":checked").val() +
                                    '&serviceTag=' 	  	+ $('#service-tag-field').val() + 
                                    '&model=' 		  	+ $('#model-field').val() + 
                                    '&invoiceNumber=' 	+ $('#invoice-number-field').val() + 
                                    '&software='		+ softwareFieldChoices.join(',') +
                                    '&licenseType='		+ $('#license-type-field').val() +
                                    '&expireTime='		+ $('#expire-time-field').val() +
                                    '&description='		+ $('#description-field').val() +
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
                                    "&cost=" 			+ $("#cost-field").val() + 
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
        
        function deviceFields (oper){
        	switch(oper){
        		case 'hide':
                	$('#service-tag-field-group').hide()
                    $('#place-field-group').hide()
                    $('#model-field-group').hide()
                    $('#invoice-number-field-group').hide()
                    break
                case 'show':
                	$('#service-tag-field-group').show()
                    $('#place-field-group').show()
                    $('#model-field-group').show()
                    $('#invoice-number-field-group').show()
                    break
                default:
                	log.error('not found oper')
        	}
        }
        
        function licenseFields (oper){
        	switch (oper){
            	case 'hide':
                	$('#software-field-group').hide()
                    $('#license-type-field-group').hide()
                    $('#expire-time-field-group').hide()
                	break
                case 'show':
                    $('#software-field-group').show()
                    $('#license-type-field-group').show()
                    $('#license-type-field').change(function() {
                        if ($('#license-type-field').val() === 'Subscription')
                            $('#expire-time-field-group').show()
                        else 
                            $('#expire-time-field-group').hide() 
                    })
                    break
                default:
                	log.error('not found oper')
            }
        }
        
        function globalFields(toggleIsChecked){
        	switch (toggleIsChecked){
            	case false:
                	$('#cost-field-group').show()
                    $('#not-create-asset-paragraph').show() 
                	$('#asset-type-field-group').hide()
                    $('#summary-field-group').hide()
                    $('#description-field-group').hide()
                    $('#create-asset-paragraph').hide()
                	break
                case true:
                    $('#cost-field-group').hide()
                    $('#not-create-asset-paragraph').hide() 
                	$('#asset-type-field-group').show()
                    $('#summary-field-group').show()
                    $('#description-field-group').show()
                    $('#create-asset-paragraph').show()
					$("#device-radio-button").prop("checked", true);
                    break
                default:
                	log.error('provided value should be boolean')
            }
        }
})(AJS.$);
