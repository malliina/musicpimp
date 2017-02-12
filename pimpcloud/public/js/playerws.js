"use strict";
var webSocket;
var isMute = false;

var onconnect = function () {
    setFeedback("Connected.");
    // update: sends status only after welcome message
//    send(statusJson());
};
// user input handlers
var stop = function () {
    send(stopJson());
};
var resume = function () {
    send(resumeJson());
};
var next = function () {
    send(nextJson());
};
var prev = function () {
    send(prevJson());
};
var seek = function (pos) {
    send(seekJson(pos));
};
var skip = function (index) {
    send(skipJson(index));
};
var remove = function (index) {
    send(removeJson(index));
    send(statusJson());
};
var vol = function (level) {
    send(volJson(level));
};
var togglemute = function () {
    isMute = !isMute;
    send(muteJson(isMute));
};

var send = function (json) {
    webSocket.send(json);
};
var add = function (id) {
    return send(addJson(id));
};
var play = function (id) {
    return send(playJson(id));
};
// server message handlers
var onmessage = function (payload) {
    var json = jQuery.parseJSON(payload.data);
    setFeedback(JSON.stringify(json));
};
var updateTime = function (secs) {
    $('#pos').html(secs.toHHMMSS());
    $("#slider").slider("option", "value", secs);
};
var updateDuration = function (secs) {
    $("#duration").html(secs.toHHMMSS());
    $("#slider").slider("option", "max", secs);
};
var updateTimeAndDuration = function (pos, dur) {
    updateTime(pos);
    updateDuration(dur);
};
var updatePlaylist = function (playlist) {
    $('li').remove(".song");
    var playlistElement = $("#playlist");
    for (var i = 0; i < playlist.length; i++) {
        playlistElement.append('<li class="song">' +
            '<a href="#" onclick="skip(' + i + ')">' + playlist[i].title + '</a>' +
            ' <a href="#" onclick="remove(' + i + ')"><i class="icon-remove"></i></a>' +
            '</li>')
    }
};
var updateTrack = function (track) {
    $('#title').html(track.title);
    $('#notracktext').hide();
    $('#album').html(track.album);
    $('#artist').html(track.artist);
    var zeroPos = 0;
    updateTimeAndDuration(zeroPos, track.duration)
};
var onStatus = function (json) {
    var title = json.track.title;
    if (title && title.length > 0) {
        updateTrack(json.track);
    }
    $('#pos').html(json.position.toHHMMSS());
    $("#volume").slider("option", "value", json.volume);
    isMute = json.mute;
    updateTimeAndDuration(json.position, json.track.duration);
    updatePlaylist(json.playlist);
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
