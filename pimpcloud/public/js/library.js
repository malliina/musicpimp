var postAjax = function (json, resource) {
    $.ajax({
        url: resource,
        type: 'POST',
        contentType: 'application/json',
        data: json
    });
    // don't jump to the top of the page following calls to this script
    return false;
};
var postPlayback = function (json) {
    return postAjax(json, "/playback");
};
var add = function (id) {
    return postPlayback(addJson(id));
};
var play = function (id) {
    return postPlayback(playJson(id));
};