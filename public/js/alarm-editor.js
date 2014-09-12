$(document).ready(function () {
    updateEveryDayCheckBox();
    // autocomplete for tracks
//    $.get("/tracks", function (tracks) {
//        var trackIDs = jQuery.map(tracks, function (n, i) {
//            return {
//                label: n.title,
//                value: n.title,
//                id: n.id
//            };
//        });
    $(".selector").autocomplete({
        source: function (request, response) {
            $.getJSON("/search?f=json", request, function (data, status, xhr) {
                response($.map(data, function (v, i) {
                    return {
                        label: v.artist + " - " + v.title,
                        value: v.artist + " - " + v.title,
                        id: v.id
                    }
                }));
            });
        },
        minLength: 3,
        select: function (event, ui) {
            var item = ui.item;
            // stores the track ID in a hidden field
            $("#track_id").val(item.id);
        }
    });
//    });
});