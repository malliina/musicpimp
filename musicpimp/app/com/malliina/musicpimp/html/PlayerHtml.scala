package com.malliina.musicpimp.html

import com.malliina.musicpimp.assets.AppAssets
import com.malliina.musicpimp.js.PlayerStrings
import controllers.musicpimp.{UserFeedback, routes}

import scalatags.Text.all._

object PlayerHtml extends PimpBootstrap with PlayerStrings {

  import tags._

  val playerWidth = col.md.width("12")

  def playerContent(feedback: Option[UserFeedback]) = row(
    divClass(col.md.six)(
      headerDiv(h1("Player")),
      div(id := PlayerDivId, style := "display: none")(
        feedback.fold(empty) { fb =>
          div(id := FeedbackId)(PimpHtml.feedbackDiv(fb))
        },
        fullRow(
          pClass(Lead, id := NoTrackTextId)(
            "No track. Play one from the ",
            a(href := routes.LibraryController.rootLibrary())("library"),
            " or stream from a mobile device."
          )
        ),
        playerCtrl
      )
    ),
    divClass(col.md.six)(
      headerDiv(h1("Playlist")),
      pClass(Lead, id := EmptyPlaylistText)("The playlist is empty."),
      ol(id := PlaylistId, `class` := "playlist-list")
    )
  )

  def playerCtrl: Modifier = {
    val img = AppAssets.img
    val imgLight = img.light
    val centerAttr = `class` := "track-meta"
    modifier(
      div(`class` := "row")(
        divClass(s"${col.md.twelve} track-container")(
          h2(id := TitleId, centerAttr)("No track"),
          h4(id := AlbumId, centerAttr),
          h3(id := ArtistId, centerAttr)
        )),
      fullRow(
        divClass(playerWidth)(
          div(id := SliderId)
        ),
        divClass(s"$Row $playerWidth", id := ProgressId)(
          span(id := PositionId)("00:00"), span(" / "), span(id := DurationId)("00:00")
        ),
        divClass(s"$Row mx-auto justify-content-center")(
          imageInput(img.transport_rew_png, id := PrevButton),
          imageInput(imgLight.transport_play_png, id := PlayButton),
          imageInput(imgLight.transport_pause_png, id := PauseButton, style := "display: none"),
          imageInput(imgLight.transport_ff_png, id := NextButton)
        ),
        div(`class` := s"$Row $playerWidth", id := "volume-control")(
          divClass(col.md.width("3"))(
            imageInput(imgLight.appbar_sound_3_png, id := VolumeButton, `class` := PullRight)
          ),
          divClass(col.md.width("9"))(
            divClass(s"${col.md.twelve} $PullLeft", id := VolumeId)
          )
        )
      )
    )
  }
}
