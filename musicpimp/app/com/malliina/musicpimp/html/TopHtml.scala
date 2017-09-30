package com.malliina.musicpimp.html

import com.malliina.musicpimp.stats._
import com.malliina.play.tags.All._

import scalatags.Text.all._

abstract class TableRenderer[T <: TopEntry](title: String) {
  def headers: Seq[Modifier]

  def cells(row: T): Seq[Modifier]

  def render(list: ListLike[T]): Modifier = Seq(
    headerRow()(title),
    div(`class` := "sub-header")(
      div(s"showing ${list.from} - ${list.until - 1}")
    ),
    fullRow(
      table(`class` := s"$TableStripedHoverResponsive ${TopHtml.TableClass}")(
        thead(headers),
        tbody(list.entries.map(entry => tr(cells(entry))))
      )
    ),
    fullRow(
      ulClass("pager")(
        if (list.from > 0) liHref(list.prev)("Previous") else (),
        " ",
        if (list.entries.length >= list.meta.maxItems) liHref(list.next)("Next") else ()
      )
    )
  )
}

object TopHtml {
  val TableClass = "top-table"
  val HiddenSmall = PimpHtml.HiddenSmall

  def toRow[T <: TopEntry](entry: T, fourth: T => Modifier) = Seq(
    td(entry.track.title),
    td(entry.track.artist),
    td(`class` := HiddenSmall)(entry.track.album),
    td(fourth(entry)),
    td(LibraryHtml.trackActions(entry.track.id, Option("flex"))())
  )

  object popular extends TableRenderer[FullPopularEntry]("Most Played") {
    override def headers: Seq[Modifier] = Seq(
      th("Title"),
      th("Artist"),
      th(`class` := HiddenSmall)("Album"),
      th("Plays"),
      th("Actions")
    )

    override def cells(row: FullPopularEntry): Seq[Modifier] =
      toRow[FullPopularEntry](row, _.playbackCount)
  }

  object recent extends TableRenderer[FullRecentEntry]("Most Recent") {
    override def headers = Seq(
      th("Title"),
      th("Artist"),
      th(`class` := HiddenSmall)("Album"),
      th(`class` := "cell-content")("When"),
      th("Actions")
    )

    override def cells(row: FullRecentEntry) =
      toRow[FullRecentEntry](row, _.whenFormatted)
  }

  def mostRecentContent(list: RecentList) = recent.render(list)

  def mostPopular(list: PopularList) = popular.render(list)
}
