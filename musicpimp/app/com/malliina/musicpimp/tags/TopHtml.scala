package com.malliina.musicpimp.tags

import com.malliina.musicpimp.stats._
import com.malliina.play.models.Username
import com.malliina.play.tags.All._

import scalatags.Text.all._

object TopHtml {
  def mostRecentContent(entries: Seq[RecentEntry], username: Username) =
    topListContent[RecentLike](username, "Most recent", entries, "When", _.whenFormatted)

  def mostPopular(entries: Seq[PopularLike], username: Username) =
    topListContent[PopularLike](username, "Most popular", entries, "Plays", _.playbackCount)

  def topListContent[T <: TopEntry](username: Username,
                                    headerText: String,
                                    entries: Seq[T],
                                    fourthHeader: String,
                                    fourthValue: T => Modifier) = Seq(
    headerRow()(s"$headerText ", small(`class` := HiddenXs)(s"by ${username.name}")),
    fullRow(
      responsiveTable(entries)("Title", "Artist", "Album", fourthHeader, "Actions") { entry =>
        Seq(
          td(entry.track.title),
          td(entry.track.artist),
          td(entry.track.album),
          td(fourthValue(entry)),
          td(LibraryHtml.trackActions(entry.track.id, Option("flex"))())
        )
      }
    )
  )
}
