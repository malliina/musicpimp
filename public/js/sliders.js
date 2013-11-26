$(document).ready(function () {
    $("#slider").slider({
        stop: function (event, ui) {
            var position = $("#slider").slider("option", "value");
            seek(position)
        }
    });
    $("#volume").slider({
        orientation: "horizontal",
        range: "min",
        min: 0,
        max: 100,
        stop: function (event, ui) {
            var position = $("#volume").slider("option", "value");
            vol(position)
        }
    });
});