var webSocket;

var onconnect = function (payload) {
    showConnected();
    sendCommand("subscribe");
};
var onmessage = function (payload) {
    var json = jQuery.parseJSON(payload.data);
    var eventType = json.event;
    switch (eventType) {
        case "search_status":
            setIndexInfo(json.status);
            break;
    }
};
var onclose = function (payload) {
    // setStatus("Connection closed");
    showDisconnected();
};
var onerror = function (payload) {
    // setStatus("Connection error");
    showDisconnected();
};
var refresh = function () {
    sendCommand("refresh");
};
var sendCommand = function (command) {
    webSocket.send(JSON.stringify({cmd: command}));
};
var showConnected = function () {
    $("#okstatus").removeClass("hide");
    $("#failstatus").addClass("hide");
};
var showDisconnected = function () {
    $("#okstatus").addClass("hide");
    $("#failstatus").removeClass("hide");
};
var setIndexInfo = function (status) {
    $("#index-info").html(status);
};
$(document).ready(function () {

});
