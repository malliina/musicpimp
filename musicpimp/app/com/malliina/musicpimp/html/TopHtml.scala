package com.malliina.musicpimp.html

import com.malliina.musicpimp.html.PimpBootstrap._
import com.malliina.musicpimp.html.PimpBootstrap.tags._
import com.malliina.musicpimp.stats._

import scalatags.Text.all._

abstract class TableRenderer[T <: TopEntry](title: String) {
  def headers: Seq[Modifier]

  def cells(row: T): Seq[Modifier]

  def render(list: ListLike[T]): Modifier = modifier(
    headerRow(title),
    div(`class` := "sub-header")(
      div(s"showing ${list.from} - ${list.until - 1}")
    ),
    fullRow(
      table(`class` := s"${tables.defaultClass} ${TopHtml.TableClass}")(
        thead(headers),
        tbody(list.entries.map(entry => tr(cells(entry))))
      )
    ),
    nav(aria.label := "Popular and recent tracks")(
      ulClass("pagination")(
        if (list.from > 0) pageLink(list.prev, "Previous") else empty,
        " ",
        if (list.entries.length >= list.meta.maxItems) pageLink(list.next, "Next") else empty
      )
    )
  )

  def pageLink[V: AttrValue](url: V, text: String) =
    li(`class` := "page-item")(a(href := url, `class` := "page-link")(text))
}

object TopHtml {
  val TableClass = "top-table"
  val HiddenXxs = "hidden-xxs"

  def toRow[T <: TopEntry](entry: T, fourth: T => Modifier) = Seq(
    td(entry.track.title),
    td(entry.track.artist, `class` := HiddenXxs),
    td(`class` := HiddenXxs)(entry.track.album),
    td(fourth(entry), `class` := HiddenXxs),
    td(LibraryHtml.trackActions(entry.track.id, extraClass = Option("flex"))())
  )

  object popular extends TableRenderer[FullPopularEntry]("Most Played") {
    override def headers: Seq[Modifier] = Seq(
      th("Title", `class` := "top-popular"),
      th("Artist", `class` := s"top-popular $HiddenXxs"),
      th(`class` := s"$HiddenXxs top-popular")("Album"),
      th("Plays", `class` := s"plays $HiddenXxs"),
      th("Actions", `class` := "actions")
    )

    override def cells(row: FullPopularEntry): Seq[Modifier] =
      toRow[FullPopularEntry](row, _.playbackCount)
  }

  object recent extends TableRenderer[FullRecentEntry]("Most Recent") {
    override def headers = Seq(
      th("Title"),
      th("Artist", `class` := HiddenXxs),
      th(`class` := HiddenXxs)("Album"),
      th(`class` := s"cell-content when $HiddenXxs")("When"),
      th("Actions", `class` := "actions")
    )

    override def cells(row: FullRecentEntry) =
      toRow[FullRecentEntry](row, _.whenFormatted)
  }

  def mostRecentContent(list: RecentList) = recent.render(list)

  def mostPopular(list: PopularList) = popular.render(list)
}
