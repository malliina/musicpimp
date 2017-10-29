package controllers.musicpimp

import com.malliina.musicpimp.audio.{FullTrack, TrackJson}
import com.malliina.musicpimp.html.PimpHtml
import com.malliina.musicpimp.http.PimpContentController.default
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.messaging._
import com.malliina.musicpimp.messaging.adm.AmazonDevices
import com.malliina.musicpimp.messaging.apns.APNSDevices
import com.malliina.musicpimp.messaging.gcm.GoogleDevices
import com.malliina.musicpimp.messaging.mpns.PushUrls
import com.malliina.musicpimp.scheduler.json.AlarmJsonHandler
import com.malliina.musicpimp.scheduler.web.SchedulerStrings
import com.malliina.musicpimp.scheduler.{ClockPlayback, PlaybackJob, ScheduledPlaybackService}
import com.malliina.play.http.Proxies
import com.malliina.push.adm.ADMToken
import controllers.musicpimp.Alarms.log
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc.Result

class Alarms(tags: PimpHtml,
             auth: AuthDeps,
             messages: Messages)
  extends AlarmEditor(tags, auth, messages)
    with SchedulerStrings {

  def alarms = pimpAction { request =>
    def content: Seq[ClockPlayback] = ScheduledPlaybackService.status

    implicit val w = TrackJson.writer(request)
    default.respond(request)(
      html = tags.alarms(content, request.user),
      json = Json.toJson(content.map(_.toFull(TrackJson.host(request))))
    )
  }

  def tokens = pimpAction { request =>
    def ts2 = APNSDevices.get().map(d => TokenInfo(d.id, Apns)) ++
      PushUrls.get().map(p => TokenInfo(p.url, Mpns)) ++
      GoogleDevices.get().map(g => TokenInfo(g.id, Gcm)) ++
      AmazonDevices.get().map(a => TokenInfo(a.id, Adm))

    def ts = Seq(TokenInfo(ADMToken("amzn1.adm-registration.v3.Y29tLmFtYXpvbi5EZXZpY2VNZXNzYWdpbmcuUmVnaXN0cmF0aW9uSWRFbmNyeXB0aW9uS2V5ITEhaXJTVmVVQXhJSnhnRGs3MGl0S0E1TExldGwxcWFPRzFHK3cwL1N1OS9zMnN1RFFBZHd1VkNWMXhaYlp3dTExWGdTNytOYk5jaVZ1OEtKWnoyNmhubnBoYTdiRVhlTzJTYkd3TlFaWXBNMHRCSDRnZi9KQUl1VGtoUzBwdjVkU2gwdGs1R2RWaVRRYzRHWFF3ZUU5MTU2MHI2ODRxN0pCYnJSMVFyaHhPUjI4NmVPT0lUcG5SWjJxUnRrOWZMdFdJdlpWMWxCVk1MSmtkRmIxY3llMkZZRWo0WFpiVWxMUEsrbnduZFB0Rm80TUdubFYxK1ZNdDA2bGJ5NFozNTZnbCtJVXBPRG9maTZ1NktnZXR0akZEeXhSV1pRV0lDRWh6b2ROenNxRTRsa0poS3EvSjJzaXFPQWpuVzd2Z0tINjZvRTFUV0MvOVJDaU81bE9pQmtGY3RnPT0hZGQ5WjZ2Z1c1MVJxY2kva2NmbmhyZz09"), Adm))

    default.respond(request)(
      html = tags.tokens(ts, request.user),
      json = Json.toJson(Tokens(ts))
    )
  }

  def handleJson = pimpParsedAction(parsers.json) { jsonRequest =>
    val json = jsonRequest.body
    val remoteAddress = Proxies.realAddress(jsonRequest)
    log debug s"User '${jsonRequest.user}' from '$remoteAddress' said '$json'."
    onRequest(json)
  }

  def tracks = pimpAction { request =>
    val tracks: Iterable[FullTrack] = Library.tracksRecursive.map(TrackJson.toFull(_, TrackJson.host(request)))
    Ok(Json.toJson(tracks))
  }

  def paths = pimpAction { _ =>
    val tracks = Library.songPathsRecursive
    implicit val pathFormat = PlaybackJob.pathFormat
    Ok(Json.toJson(tracks))
  }

  private def onRequest(json: JsValue): Result = {
    val jsResult = AlarmJsonHandler.handle(json)
    simpleResult(json, jsResult)
  }

  private def simpleResult[T](json: JsValue, result: JsResult[T]): Result = {
    result.fold(
      errors => badRequest(s"Invalid JSON '$json'. Errors '$errors'."),
      _ => Ok
    )
  }
}

object Alarms {
  private val log = Logger(getClass)
}
