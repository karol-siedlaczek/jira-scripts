AJS.$.ajax({
    url: '/rest/api/latest/project/{projkey}/components',
    type: 'GET',
    datatype: 'json',
    success: function(data) {
        data.forEach( elem =>
            components.push(elem.name)
       )
   }
})
