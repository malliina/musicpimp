package controllers.pimpcloud

import akka.stream.Materializer
import com.malliina.musicpimp.models.Errors
import com.malliina.play.auth.Authenticator
import com.malliina.play.controllers.{AuthBundle, BaseSecurity}
import controllers.pimpcloud.ServersController.log
import play.api.Logger
import play.api.mvc._

class ServersController(comps: ControllerComponents, auth: BaseSecurity[ServerRequest])
  extends StreamReceiver(comps) {

  def receiveUpload = auth.authenticatedLogged((r: ServerRequest) => receive(r))

  private def receive(server: ServerRequest): EssentialAction = {
    val requestID = server.request
    log debug s"Processing '$requestID'..."
    val transfers = server.socket.fileTransfers
    val maybeParser = transfers parser requestID
    maybeParser.fold[EssentialAction](Action(Errors.notFound(s"Request not found '$requestID'."))) { parser =>
      receiveStream(parser, transfers, requestID)
    }
  }
}

object ServersController {
  private val log = Logger(getClass)

  def forAuth(comps: ControllerComponents,
              auth: Authenticator[ServerRequest],
              mat: Materializer): ServersController = {
    val serverBundle = AuthBundle.default(auth)
    val serverAuth: BaseSecurity[ServerRequest] = new BaseSecurity(comps.actionBuilder, serverBundle, mat)
    new ServersController(comps, serverAuth)
  }
}
