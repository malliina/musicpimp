package com.malliina.musicpimp.html

import com.malliina.musicpimp.db.DataTrack
import com.malliina.musicpimp.js.SearchStrings
import controllers.musicpimp.routes
import scalatags.Text.all._

object SearchHtml extends PimpBootstrap with SearchStrings {

  import tags._

  val verticalMargin = "mt-2 mb-4"

  def searchContent(query: Option[String], results: Seq[DataTrack]) = modifier(
    headerRow("Search"),
    row(
      div6(searchForm()),
      div6(
        button(`type` := Button, `class` := s"${btn.info} $verticalMargin", id := RefreshButton)(
          iconic("reload"),
          " "
        ),
        span(id := IndexInfo, `class` := "search-index-info")
      )
    ),
    fullRow(
      if (results.nonEmpty) {
        headeredTable(tables.stripedHover, Seq("Track", "Artist", "Album", "Actions"))(
          tbody(
            results.map(
              track =>
                tr(
                  Seq(
                    td(track.title),
                    td(track.artist),
                    td(track.album),
                    td(`class` := "table-button")(
                      LibraryHtml
                        .trackActions(track.id, trackExtra = btn.sm, extraClass = Option("flex"))()
                    )
                  )
                )
            )
          )
        )
      } else {
        query.fold(empty) { term =>
          h4(`class` := "small-heading")(s"No results for '$term'.")
        }
      }
    )
  )

  def searchForm(query: Option[String] = None) =
    form(action := routes.SearchPage.search(), role := Search, `class` := "form")(
      divClass("form-row")(
        divClass("col-8 col-md-8")(
          input(
            `type` := Search,
            `class` := s"$FormControl $verticalMargin mr-sm-2",
            placeholder := query.getOrElse("artist, album or track..."),
            name := "term",
            id := TermId,
            aria.label := "Search"
          )
        ),
        divClass("col")(
          button(`type` := Submit, `class` := s"${btnOutline.success} $verticalMargin")("Search")
        )
      )
    )

  def navbarSearch(query: Option[String] = None) =
    form(action := routes.SearchPage.search(), role := Search, `class` := "form-inline")(
      input(
        `type` := Search,
        `class` := s"$FormControl mr-sm-2",
        placeholder := query.getOrElse("artist, album or track..."),
        name := "term",
        id := TermId,
        aria.label := "Search"
      ),
      button(`type` := Submit, `class` := s"${btnOutline.success} my-2 my-sm-0")("Search")
    )
}
