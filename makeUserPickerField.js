//<input class="text aui-select2" type="text" length="60" id="user-field" name="User" placeholder="Select a user"></input>
(function($) {
  $(function() {
	  var usersList
	  AJS.$.ajax({
		url: '/rest/scriptrunner/latest/custom/{endpointWithUserList}',
		type: 'GET',
		datatype: 'json',
		async: false,
		success: function(data){
			usersList = data
		}
	})
	AJS.$(".aui-select2 select").auiSelect2();
    makeUserPicker(AJS.$("#user-field"), usersList);
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
      formatResult: function(result) {
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
      query: function(query) {
        var results = [];
        for (var i = 0, ii = usersList.length; i < ii; i++) {
          var result = usersList[i];
          if (result.text.toLowerCase().indexOf(query.term.toLowerCase()) > -1) {
            results.push(result);
          }
        }
        query.callback({
          results: results
        });
      },
      multiple: multiple
    });
  }
})(AJS.$);
