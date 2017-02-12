var webSocket;

var requestsContent;
var phonesContent;
var serversContent;

var onconnect = function (payload) {
    setStatus("Connected.");
    webSocket.send(JSON.stringify({cmd: "subscribe"}));
};
var onmessage = function (payload) {
    var event = jQuery.parseJSON(payload.data);
    switch (event.event) {
        case "ping":
            break;
        case "requests":
            updateRequests(event.body);
            break;
        case "phones":
            updatePhones(event.body);
            break;
        case "servers":
            updateServers(event.body);
            break;
    }
};
var onclose = function (payload) {
    setStatus("Closed.");
//    alert('the connection has been closed')
};
var onerror = function (payload) {
    setStatus("Error.");
};
var updateRequests = function (requests) {
    requestsContent.find("tr").remove();
    // adds entries
    for (var i = 0; i < requests.length; i++) {
        var request = requests[i];
        var track = request.track;
        var range = request.range;
        requestsContent.append("<tr>" +
            "<td>" + request.serverID + "</td>" +
            "<td>" + request.uuid + "</td>" +
            "<td>" + track.title + "</td>" +
            "<td>" + track.artist + "</td>" +
            "<td>" + range.description + "</td>" +
            "</tr>");
    }
};
var updatePhones = function (phones) {
    // removes all entries
    phonesContent.find("tr").remove();
    // adds entries
    for (var i = 0; i < phones.length; i++) {
        var phone = phones[i];
        phonesContent.append("<tr><td>" + phone.s + "</td><td>" + phone.address + "</td></tr>");
    }
};
var updateServers = function (servers) {
    serversContent.find("tr").remove();
    for (var i = 0; i < servers.length; i++) {
        var server = servers[i];
        serversContent.append("<tr><td>" + server.id + "</td><td>" + server.address + "</td></tr>");
    }
};
var setStatus = function (newStatus) {
    document.getElementById("status").innerHTML = newStatus;
};
$(document).ready(function () {
    phonesContent = $("#phonesTable");
    serversContent = $("#serversTable");
    requestsContent = $("#requestsTable");
});