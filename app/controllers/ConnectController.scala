package controllers

import java.net.NetworkInterface

import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import com.malliina.play.Authenticator
import com.malliina.play.http.AuthRequest
import com.malliina.play.json.JsonFormats
import controllers.ConnectController.{Protocols, log}
import net.glxn.qrgen.QRCode
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.mvc.RequestHeader
import views.html

import scala.collection.JavaConversions._

/**
  * This code is totally best effort so make sure the user has the final say.
  */
class ConnectController(auth: Authenticator, mat: Materializer) extends HtmlController(auth, mat) {
  def connect = navigate(html.connectApp())

  def image = pimpAction(implicit req => {
    val qrText = stringify(Json.toJson(coordinate(req))(Coordinate.json))
    log info s"Generating image with QR code: $qrText"
    val qrFile = QRCode.from(qrText).withSize(768, 768).file()
    FileIO.fromFile(qrFile)
    Ok.chunked(FileIO.fromFile(qrFile))
  })

  def coordinate(req: AuthRequest[_]): Coordinate =
    Coordinate(protocol(req), systemIPs, RequestHelpers port req, req.user)

  def systemIPs: Seq[String] =
    (for {
      interface <- NetworkInterface.getNetworkInterfaces
      address <- interface.getInetAddresses
      if !address.isAnyLocalAddress && !address.isLinkLocalAddress && !address.isLoopbackAddress
    } yield address.getHostAddress).toList

  def protocol(req: RequestHeader) = if (req.secure) Protocols.https else Protocols.http
}

object ConnectController {
  private val log = Logger(getClass)

  implicit object protoJson extends JsonFormats.SimpleFormat[Protocols.Protocol](name => Protocols.withName(name.toLowerCase))

  object Protocols extends Enumeration {
    type Protocol = Value
    val http, https, ws, wss = Value
  }

}

case class Coordinate(protocol: Protocols.Protocol, hosts: Seq[String], port: Int, username: String)

object Coordinate {
  implicit val json = Json.format[Coordinate]
}
