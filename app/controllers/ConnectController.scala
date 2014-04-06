package controllers

import views.html
import play.api.libs.json.Json._
import net.glxn.qrgen.QRCode
import play.api.libs.iteratee.Enumerator
import com.mle.util.Log
import com.mle.play.controllers.AuthRequest
import play.api.libs.json.Json
import controllers.ConnectController.Protocols
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import java.net.NetworkInterface
import collection.JavaConversions._
import play.api.mvc.RequestHeader
import com.mle.play.json.JsonFormats2

/**
 * This code is totally best effort so make sure the user has the final say.
 *
 * @author mle
 */
trait ConnectController extends Secured with HtmlController with Log {
  def connect = navigate(html.connectApp())

  def image = PimpAction(implicit req => {
    val qrText = stringify(Json.toJson(coordinate(req))(Coordinate.json))
    log info s"Generating image with QR code: $qrText"
    val qrFile = QRCode.from(qrText).withSize(768, 768).file()
    val enumerator = Enumerator fromFile qrFile
    Ok.chunked(enumerator)
  })

  def coordinate(req: AuthRequest[_]): Coordinate =
    Coordinate(protocol(req), systemIPs, RequestHelpers port req, req.user)

  def systemIPs: Seq[String] =
    (for {
      interface <- NetworkInterface.getNetworkInterfaces
      address <- interface.getInetAddresses
      if !address.isAnyLocalAddress && !address.isLinkLocalAddress && !address.isLoopbackAddress
    } yield address.getHostAddress).toList

  def protocol(req: RequestHeader) =
    if (RequestHelpers.isHttps(req)) Protocols.https else Protocols.http
}

object ConnectController {

  implicit object protoJson extends JsonFormats2.SimpleFormat[Protocols.Protocol](name => Protocols.withName(name.toLowerCase))

  object Protocols extends Enumeration {
    type Protocol = Value
    val http, https, ws, wss = Value
  }

}

case class Coordinate(protocol: Protocols.Protocol, hosts: Seq[String], port: Int, username: String)

object Coordinate {
  implicit val json = Json.format[Coordinate]
}
