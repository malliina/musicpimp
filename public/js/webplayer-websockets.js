"use strict";
var webSocket;
var playlist = [];
var playlistPos = -1;
var player = $("#player");
var playerHtml = player[0];
var playlistElem = $("#playlist");

var onconnect = function () {
    player.bind("timeupdate", function () {
        var t = playerHtml.currentTime;
        $("#slider").slider("option", "value", t);
        $("#pos").html(t.toHHMMSS());
        sendValuedJson("time_updated", playerHtml.currentTime);
    });
    player.bind("ended", function () {
        next();
    });
    player.bind("playing", function () {
        $("#playButton").hide();
        $("#pauseButton").show();
        sendValuedJson("playstate_changed", "Started");
    });
    player.bind("pause", function () {
        $("#pauseButton").hide();
        $("#playButton").show();
        sendValuedJson("playstate_changed", "Stopped");
    });
    player.bind("volumechange", function () {
        sendValuedJson("volume_changed", 100.0 * playerHtml.volume)
    });
    sendAsJson("status");
};

// user input handlers
var resume = function () {
    playerHtml.play();
};
var stop = function () {
    playerHtml.pause();
};
var next = function () {
    if (playlistPos + 1 < playlist.length) {
        playlistPos += 1;
        playAndSend(playlistPos);
    }
};
var prev = function () {
    if (playlistPos > 0 && playlistPos <= playlist.length) {
        playlistPos -= 1;
        playAndSend(playlistPos);
    }
};
var playAndSend = function (pos) {
    playTrack(playlist[pos]);
    sendValuedJson("playlist_index_changed", pos);
};
var seek = function (pos) {
    playerHtml.currentTime = pos;
};
var skip = function (index) {
    playlistPos = index;
    playTrack(playlist[index]);
};
var remove = function (index) {
    // TODO
};
var vol = function (level) {
    playerHtml.volume = level / 100;
    $("#volume").slider("value", playerHtml.volume * 100);
};
var setMute = function (trueOrFalse) {
    playerHtml.muted = trueOrFalse;
    sendValuedJson("mute_toggled", trueOrFalse);
};
var togglemute = function () {
    setMute(!playerHtml.muted);
};

var sendAsJson = function (cmd) {
    send(toJson(cmd));
};
var sendValuedJson = function (cmd, value) {
    send(valuedJson(cmd, value));
};
// sends json back to server
var send = function (json) {
    webSocket.send(json);
};
// server message handlers
var onmessage = function (payload) {
    var json = jQuery.parseJSON(payload.data);
    var event = json.event;
    if (event == "status") {
        var tracks = json.playlist;
        setPlaylist(tracks);
        if (tracks.length > 0) {
            setTrack(tracks[json.index]);
        }
    } else {
        var cmd = json.cmd;
        switch (cmd) {
            case "play":
                setPlaylist([json.track]);
                playlistPos = 0;
                playTrack(json.track);
                break;
            case "add":
                appendTrack(json.track);
                break;
            case "remove":
                removeIndex(json.value);
                break;
            case "stop":
                stop();
                break;
            case "resume":
                resume();
                break;
            case "next":
                next();
                break;
            case "prev":
                prev();
                break;
            case "skip":
                skip(json.value);
                break;
            case "volume":
                vol(json.value);
                break;
            case "seek":
                seek(json.value);
                break;
            case "mute":
                setMute(json.value);
                break;
            default:
                break;
        }
    }
};

var setTrack = function (track) {
    player.attr("src", "tracks/" + track.id);
    $("#title").html(track.title);
    $("#artist").html(track.artist);
    $("#album").html(track.album);
    $("#volume").slider("value", playerHtml.volume * 100);
    var dur = track.duration;
    $("#slider").slider("option", "max", dur);
    $("#duration").html(dur.toHHMMSS());
    send(trackJson("track_changed", track.id));
};
var playTrack = function (track) {
    setTrack(track);
    play();
};

var play = function () {
    playerHtml.play();
};

var appendTrack = function (track) {
    playlist[playlist.length] = track;
    appendToPlaylistUI(track);
};
var removeIndex = function (index) {
    // mutates playlist, removing 1 element starting from index
    playlist.splice(index, 1);
    setPlaylist(playlist);
};
var setPlaylist = function (tracks) {
    $('li').remove(".song");
    playlist = [];
    var playlistLength = tracks.length;
    for (var i = 0; i < playlistLength; ++i) {
        appendTrack(tracks[i]);
    }
};
var appendToPlaylistUI = function (track) {
    playlistElem.append("<li class='song'><a href='#' onclick='skip(IndexOf(this))'>" + track.title + "</a></li>");
};
var onclose = function (closeEvent) {
    // console.log("Closed, code " + closeEvent.code + ", reason " + closeEvent.reason + ", was clean " + closeEvent.wasClean);
    // alert('the connection has been closed')
};
var onerror = function (payload) {
    alert('A connection error occurred.');
};
// http://stackoverflow.com/q/4032654
var IndexOf = function (sender) {
    var aElements = sender.parentNode.parentNode.getElementsByTagName("a");
    var aElementsLength = aElements.length;
    var index;
    for (var i = 0; i < aElementsLength; i++) {
        if (aElements[i] == sender) {
            index = i;
//            alert("found match at " + i);
        }
    }
    return index;
};

