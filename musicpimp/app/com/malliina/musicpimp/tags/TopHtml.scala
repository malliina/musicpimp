package com.malliina.musicpimp.tags

import com.malliina.musicpimp.stats._
import com.malliina.play.models.Username
import com.malliina.play.tags.All._

import scalatags.Text.all._

object TopHtml {
  def mostRecentContent(list: RecentList) =
    topListContent[RecentLike](list, "Most Recent", list.recents, "When", _.whenFormatted)

  def mostPopular(list: PopularList) =
    topListContent[PopularLike](list, "Most Played", list.populars, "Plays", _.playbackCount)

  def topListContent[T <: TopEntry](list: ListLike,
                                    headerText: String,
                                    entries: Seq[T],
                                    fourthHeader: String,
                                    fourthValue: T => Modifier) = Seq(
    headerRow()(s"$headerText ", small(`class` := HiddenXs)(s"by ${list.username}")),
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
