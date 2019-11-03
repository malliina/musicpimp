package controllers.musicpimp

import com.malliina.musicpimp.audio.TrackJson
import com.malliina.musicpimp.db.{DataTrack, FullText, Indexer}
import com.malliina.musicpimp.html.PimpHtml
import com.malliina.musicpimp.http.PimpContentController
import controllers.musicpimp.SearchPage.{LimitKey, TermKey}
import play.api.libs.json.Json
import play.api.mvc.Results

import scala.util.Try

object SearchPage {
  val LimitKey = "limit"
  val PlaceHolder = "artist, album or track..."
  val TermKey = "term"
}

class SearchPage(tags: PimpHtml, s: Search, indexer: Indexer, fullText: FullText, auth: AuthDeps)
  extends HtmlController(auth) {

  def search = pimpActionAsync { req =>
    def query(key: String) = (req getQueryString key) filter (_.nonEmpty)

    val term = query(TermKey)
    val limit = query(LimitKey)
      .filter(i => Try(i.toInt).isSuccess)
      .map(_.toInt) getOrElse Search.DefaultLimit
    val results = term.fold(fut(Seq.empty[DataTrack]))(fullText.fullText(_, limit))
    results.map { tracks =>
      PimpContentController.default.respond(req)(
        html = tags.search(term, tracks, req.user),
        json = Json.toJson(tracks.map(t => TrackJson.makeFull(t, req)))
      )
    }
  }

  def refresh = pimpAction {
    indexer.indexAndSave()
    Results.Ok
  }
}
