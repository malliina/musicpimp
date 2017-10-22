package com.malliina.musicpimp.html

import com.malliina.musicpimp.db.DataTrack
import com.malliina.musicpimp.js.SearchStrings
import com.malliina.play.tags.All._
import controllers.musicpimp.routes

import scalatags.Text.all._

object SearchHtml extends SearchStrings {
  def searchContent(query: Option[String], results: Seq[DataTrack]) = Seq(
    headerRow()("Search"),
    row(
      div4(
        searchForm(None, "")
      ),
      divClass(s"$ColMd4 $ColMdOffset4")(
        button(`type` := Button, `class` := s"$BtnDefault $BtnLg", id := RefreshButton)(glyphIcon("refresh"), " "),
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
          h3(s"No results for '$term'.")
        }
      }
    )
  )

  def searchForm(query: Option[String] = None, formClass: String, size: String = InputGroupLg) =
    form(action := routes.SearchPage.search(), role := Search, `class` := formClass)(
      divClass(s"$InputGroup $size")(
        input(`type` := Text, `class` := FormControl, placeholder := query.getOrElse("artist, album or track..."), name := "term", id := TermId),
        divClass(InputGroupBtn)(
          defaultSubmitButton(glyphIcon("search"))
        )
      )
    )
}