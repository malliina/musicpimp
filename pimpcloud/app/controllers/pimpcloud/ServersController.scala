package controllers.pimpcloud

import com.malliina.pimpcloud.auth.CloudAuthentication
import controllers.pimpcloud.ServersController.log
import play.api.Logger
import play.api.mvc._

class ServersController(cloudAuth: CloudAuthentication, auth: CloudAuth)
  extends StreamReceiver(auth.mat) {

  val mat = auth.mat
  implicit val ec = mat.executionContext

  def receiveUpload = serverAction(receive)

  def serverAction(f: ServerRequest => EssentialAction): EssentialAction =
    auth.loggedSecureActionAsync(cloudAuth.authServer)(f)

  def receive(server: ServerRequest): EssentialAction = {
    val requestID = server.request
    log debug s"Processing $requestID..."
    val transfers = server.socket.fileTransfers
    val parser = transfers parser requestID
    parser.fold[EssentialAction](Action(NotFound)) { parser =>
      receiveStream(parser, transfers, requestID)
    }
  }
}

object ServersController {
  private val log = Logger(getClass)
}
