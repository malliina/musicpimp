"use strict";
var webSocket;
var isMute = false;

var onconnect = function () {
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
// server message handlers
var onmessage = function (payload) {
    var json = jQuery.parseJSON(payload.data);
    var eventType = json.event;
    switch (eventType) {
        case "welcome":
            send(statusJson());
            break;
        case "time_updated":
            updateTime(json.pos_seconds);
            break;
        case "track_changed":
            updateTrack(json.track);
            break;
        case "playlist_modified":
            updatePlaylist(json.playlist);
            break;
        case "volume_changed":
            $("#volume").slider("option", "value", json.volume);
            break;
        case "mute_toggled":
            isMute = json.mute;
            break;
        case "status":
            onStatus(json);
            break;
        default:
            break;
    }
};
var updateTime = function (secs) {
    $('#pos').html(secs.toHHMMSS());
    $("#slider").slider("option", "value", secs);
};
var updateDuration = function (secs) {
    $('#duration').html(secs.toHHMMSS());
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
//    alert('the connection has been closed')
};
var onerror = function (payload) {
    alert('A connection error occurred.')
};
