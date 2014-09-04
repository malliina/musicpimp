var webSocket;

var tableContent;

var onconnect = function (payload) {
    webSocket.send(JSON.stringify({cmd: "subscribe"}));
};
var onmessage = function (payload) {
    var logEvent = jQuery.parseJSON(payload.data);
    prepend(logEvent);
};
// case class LogEvent(timeStamp: Long, timeFormatted: String, message: String, loggerName: String, threadName: String, level: Level)
var prepend = function (e) {
    tableContent.prepend("<tr><td class='col-md-1'>" + e.timeFormatted + "</td><td>" + e.message + "</td><td>" + e.loggerName + "</td><td>" + e.threadName + "</td><td>" + e.level + "</td></tr>")
};
$(document).ready(function () {
    tableContent = $("#logTableBody");
});