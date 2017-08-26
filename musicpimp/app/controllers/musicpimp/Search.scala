package controllers.musicpimp

import akka.actor.Props
import com.malliina.musicpimp.db.Indexer
import com.malliina.musicpimp.models.{Refresh, SearchMessage, SearchStatus, Subscribe}
import com.malliina.play.ActorExecution
import com.malliina.play.auth.Authenticator
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws.{ActorConfig, ActorMeta, JsonActor, Sockets}
import controllers.musicpimp.Search.{indexingObserver, log, subscribeUntilComplete}
import play.api.Logger
import play.api.libs.json.JsValue
import rx.lang.scala.{Observable, Observer, Subscription}

import scala.concurrent.{ExecutionContext, Future, Promise}

object Search {
  private val log = Logger(getClass)
  val DefaultLimit = 1000

  def indexingObserver(onNext: String => Unit,
                       onErr: (String, Throwable) => Unit,
                       onCompleted: String => Unit) = Observer[Long](
    (next: Long) => onNext(s"Indexing... $next files indexed..."),
    (t: Throwable) => onErr("Indexing failed.", t),
    () => onCompleted("Indexing complete."))

  def subscribeUntilComplete[T](observable: Observable[T], observers: Observer[T]*)(implicit ec: ExecutionContext) = {
    val subs = observers map observable.subscribe
    toFuture(observable).onComplete(_ => subs.foreach(_.unsubscribe()))
  }

  private def toFuture[T](obs: Observable[T])(implicit ec: ExecutionContext): Future[Unit] = {
    val p = Promise[Unit]()
    val sub = obs.subscribe(_ => (), error => p.failure(error), () => p.success(()))
    val ret = p.future
    ret.onComplete(_ => sub.unsubscribe())
    ret
  }
}

class Search(indexer: Indexer,
             auth: Authenticator[AuthedRequest],
             ctx: ActorExecution) {
  implicit val ec = ctx.executionContext
  val loggingObserver = indexingObserver(
    msg => log.debug(msg),
    (msg, t) => log.error(msg, t),
    compl => log.debug(compl))
  val subscription = indexer.ongoing.subscribe(op => subscribeUntilComplete(op, loggingObserver))
  val sockets = new Sockets(auth, ctx) {
    override def props(conf: ActorConfig[AuthedRequest]) =
      Props(new SearchActor(indexer, conf))
  }

  def openSocket = sockets.newSocket
}

object SearchActor {
  private val log = Logger(getClass)
}

class SearchActor(indexer: Indexer, ctx: ActorMeta)
  extends JsonActor(ctx) {
  var subscription: Option[Subscription] = None
  val socketBroadcaster = indexingObserver(
    msg => send(msg),
    (msg, _) => send(msg),
    compl => send(compl))

  override def preStart(): Unit = {
    super.preStart()
    // WTF?
    send("")
    val sub = indexer.ongoing.subscribe(op => subscribeUntilComplete(op, socketBroadcaster)(ec))
    subscription = Option(sub)
  }

  override def onMessage(msg: JsValue): Unit =
    msg.validate[SearchMessage].map {
      case Refresh =>
        send("Indexing...")
        indexer.indexAndSave()
      case Subscribe =>
        ()
    }.getOrElse {
      SearchActor.log warn s"Unknown message '$msg'."
    }

  def send(message: String): Unit = sendOut(SearchStatus(message))

  override def postStop() = {
    super.postStop()
    subscription foreach { sub => sub.unsubscribe() }
  }
}
