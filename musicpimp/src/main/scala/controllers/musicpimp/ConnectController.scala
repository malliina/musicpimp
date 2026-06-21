package controllers.musicpimp

import com.malliina.play.http.CookiedRequest
import com.malliina.values.Username
import controllers.musicpimp.ConnectController.{Protocols, log}
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Decoder, Encoder}
import net.glxn.qrgen.QRCode
import org.apache.pekko.stream.scaladsl.FileIO
import play.api.Logger
import play.api.mvc.{AnyContent, RequestHeader}

import java.net.NetworkInterface
import scala.jdk.CollectionConverters.EnumerationHasAsScala

/** Unused.
  */
class ConnectController(auth: AuthDeps) extends HtmlController(auth):

  def connect = navigate(_ => "todo")

  def image = pimpAction: req =>
    val qrText = coordinate(req).asJson.noSpaces
    log info s"Generating image with QR code: $qrText"
    val qrFile = QRCode.from(qrText).withSize(768, 768).file().toPath
    Ok.chunked(FileIO.fromPath(qrFile))

  def coordinate(req: CookiedRequest[AnyContent, Username]): Coordinate =
    Coordinate(protocol(req), systemIPs, req.user)

  def systemIPs: Seq[String] =
    (for
      interface <- NetworkInterface.getNetworkInterfaces.asScala
      address <- interface.getInetAddresses.asScala
      if !address.isAnyLocalAddress && !address.isLinkLocalAddress && !address.isLoopbackAddress
    yield address.getHostAddress).toList

  def protocol(req: RequestHeader) = if req.secure then Protocols.https else Protocols.http

object ConnectController:
  private val log = Logger(getClass)

  object Protocols extends Enumeration:
    type Protocol = Value
    val http, https, ws, wss = Value

  given Codec[Protocols.Protocol] = Codec.from(
    Decoder.decodeString.map(s => Protocols.withName(s.toLowerCase)),
    Encoder.encodeString.contramap(_.toString)
  )

case class Coordinate(protocol: Protocols.Protocol, hosts: Seq[String], username: Username)
  derives Codec.AsObject
