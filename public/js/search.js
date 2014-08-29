var webSocket;

var onconnect = function (payload) {
//    webSocket.send(JSON.stringify({cmd: "subscribe"}));
};
var onmessage = function (payload) {
    var json = jQuery.parseJSON(payload.data);
    var eventType = json.event;
    switch (eventType) {
        case "search_status":
            setStatus(json.status);
            break;
    }
};
var onclose = function (payload) {
    setStatus("Connection closed.");
};
var onerror = function (payload) {
    setStatus("Connection error.");
};
var refresh = function () {
    webSocket.send(JSON.stringify({cmd: "refresh"}));
};
var sendCommand = function (command) {
    webSocket.send(JSON.stringify({cmd: command}));
};
var setStatus = function (status) {
    $('#status').html(status);
};
$(document).ready(function () {
//    $("#term").autocomplete({
//        source: "/search",
//        minLength: 2
//    });
});
