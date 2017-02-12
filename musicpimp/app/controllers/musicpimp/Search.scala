package controllers.musicpimp

import com.malliina.musicpimp.db.Indexer
import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.json.JsonStrings.{Cmd, Refresh, Subscribe}
import controllers.musicpimp.Search.log
import play.api.Logger
import play.api.mvc.Call
import rx.lang.scala.{Observable, Observer}

import scala.concurrent.{Future, Promise}

object Search {
  private val log = Logger(getClass)
  val DefaultLimit = 1000
}

class Search(indexer: Indexer, security: SecureBase)
  extends PimpSockets(security) {

  val socketBroadcaster = indexingObserver(
    msg => broadcastStatus(msg),
    (msg, _) => broadcastStatus(msg),
    compl => broadcastStatus(compl))
  val loggingObserver = indexingObserver(
    msg => log.debug(msg),
    (msg, t) => log.error(msg, t),
    compl => log.debug(compl))
  val subscription = indexer.ongoing.subscribe(op => subscribeUntilComplete(op, socketBroadcaster, loggingObserver))

  private def indexingObserver(onNext: String => Unit,
                               onErr: (String, Throwable) => Unit,
                               onCompleted: String => Unit) = Observer[Long](
    (next: Long) => onNext(s"Indexing... $next files indexed..."),
    (t: Throwable) => onErr("Indexing failed.", t),
    () => onCompleted(s"Indexing complete."))

  override def openSocketCall: Call = routes.Search.openSocket()

  override def welcomeMessage(client: Client): Option[Message] =
    Some(JsonMessages.searchStatus(""))

  override def onMessage(msg: Message, client: Client): Boolean = {
    (msg \ Cmd).asOpt[String].fold(log warn s"Unknown message: $msg")({
      case Refresh =>
        broadcastStatus("Indexing...")
        indexer.indexAndSave()
      case Subscribe =>
        ()
    })
    true
  }

  def broadcastStatus(message: String): Unit = broadcast(JsonMessages.searchStatus(message))

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
