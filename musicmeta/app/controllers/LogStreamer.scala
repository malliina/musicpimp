package controllers

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.malliina.http.{Errors, SingleError}
import com.malliina.logback.LogbackUtils
import com.malliina.logback.akka.DefaultAkkaAppender
import com.malliina.logstreams.client.LogEvents
import com.malliina.play.auth.UserAuthenticator
import controllers.LogStreamer.log
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{Call, WebSocket}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object LogStreamer {
  private val log = Logger(getClass)

  def apply(ec: ExecutionContext): LogStreamer =
    new LogStreamer()(ec)
}

class LogStreamer()(implicit ec: ExecutionContext) {
  lazy val jsonEvents: Source[JsValue, NotUsed] = LogbackUtils
    .getAppender[DefaultAkkaAppender]("AKKA")
    .logEvents
    .groupedWithin(5, 100.millis)
    .filter(_.nonEmpty)
    .map(es => Json.toJson(LogEvents(es)))
  val auth = UserAuthenticator.session()

  def openSocket = WebSocket.acceptOrResult[JsValue, JsValue] { rh =>
    UserAuthenticator.session().authenticate(rh).map { authResult =>
      authResult.map { ok =>
        log.info(s"Opening logs socket for '${ok.name}'...")
        Flow.fromSinkAndSource(Sink.ignore, jsonEvents)
      }.left.map { err =>
        log.error(s"Unauthorized request '$rh': '$err'.")
        Unauthorized(Json.toJson(Errors(Seq(SingleError("Access denied.")))))
      }
    }
  }

  def openSocketCall: Call = routes.MetaOAuth.openSocket()
}
