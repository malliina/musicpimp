package controllers

import com.mle.musicpimp.db.{DataTrack, Indexer, PimpDb}
import com.mle.musicpimp.json.JsonMessages
import com.mle.musicpimp.json.JsonStrings.{CMD, REFRESH}
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import com.mle.util.Log
import play.api.libs.json.Json
import play.api.mvc.Call
import rx.lang.scala.{Observable, Observer}

import scala.concurrent.{Future, Promise}
import scala.util.Try

/**
 * @author Michael
 */
object Search extends PimpSocket with Log {
  val DEFAULT_LIMIT = 1000

  val socketObserver = Observer[Long](
    (next: Long) => broadcastStatus(s"Indexing... $next files indexed..."),
    (t: Throwable) => broadcastStatus(s"Indexing failed."),
    () => broadcastStatus("Indexing complete."))
  val loggingObserver = Observer[Long](
    (next: Long) => log info s"Indexing... $next files indexed...",
    (t: Throwable) => log.error(s"Indexing failed.", t),
    () => log info s"Indexing complete.")

  def search = PimpAction(implicit req => {
    val query = req.getQueryString("term").filter(_.nonEmpty)
    val limit = req.getQueryString("limit").filter(i => Try(i.toInt).isSuccess).map(_.toInt) getOrElse DEFAULT_LIMIT
    val results = query.fold(Seq.empty[DataTrack])(databaseSearch(_, limit))
    respond(html = views.html.search(query, results), json = Json.toJson(results))
  })

  def refresh = PimpAction(implicit req => {
    observeRefresh(loggingObserver)
    Ok
  })

  override def welcomeMessage: Option[Message] = Some(JsonMessages.searchStatus(s"Files indexed: ${PimpDb.trackCount}"))

  override def onMessage(msg: Message, client: Client): Unit =
    (msg \ CMD).asOpt[String].fold(log warn s"Unknown message: $msg")({
      case REFRESH =>
        broadcastStatus("Indexing...")
        observeRefresh(socketObserver, loggingObserver)
    })

  private def toFuture[T](obs: Observable[T]): Future[Unit] = {
    val p = Promise[Unit]()
    val sub = obs.subscribe(_ => (), error => p.failure(error), () => p.success(()))
    val ret = p.future
    ret.onComplete(_ => sub.unsubscribe())
    ret
  }

  def broadcastStatus(message: String) = broadcast(JsonMessages.searchStatus(message))

  def observeRefresh(observers: Observer[Long]*) = {
    val observable = Indexer.index()
    val subs = observers map observable.subscribe
    toFuture(observable).onComplete(_ => subs.foreach(_.unsubscribe()))
  }

  private def databaseSearch(query: String, limit: Int): Seq[DataTrack] = PimpDb.fullText(query, limit)

  override def openSocketCall: Call = routes.Search.openSocket()
}


