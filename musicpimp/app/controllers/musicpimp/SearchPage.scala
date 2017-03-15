package controllers.musicpimp

import akka.stream.Materializer
import com.malliina.musicpimp.db.{DataTrack, Indexer, PimpDb}
import com.malliina.musicpimp.tags.PimpHtml
import com.malliina.play.CookieAuthenticator
import controllers.musicpimp.SearchPage.{LimitKey, TermKey}
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.util.Try

class SearchPage(tags: PimpHtml,
                 s: Search,
                 indexer: Indexer,
                 db: PimpDb,
                 auth: CookieAuthenticator,
                 mat: Materializer)
  extends HtmlController(auth, mat) {

  def search = pimpActionAsync { req =>
    def query(key: String) = (req getQueryString key) filter (_.nonEmpty)

    val term = query(TermKey)
    val limit = query(LimitKey).filter(i => Try(i.toInt).isSuccess).map(_.toInt) getOrElse Search.DefaultLimit
    val results = term.fold(fut(Seq.empty[DataTrack]))(databaseSearch(_, limit))
    results.map { tracks =>
      respond(req)(
        html = tags.search(term, tracks, req.user),
        json = Json.toJson(tracks)
      )
    }
  }

  def refresh = pimpAction {
    indexer.indexAndSave()
    Ok
  }

  private def databaseSearch(query: String, limit: Int): Future[Seq[DataTrack]] =
    db.fullText(query, limit)
}

object SearchPage {
  val PlaceHolder = "artist, album or track..."
  val TermKey = "term"
  val LimitKey = "limit"
}
