package com.malliina.musicpimp.tags

import com.malliina.musicpimp.js.PlayerStrings
import com.malliina.play.tags.All._
import controllers.musicpimp.{UserFeedback, routes}
import controllers.routes.Assets.at

import scalatags.Text.all._

object PlayerHtml extends PlayerStrings {
  def playerContent(feedback: Option[UserFeedback]) = row(
    divClass(ColMd6)(
      headerDiv(h1("Player")),
      div(id := PlayerDivId, style := "display: none")(
        feedback.fold(empty) { fb =>
          div(id := FeedbackId)(PimpHtml.feedbackDiv(fb))
        },
        fullRow(
          pClass(Lead, id := NoTrackTextId)(
            "No track. Play one from the ",
            aHref(routes.LibraryController.rootLibrary())("library"),
            " or stream from a mobile device."
          )
        ),
        playerCtrl
      )
    ),
    divClass(ColMd6)(
      headerDiv(h1("Playlist")),
      pClass(Lead, id := EmptyPlaylistText)("The playlist is empty."),
      ol(id := PlaylistId)
    )
  )

  def playerCtrl: Modifier = {
    val centerAttr = `class` := "track-meta"
    Seq(
      fullRow(
        h2(id := TitleId, centerAttr)("No track"),
        h4(id := AlbumId, centerAttr),
        h3(id := ArtistId, centerAttr)
      ),
      fullRow(
        divClass(ColMd11)(
          div(id := SliderId)
        ),
        divClass(s"$Row $ColMd11", id := ProgressId)(
          span(id := PositionId)("00:00"), " / ", span(id := DurationId)("00:00")
        ),
        divClass(s"$Row $ColMd11 text-center")(
          imageInput(at("img/transport.rew.png"), id := PrevButton),
          imageInput(at("img/light/transport.play.png"), id := PlayButton),
          imageInput(at("img/light/transport.pause.png"), id := PauseButton, style := "display: none"),
          imageInput(at("img/light/transport.ff.png"), id := NextButton)
        ),
        divClass(s"$Row $ColMd11 $VisibleLg")(
          divClass(ColMd3)(
            imageInput(at("img/light/appbar.sound.3.png"), id := VolumeButton, `class` := PullRight)
          ),
          divClass(ColMd8)(
            divClass(s"$ColMd12 $PullLeft", id := VolumeId)
          )
        )
      )
    )
  }
}
