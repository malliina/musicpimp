package controllers

import com.mle.concurrent.ExecutionContexts.cached
import com.mle.musicpimp.db.{Indexer, PimpDb}
import com.mle.musicpimp.json.JsonMessages
import com.mle.musicpimp.json.JsonStrings.{CMD, REFRESH, SUBSCRIBE}
import com.mle.util.Log
import play.api.mvc.Call
import rx.lang.scala.{Observable, Observer}

import scala.concurrent.{Future, Promise}

/**
 * @author Michael
 */
object Search {
  val DEFAULT_LIMIT = 1000
}

class Search extends PimpSockets with Log {

  val socketBroadcaster = indexingObserver(broadcastStatus, (msg, _) => broadcastStatus(msg), broadcastStatus)
  val subscription = Indexer.ongoing.subscribe(op => subscribeUntilComplete(op, socketBroadcaster))
  val loggingObserver = indexingObserver(log.debug, (msg, t) => log.error(msg, t), log.info)

//   call this to ensure that this object is initialized, to ensure that we subscribe to indexer operations
//  def init(): Unit = ()

  private def indexingObserver(onNext: String => Unit,
                               onErr: (String, Throwable) => Unit,
                               onCompleted: String => Unit) = Observer[Long](
    (next: Long) => onNext(s"Indexing... $next files indexed..."),
    (t: Throwable) => onErr("Indexing failed.", t),
    () => onCompleted(s"Indexed ${PimpDb.trackCount} files."))

  override def openSocketCall: Call = routes.Search.openSocket()

  override def welcomeMessage(client: Client): Option[Message] = Some(JsonMessages.searchStatus(s"Files indexed: ${PimpDb.trackCount}"))

  override def onMessage(msg: Message, client: Client): Boolean = {
    (msg \ CMD).asOpt[String].fold(log warn s"Unknown message: $msg")({
      case REFRESH =>
        broadcastStatus("Indexing...")
        Indexer.indexAndSave()
      case SUBSCRIBE =>
        ()
    })
    true
  }

  override def onDisconnect(client: Client): Unit = super.onDisconnect(client)

  def broadcastStatus(message: String) = broadcast(JsonMessages.searchStatus(message))

  def subscribeUntilComplete[T](observable: Observable[T], observers: Observer[T]*) = {
    val subs = observers map observable.subscribe
    toFuture(observable).onComplete(_ => subs.foreach(_.unsubscribe()))
  }

  private def toFuture[T](obs: Observable[T]): Future[Unit] = {
    val p = Promise[Unit]()
    val sub = obs.subscribe(_ => (), error => p.failure(error), () => p.success(()))
    val ret = p.future
    ret.onComplete(_ => sub.unsubscribe())
    ret
  }
}


