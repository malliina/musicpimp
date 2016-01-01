package controllers

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.musicpimp.db.Indexer
import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.json.JsonStrings.{CMD, REFRESH, SUBSCRIBE}
import com.malliina.play.Authenticator
import com.malliina.util.Log
import play.api.mvc.Call
import rx.lang.scala.{Observable, Observer}

import scala.concurrent.{Future, Promise}

/**
  * @author Michael
  */
object Search {
  val DefaultLimit = 1000
}

class Search(indexer: Indexer, auth: Authenticator) extends PimpSockets(auth) with Log {

  val socketBroadcaster = indexingObserver(broadcastStatus, (msg, _) => broadcastStatus(msg), broadcastStatus)
  val loggingObserver = indexingObserver(log.debug, (msg, t) => log.error(msg, t), log.debug)
  val subscription = indexer.ongoing.subscribe(op => subscribeUntilComplete(op, socketBroadcaster, loggingObserver))

  private def indexingObserver(onNext: String => Unit,
                               onErr: (String, Throwable) => Unit,
                               onCompleted: String => Unit) = Observer[Long](
    (next: Long) => onNext(s"Indexing... $next files indexed..."),
    (t: Throwable) => onErr("Indexing failed.", t),
    () => onCompleted(s"Indexing complete."))

  override def openSocketCall: Call = routes.Search.openSocket()

  override def welcomeMessage(client: Client): Option[Message] = Some(JsonMessages.searchStatus(s"Connected."))

  override def onMessage(msg: Message, client: Client): Boolean = {
    (msg \ CMD).asOpt[String].fold(log warn s"Unknown message: $msg")({
      case REFRESH =>
        broadcastStatus("Indexing...")
        indexer.indexAndSave()
      case SUBSCRIBE =>
        ()
    })
    true
  }

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


