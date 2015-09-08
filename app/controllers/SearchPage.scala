package controllers

import com.mle.musicpimp.db.{PimpDb, Indexer, DataTrack}
import play.api.libs.json.Json

import scala.util.Try

/**
 * @author mle
 */
class SearchPage(s: Search) extends HtmlController {
  def search = PimpAction(implicit req => {
    def query(key: String) = (req getQueryString key) filter (_.nonEmpty)
    val term = query("term")
    val limit = query("limit").filter(i => Try(i.toInt).isSuccess).map(_.toInt) getOrElse Search.DEFAULT_LIMIT
    val results = term.fold(Seq.empty[DataTrack])(databaseSearch(_, limit))
    respond(html = views.html.search(term, results, s.wsUrl(req)), json = Json.toJson(results))
  })

  def refresh = PimpAction(implicit req => {
    Indexer.indexAndSave()
    Ok
  })

  private def databaseSearch(query: String, limit: Int): Seq[DataTrack] = PimpDb.fullText(query, limit)

}
