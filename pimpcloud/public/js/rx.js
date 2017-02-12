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
        prependAll(event);
    }
};
// case class LogEvent(timeStamp: Long, timeFormatted: String, message: String, loggerName: String, threadName: String, level: Level)
var rowCounter = 0;
var prependAll = function (es) {
    for(var i = 0; i < es.length; i++) {
        prepend(es[i]);
    }
};
var prepend = function(e) {
    var trc;
    var level = e.level;
    if (level == "ERROR") {
        trc = "danger";
    } else if (level == "WARN") {
        trc = "warning";
    } else {
        trc = "";
    }
    rowCounter += 1;
    var levelContent = e.level;
    if (e.stackTrace != null) {
        tableContent.prepend("<tr style='display: none' id='row" + rowCounter + "'><td colspan='5'><pre>" + e.stackTrace + "</pre></td></tr>");
        levelContent = "<a href='#' onclick='return toggle(" + rowCounter + ")'>" + levelContent + "</a>";
    }
    tableContent.prepend(
        "<tr class=" + trc + ">" +
        "<td class='col-md-1'>" + e.timeFormatted + "</td>" +
        "<td>" + e.message + "</td>" +
        "<td>" + e.loggerName + "</td>" +
        "<td>" + e.threadName + "</td>" +
        "<td>" + levelContent + "</td>" +
        "</tr>")
};
var toggle = function (row) {
    $("#row" + row).toggle();
    // prevents returning to the top of the page
    return false;
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
