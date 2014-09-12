var everyDay = ["mon", "tue", "wed", "thu", "fri", "sat", "sun"];

var isChecked = function (id) {
    return $("#" + id).is(":checked");
};
var everyDayClicked = function () {
    var newValue = $("#every").is(":checked");
    setAll(everyDay, newValue);
};
var updateEveryDayCheckBox = function () {
    var isEveryDayClicked = everyDay.every(isChecked);
    setAll(["every"], isEveryDayClicked);
};
var setAll = function (checkIDs, checkedValue) {
    var index;
    for (index = 0; index < checkIDs.length; ++index) {
        var queryID = "#" + checkIDs[index];
        $(queryID).prop("checked", checkedValue);
    }
};
var deleteAP = function (id) {
    return postCommand(idCommandJson("delete", id));
};
var runAP = function (id) {
    return postCommand(idCommandJson("start", id));
};
var stopPlayback = function () {
    return postAjax2(simpleCommandJson("stop"), "/alarms");
};
var postAjaxFinallyReload = function (json, resource) {
    postAjax(json,resource).done(function (data) {
        // reloads the page
        location.reload(false);
    });
    // doesn't jump to the top of the page following calls to this script
    return false;
};
var postAjax = function (json, resource) {
    return $.ajax({
        url: resource,
        type: 'POST',
        contentType: 'application/json',
        data: json
    });
};
var postAjax2 = function (json, resource) {
    postAjax(json, resource);
    return false;
};
var postCommand = function (json) {
    return postAjaxFinallyReload(json, "/alarms");
};
var simpleCommandJson = function (cmd) {
    return JSON.stringify({cmd: cmd});
};
var idCommandJson = function (cmd, id) {
    return JSON.stringify({cmd: cmd, id: id});
};

