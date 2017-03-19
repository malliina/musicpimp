package com.malliina.musicpimp.tags

import com.malliina.play.tags.All._
import controllers.musicpimp.{UserFeedback, routes}
import controllers.routes.Assets.at

import scalatags.Text.all._

object PlayerHtml {
  def playerContent(feedback: Option[UserFeedback]) = row(
    divClass(ColMd6)(
      headerDiv(h1("Player")),
      div(id := "playerDiv", style := "display: none")(
        feedback.fold(empty) { fb =>
          div(id := "feedback")(PimpHtml.feedbackDiv(fb))
        },
        fullRow(
          pClass(Lead, id := "notracktext")(
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
      pClass(Lead, id := "empty_playlist_text")("The playlist is empty."),
      ol(id := "playlist")
    )
  )

  def playerCtrl: Modifier = {
    val centerAttr = `class` := "track-meta"
    Seq(
      fullRow(
        h2(id := "title", centerAttr)("No track"),
        h4(id := "album", centerAttr),
        h3(id := "artist", centerAttr)
      ),
      fullRow(
        divClass(ColMd11)(
          div(id := "slider")
        ),
        divClass(s"$Row $ColMd11", id := "progress")(
          span(id := "pos")("00:00"), " / ", span(id := "duration")("00:00")
        ),
        divClass(s"$Row $ColMd11 text-center")(
          imageInput(at("img/transport.rew.png"), id := "prevButton"),
          imageInput(at("img/light/transport.play.png"), id := "playButton"),
          imageInput(at("img/light/transport.pause.png"), id := "pauseButton", style := "display: none"),
          imageInput(at("img/light/transport.ff.png"), id := "nextButton")
        ),
        divClass(s"$Row $ColMd11 $VisibleLg")(
          divClass(ColMd3)(
            imageInput(at("img/light/appbar.sound.3.png"), id := "volumeButton", `class` := PullRight)
          ),
          divClass(ColMd8)(
            divClass(s"$ColMd12 $PullLeft", id := "volume")
          )
        )
      )
    )
  }
}
