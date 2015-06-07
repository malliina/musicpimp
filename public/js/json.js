var statusJson = function () {
    return toJson("status")
};
var prevJson = function () {
    return toJson("prev")
};
var stopJson = function () {
    return toJson("stop")
};
var resumeJson = function () {
    return toJson("resume")
};
var nextJson = function () {
    return toJson("next")
};
var muteJson = function (muteOrNot) {
    return valuedJson("mute", muteOrNot)
};
var seekJson = function (pos) {
    return valuedJson("seek", pos)
};
var skipJson = function (index) {
    return valuedJson("skip", index)
};
var removeJson = function (index) {
    return valuedJson("remove", index)
};
var volJson = function (level) {
    return valuedJson("volume", level)
};
var addJson = function (id) {
    return trackJson("add", id)
};
var playJson = function (id) {
    return trackJson("play", id)
};
var addItemsJson = function(fs, ts) {
    return itemsJson("add_items", fs, ts)
};
var playItemsJson = function(fs, ts) {
    return itemsJson("play_items", fs, ts)
};
var toJson = function (cmd) {
    return JSON.stringify({cmd: cmd})
};
var valuedJson = function (cmd, param) {
    return JSON.stringify({cmd: cmd, value: param})
};
var trackJson = function (cmd, track) {
    return JSON.stringify({cmd: cmd, track: track})
};
var itemsJson = function (cmd, fs, ts) {
    return JSON.stringify({cmd: cmd, folders: fs, tracks: ts})
};

// Assuming a number is a duration in seconds,
// returns the duration in format "HH:mm:ss".
// Adapted from http://stackoverflow.com/a/6313008.
Number.prototype.toHHMMSS = function () {
    var sec_num = parseInt(this, 10);
    var hours = Math.floor(sec_num / 3600);
    var minutes = Math.floor((sec_num - (hours * 3600)) / 60);
    var seconds = sec_num - (hours * 3600) - (minutes * 60);
    var showHours = hours > 0;
    if (hours < 10) {
        hours = "0" + hours;
    }
    if (minutes < 10) {
        minutes = "0" + minutes;
    }
    if (seconds < 10) {
        seconds = "0" + seconds;
    }
    if (showHours) {
        return hours + ':' + minutes + ':' + seconds;
    } else {
        return minutes + ':' + seconds;
    }
};