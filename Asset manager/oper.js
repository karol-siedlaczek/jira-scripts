function spinner(event, oper) {
    switch (oper){
        case 'hide':
            $(event.target).css('display', 'block')
            break
        case 'show':
            $(event.target).css('display', 'none')
            break
        default:
            console.error('not found oper')
    }
}

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