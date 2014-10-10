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

  val socketObserver = indexingObserver(broadcastStatus, (msg, _) => broadcastStatus(msg), broadcastStatus)
  val loggingObserver = indexingObserver(log.debug, (msg, t) => log.error(msg, t), log.info)

  private def indexingObserver(onNext: String => Unit,
                               onErr: (String, Throwable) => Unit,
                               onCompleted: String => Unit) = Observer[Long](
    (next: Long) => onNext(s"Indexing... $next files indexed..."),
    (t: Throwable) => onErr("Indexing failed.", t),
    () => onCompleted(s"Indexed ${PimpDb.trackCount} files."))

  def search = PimpAction(implicit req => {
    def query(key: String) = (req getQueryString key) filter (_.nonEmpty)
    val term = query("term")
    val limit = query("limit").filter(i => Try(i.toInt).isSuccess).map(_.toInt) getOrElse DEFAULT_LIMIT
    val results = term.fold(Seq.empty[DataTrack])(databaseSearch(_, limit))
    respond(html = views.html.search(term, results), json = Json.toJson(results))
  })

  def refresh = PimpAction(implicit req => {
    observeRefresh(loggingObserver, socketObserver)
    Ok
  })

  override def openSocketCall: Call = routes.Search.openSocket()

  override def welcomeMessage(client:Client): Option[Message] = Some(JsonMessages.searchStatus(s"Files indexed: ${PimpDb.trackCount}"))

  override def onMessage(msg: Message, client: Client): Unit =
    (msg \ CMD).asOpt[String].fold(log warn s"Unknown message: $msg")({
      case REFRESH =>
        broadcastStatus("Indexing...")
        observeRefresh(loggingObserver, socketObserver)
    })

  def broadcastStatus(message: String) = broadcast(JsonMessages.searchStatus(message))

  def observeRefresh(observers: Observer[Long]*) = {
    log debug s"Got refresh command..."
    val observable = Indexer.index()
    val subs = observers map observable.subscribe
    toFuture(observable).onComplete(_ => subs.foreach(_.unsubscribe()))
  }

  private def databaseSearch(query: String, limit: Int): Seq[DataTrack] = PimpDb.fullText(query, limit)

  private def toFuture[T](obs: Observable[T]): Future[Unit] = {
    val p = Promise[Unit]()
    val sub = obs.subscribe(_ => (), error => p.failure(error), () => p.success(()))
    val ret = p.future
    ret.onComplete(_ => sub.unsubscribe())
    ret
  }
}


