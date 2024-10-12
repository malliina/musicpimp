package controllers.musicpimp

import cats.effect.IO
import com.malliina.logback.PimpAppender.circeTransformer
import com.malliina.logback.{LogbackUtils, PimpAppender}
import com.malliina.musicpimp.auth.Auths
import com.malliina.musicpimp.models.Errors
import com.malliina.play.ActorExecution
import controllers.musicpimp.PimpLogs.log
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Flow, Sink}
import play.api.Logger
import play.api.mvc.WebSocket

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object PimpLogs:
  private val log = Logger(getClass)

class PimpLogs(ctx: ActorExecution):
  implicit val ec: ExecutionContext = ctx.executionContext
  implicit val mat: Materializer = ctx.materializer
  lazy val appender = LogbackUtils.getAppender[PimpAppender](PimpAppender.name)
  lazy val jsonEvents: fs2.Stream[IO, Json] =
    appender.logEvents.groupWithin(5, 100.millis).filter(_.nonEmpty).map(e => e.toList.asJson)

  def openSocket = WebSocket.acceptOrResult[Json, Json]: rh =>
    Auths.session
      .authenticate(rh)
      .map: authResult =>
        authResult
          .map: ok =>
            log.info(s"Opening logs socket for '${ok.user}'...")
            Flow.fromSinkAndSource(Sink.ignore, PimpAppender.fs2StreamToAkkaSource(jsonEvents))
          .left
          .map: err =>
            log.error(s"Unauthorized request '$rh': '$err'.")
            Errors.accessDenied
