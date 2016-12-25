package controllers

import com.malliina.maps.StmItemMap
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.json.JsonMessages
import com.malliina.play.controllers.Streaming
import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.Username
import play.api.Logger
import play.api.mvc.{RequestHeader, Security}
import rx.lang.scala.Subscription
import CloudWS.log
import scala.concurrent.Future

object CloudWS {
  private val log = Logger(getClass)
}

class CloudWS(clouds: Clouds, security: SecureBase) extends Streaming(security.mat) {
  val jsonEvents = clouds.connection.map(id => JsonMessages.event("cloud", "id" -> id))

  override def openSocketCall = routes.CloudWS.openSocket()

  override lazy val subscriptions = StmItemMap.empty[Client, Subscription]

  override def authenticateAsync(req: RequestHeader): Future[AuthedRequest] =
    req.session.get(Security.username).map(Username.apply)
      .map(user => fut(new AuthedRequest(user, req)))
      .getOrElse(Future.failed(new NoSuchElementException))
}
