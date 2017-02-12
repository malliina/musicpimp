package controllers

import akka.stream.Materializer
import com.malliina.maps.{ItemMap, StmItemMap}
import com.malliina.play.controllers.Streaming
import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.Username
import com.malliina.play.ws.JsonSocketClient
import play.api.mvc.RequestHeader
import rx.lang.scala.Subscription

import scala.concurrent.Future

/** Base class that streams JSON to web clients.
  *
  * Check subclasses [[Logs]] and [[UsageStreaming]] for more info.
  */
abstract class AdminStreaming(oauth: PimpAuth, mat: Materializer) extends Streaming(mat) {
  override val subscriptions: ItemMap[JsonSocketClient[Username], Subscription] =
    StmItemMap.empty[JsonSocketClient[Username], Subscription]

  override def authenticateAsync(req: RequestHeader): Future[AuthedRequest] =
    getOrFail(oauth.authenticate(req))

  private def getOrFail[T](f: Future[Option[T]]): Future[T] =
    f.flatMap(_.map(Future.successful).getOrElse(Future.failed(new NoSuchElementException)))
}
