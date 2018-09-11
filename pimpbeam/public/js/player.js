var player = $("#player");
var playerHtml = player[0];
var cover = $("#cover");
var playbackDiv = $("#playback");
//var status = $("#status");
var splash = $("#splash");
var qrImage = $("#qr");
var initial = $("#initial");
var webSocket;

var onconnect = function () {
    player.bind("timeupdate", function () {
        send({event: "time_updated", pos_seconds: playerHtml.currentTime, position: playerHtml.currentTime});
    });
    player.bind("ended", function () {
        sendStateChanged("Ended");
    });
    player.bind("playing", function () {
        sendStateChanged("Started");
    });
    player.bind("pause", function () {
        sendStateChanged("Stopped");
    });
    player.bind("waiting", function () {
        sendStateChanged("Buffering");
    });
    player.bind("seeking", function () {
        sendStateChanged("Seeking");
    });
    player.bind("seeked", function () {
        sendStateChanged("Seeked");
    });
    player.bind("emptied", function () {
        sendStateChanged("Emptied");
    });
    player.bind("suspend", function () {
        send({event: "suspended_getting_data", state: "Suspended"});
    });
    player.bind("volumechange", function () {
        send({event: "volume_changed", volume: Math.round(100.0 * playerHtml.volume)});
        // volumechange is fired when the HTML5 player is muted
        send({event: "mute_toggled", mute: playerHtml.muted});
    });
//    setStatus("Waiting for mobile device to connect...");
    splash.removeClass("hidden");
    initial.hide();
    document.getElementById("qr").src = "image";
};

var setStatus = function (newStatus) {
    document.getElementById("status").innerHTML = newStatus;
};
var onclose = function () {
    setStatus("The connection has been closed.");
};

var onerror = function () {
    setStatus("An error occurred.");
};

var playerState = function () {
    if (playerHtml.paused) {
        return "Paused";
    } else {
        return "Playing";
    }
};
var statusJson = function () {
    return {
        event: "short_status",
        state: playerState(),
        pos_seconds: playerHtml.currentTime,
        position: playerHtml.currentTime,
        mute: playerHtml.muted,
        volume: Math.round(100.0 * playerHtml.volume)
    }
};
var sendStateChanged = function (newState) {
    send({event: "playstate_changed", state: newState});
};

// user input handlers
var stop = function () {
    playerHtml.pause();
};
var resume = function () {
    playerHtml.play();
};
var vol = function (level) {
    playerHtml.volume = level / 100;
};
var setMute = function (trueOrFalse) {
    playerHtml.muted = trueOrFalse;
    send({event: "mute_toggled", mute: playerHtml.muted});
};
var seek = function (pos) {
    playerHtml.currentTime = pos;
};
var reset = function () {
    playerHtml.pause();
    // reloads stream
    player.attr("src", "");
    player.attr("src", "stream");
    playerHtml.pause();
    playerHtml.play();
};
var preparePlayback = function () {
    splash.hide();
    playbackDiv.removeClass("hidden");
};
var showCover = function (uri) {
    if (uri == "none") {
        uri = "assets/img/guitar.png";
    }
    cover.attr("src", uri);
};
var handleTrackChanged = function (track) {
    var artistEnc = encodeURIComponent(track.artist);
    var albumEnc = encodeURIComponent(track.album);
    var coverResource = "/covers/" + artistEnc + "/" + albumEnc;
    cover.attr("src", coverResource);
};

// sends json back to server
var send = function (json) {
    webSocket.send(JSON.stringify(json));
};

// server message handlers
var onmessage = function (payload) {
    var json = jQuery.parseJSON(payload.data);
    switch (json.cmd) {
        case "stop":
            stop();
            break;
        case "resume":
            resume();
            break;
        case "volume":
            vol(json.value);
            break;
        case "mute":
            setMute(json.value);
            break;
        case "seek":
            seek(json.value);
            break;
        case "reset":
            reset();
            break;
        case "connected":
            preparePlayback();
            send(statusJson());
            break;
        case "cover":
            showCover(json.value);
            break;
        default:
            break;
    }
    switch (json.event) {
        case "track_changed":
            handleTrackChanged(json.track);
            break;
        case "cover_available":
            break;
        case "ping":
            break;
        default:
            break;
    }
};

function wsProto(location) {
    if (location.protocol == "http:") {
        return "ws";
    } else {
        return "wss";
    }
}

function initSocket() {
    var location = window.location;
    var proto = wsProto(location);
    var url = proto + "://" + location.host + "/ws/player?f=json";
    webSocket = new WebSocket(url);
    webSocket.onopen = onconnect;
    webSocket.onmessage = onmessage;
    webSocket.onclose = onclose;
    webSocket.onerror = onerror;
}

$(document).ready(function () {
    initSocket();
});
