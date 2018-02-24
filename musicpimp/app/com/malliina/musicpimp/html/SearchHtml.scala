package com.malliina.musicpimp.html

import com.malliina.musicpimp.db.DataTrack
import com.malliina.musicpimp.js.SearchStrings
import controllers.musicpimp.routes

import scalatags.Text.all._

object SearchHtml extends PimpBootstrap with SearchStrings {

  import tags._

  def searchContent(query: Option[String], results: Seq[DataTrack]) = Seq(
    headerRow("Search"),
    row(
      div6(
        searchForm()
      ),
      div6(
        button(`type` := Button, `class` := btn.info, id := RefreshButton)(iconic("reload"), " "),
        span(id := IndexInfo)
      )
    ),
    fullRow(
      if (results.nonEmpty) {
        responsiveTable(results)("Track", "Artist", "Album", "Actions") { track =>
          Seq(
            td(track.title),
            td(track.artist),
            td(track.album),
            td(LibraryHtml.trackActions(track.id, Option("flex"))())
          )
        }
      } else {
        query.fold(empty) { term =>
          h4(`class` := "small-heading")(s"No results for '$term'.")
        }
      }
    )
  )

  def searchForm(query: Option[String] = None) =
    form(action := routes.SearchPage.search(), role := Search, `class` := "form-inline")(
      input(`type` := Search, `class` := s"$FormControl mr-sm-2", placeholder := query.getOrElse("artist, album or track..."), name := "term", id := TermId, aria.label := "Search"),
      button(`type` := Submit, `class` := s"${btnOutline.success} my-2 my-sm-0")("Search")
    )
}
