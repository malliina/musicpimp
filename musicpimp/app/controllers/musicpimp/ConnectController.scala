package controllers.musicpimp

import java.net.NetworkInterface
import akka.stream.scaladsl.FileIO
import com.malliina.json.JsonFormats
import com.malliina.play.http.CookiedRequest
import com.malliina.values.Username
import controllers.musicpimp.ConnectController.{Protocols, log}
import net.glxn.qrgen.QRCode
import play.api.Logger
import play.api.libs.json.{Json, OFormat}
import play.api.libs.json.Json._
import play.api.mvc.{AnyContent, RequestHeader}

import scala.jdk.CollectionConverters.EnumerationHasAsScala

/** Unused.
  */
class ConnectController(auth: AuthDeps) extends HtmlController(auth) {

  def connect = navigate(_ => "todo")

  def image = pimpAction { req =>
    val qrText = stringify(Json.toJson(coordinate(req))(Coordinate.json))
    log info s"Generating image with QR code: $qrText"
    val qrFile = QRCode.from(qrText).withSize(768, 768).file().toPath
    Ok.chunked(FileIO.fromPath(qrFile))
  }

  def coordinate(req: CookiedRequest[AnyContent, Username]): Coordinate =
    Coordinate(protocol(req), systemIPs, req.user)

  def systemIPs: Seq[String] =
    (for {
      interface <- NetworkInterface.getNetworkInterfaces.asScala
      address <- interface.getInetAddresses.asScala
      if !address.isAnyLocalAddress && !address.isLinkLocalAddress && !address.isLoopbackAddress
    } yield address.getHostAddress).toList

  def protocol(req: RequestHeader) = if (req.secure) Protocols.https else Protocols.http
}

object ConnectController {
  private val log = Logger(getClass)

  implicit object protoJson
    extends JsonFormats.SimpleFormat[Protocols.Protocol](
      name => Protocols.withName(name.toLowerCase)
    )

  object Protocols extends Enumeration {
    type Protocol = Value
    val http, https, ws, wss = Value
  }

}

case class Coordinate(protocol: Protocols.Protocol, hosts: Seq[String], username: Username)

object Coordinate {
  implicit val json: OFormat[Coordinate] = Json.format[Coordinate]
}
