var loadingAjax = $(".loading");

$(document).ready(function () {
    updateEveryDayCheckBox();
    // autocomplete for tracks
    loadingAjax.show();
    $.get("/tracks", function (tracks) {
        var trackIDs = jQuery.map(tracks, function (n, i) {
            return {
                label: n.title,
                value: n.id
            };
        });
        loadingAjax.hide();
        $(".selector").autocomplete({
            source: trackIDs,
            minLength: 3
        });
    });
});