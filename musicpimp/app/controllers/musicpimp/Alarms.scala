package controllers.musicpimp

import com.malliina.musicpimp.audio.{FullTrack, TrackJson}
import com.malliina.musicpimp.html.PimpHtml
import com.malliina.musicpimp.http.PimpContentController.default
import com.malliina.musicpimp.library.FileLibrary
import com.malliina.musicpimp.messaging.*
import com.malliina.musicpimp.messaging.adm.AmazonDevices
import com.malliina.musicpimp.messaging.apns.APNSDevices
import com.malliina.musicpimp.messaging.gcm.GoogleDevices
import com.malliina.musicpimp.messaging.mpns.PushUrls
import com.malliina.musicpimp.scheduler.*
import com.malliina.musicpimp.scheduler.json.JsonHandler
import com.malliina.musicpimp.scheduler.web.SchedulerStrings
import com.malliina.play.http.Proxies
import controllers.musicpimp.Alarms.log
import io.circe.{Decoder, Encoder, Json}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.*
import play.api.http.Writeable
import play.api.i18n.Messages
import play.api.mvc.Result

import java.nio.file.Path

case class RemoveToken(token: String, platform: String)

class Alarms(
  library: FileLibrary,
  handler: JsonHandler,
  tags: PimpHtml,
  auth: AuthDeps,
  messages: Messages
) extends AlarmEditor(handler.schedules, tags, auth, messages)
  with SchedulerStrings:

  val removalForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "platform" -> nonEmptyText
    )(RemoveToken.apply)(t => Option((t.token, t.platform)))
  )

  def alarms = pimpActionAsync: request =>
    schedules
      .clockList(TrackJson.host(request))
      .map: fcps =>
        default.respond(request)(
          html = tags.alarms(fcps, request.user),
          json = fcps
        )

  def tokens = pimpAction: request =>
    def ts =
      APNSDevices.get().map(d => TokenInfo(d.id, Apns)) ++
        PushUrls.get().map(p => TokenInfo(p.url, Mpns)) ++
        GoogleDevices.get().map(g => TokenInfo(g.id, Gcm)) ++
        AmazonDevices.get().map(a => TokenInfo(a.id, Adm))

    default.respond(request)(
      html = tags.tokens(ts, request.user, UserFeedback.flashed(request)),
      json = Tokens(ts)
    )

  def remove =
    pimpParsedAction(parsers.form(removalForm)): request =>
      val spec = request.body
      val token = spec.token
      TokenPlatform
        .build(spec.platform)
        .map:
          case Apns => APNSDevices.removeWhere(_.id.token == token)
          case Mpns => PushUrls.removeURL(token)
          case Gcm  => GoogleDevices.removeWhere(_.id.token == token)
          case Adm  => AmazonDevices.removeWhere(_.id.token == token)
        .map: _ =>
          Redirect(routes.Alarms.tokens).flashing(UserFeedback.success("Removed.").flash)
        .getOrElse:
          BadRequest

  def handleJson = pimpParsedAction(circeJson): jsonRequest =>
    val json = jsonRequest.body
    val remoteAddress = Proxies.realAddress(jsonRequest)
    log.debug(s"User '${jsonRequest.user}' from '$remoteAddress' said '$json'.")
    onRequest(json)

  def tracks = pimpAction: request =>
    val tracks: Iterable[FullTrack] =
      library.tracksRecursive.map(TrackJson.toFull(_, TrackJson.host(request)))
    Ok(tracks)

  given Encoder[Path] = Encoder.encodeString.contramap(_.toString)
  given Writeable[List[Path]] = jsonWriteable[List[Path]]

  def paths = pimpAction: _ =>
    val tracks: List[Path] = library.songPathsRecursive.toList
    Ok(tracks)

  private def onRequest(json: Json): Result =
    val jsResult = handler.handle(json)
    simpleResult(json, jsResult)

  private def simpleResult[T](json: Json, result: Decoder.Result[T]): Result =
    result.fold(
      errors => badRequest(s"Invalid JSON '$json'. Errors '$errors'."),
      _ => Ok
    )

object Alarms:
  private val log = Logger(getClass)
