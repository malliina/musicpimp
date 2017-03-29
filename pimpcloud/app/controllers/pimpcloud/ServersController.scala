package controllers.pimpcloud

import akka.stream.Materializer
import com.malliina.musicpimp.models.Errors
import com.malliina.play.auth.Authenticator
import com.malliina.play.controllers.{AuthBundle, BaseSecurity}
import controllers.pimpcloud.ServersController.log
import play.api.Logger
import play.api.mvc._

class ServersController(auth: BaseSecurity[ServerRequest], mat: Materializer)
  extends StreamReceiver(mat) {

  def receiveUpload = auth.authenticatedLogged(r => receive(r))

  def receive(server: ServerRequest): EssentialAction = {
    val requestID = server.request
    log debug s"Processing '$requestID'..."
    val transfers = server.socket.fileTransfers
    val parser = transfers parser requestID
    parser.fold[EssentialAction](Action(Errors.notFound(s"Request not found '$requestID'."))) { parser =>
      receiveStream(parser, transfers, requestID)
    }
  }
}

object ServersController {
  private val log = Logger(getClass)

  def forAuth(auth: Authenticator[ServerRequest], mat: Materializer): ServersController = {
    val serverBundle = AuthBundle.default(auth)
    val serverAuth = new BaseSecurity(serverBundle, mat)
    new ServersController(serverAuth, mat)
  }
}
