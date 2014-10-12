var webSocket;

var tableContent;

var onconnect = function (payload) {
    webSocket.send(JSON.stringify({cmd: "subscribe"}));
    setFeedback("Connected.");
};
var onmessage = function (payload) {
    var event = jQuery.parseJSON(payload.data);
    if (event.event == "ping") {

    } else {
        prepend(event);
    }
};
// case class LogEvent(timeStamp: Long, timeFormatted: String, message: String, loggerName: String, threadName: String, level: Level)
var prepend = function (e) {
    var trc;
    var level = e.level;
    if (level == "ERROR") {
        trc = "danger";
    } else if (level == "WARN") {
        trc = "warning";
    } else {
        trc = "";
    }
    tableContent.prepend("<tr class=" + trc + "><td class='col-md-1'>" + e.timeFormatted + "</td><td>" + e.message + "</td><td>" + e.loggerName + "</td><td>" + e.threadName + "</td><td>" + e.level + "</td></tr>")
};
var onclose = function (payload) {
    setFeedback("Connection closed.");
};
var onerror = function (payload) {
    setFeedback("Connection error.");
};
var setFeedback = function (fb) {
    $('#status').html(fb);
};
$(document).ready(function () {
    tableContent = $("#logTableBody");
});