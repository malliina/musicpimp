$(document).ready(function () {
    updateEveryDayCheckBox();
    // autocomplete for tracks
    $.get("/tracks", function (tracks) {
        var trackIDs = jQuery.map(tracks, function (n, i) {
            return {
                label: n.title,
                value: n.title,
                id: n.id
            };
        });
        $(".selector").autocomplete({
            source: trackIDs,
            minLength: 3,
            select: function (event, ui) {
                var item = ui.item;
                // stores the track ID in a hidden field
                $("#track_id").val(item.id);
            }
        });
    });
});