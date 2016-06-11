package controllers

import akka.stream.Materializer
import com.malliina.musicpimp.db.{DataTrack, Indexer, PimpDb}
import com.malliina.play.Authenticator
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.util.Try

class SearchPage(s: Search, indexer: Indexer, db: PimpDb, auth: Authenticator, mat: Materializer)
  extends HtmlController(auth, mat) {

  def search = PimpActionAsync { implicit req => {
    def query(key: String) = (req getQueryString key) filter (_.nonEmpty)
    val term = query("term")
    val limit = query("limit").filter(i => Try(i.toInt).isSuccess).map(_.toInt) getOrElse Search.DefaultLimit
    val results = term.fold(Future.successful(Seq.empty[DataTrack]))(databaseSearch(_, limit))
    results.map { tracks =>
      respond(
        html = views.html.search(term, tracks, s.wsUrl(req)),
        json = Json.toJson(tracks)
      )
    }
  }
  }

  def refresh = PimpAction {
    indexer.indexAndSave()
    Ok
  }

  private def databaseSearch(query: String, limit: Int): Future[Seq[DataTrack]] = db.fullText(query, limit)
}

object SearchPage {
  val PlaceHolder = "artist, album or track..."
}