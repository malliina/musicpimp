package controllers

import com.mle.musicpimp.db.{DataTrack, PimpDb}
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import com.mle.util.Log
import play.api.libs.json.Json
import rx.lang.scala.Observable

import scala.concurrent.{Future, Promise}

/**
 * @author Michael
 */
object Search extends Secured with Log {
  def search = PimpAction(implicit req => {
    val query = req.getQueryString("query").filter(_.nonEmpty)
    val results = query.fold(Seq.empty[DataTrack])(databaseSearch)
    respond(html = views.html.search(query, results), json = Json.toJson(results))
  })

  def refresh = PimpAction {
    val observable = PimpDb.refreshIndex()
    val sub = observable.subscribe(
      next => log info s"Files handled: $next",
      err => log.error(s"Refresh error.", err),
      () => log info s"Refresh complete.")
    toFuture(observable).onComplete(_ => sub.unsubscribe())
    Ok
  }

  private def toFuture[T](obs: Observable[T]): Future[Unit] = {
    val p = Promise[Unit]()
    val sub = obs.subscribe(_ => (), error => p.failure(error), () => p.success(()))
    val ret = p.future
    ret.onComplete(_ => sub.unsubscribe())
    ret
  }

  private def databaseSearch(query: String): Seq[DataTrack] = PimpDb.fullText(query)
}


