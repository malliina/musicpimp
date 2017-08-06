package controllers.pimpcloud

import com.malliina.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.malliina.play.tags.TagPage
import com.malliina.play.{ActorExecution, PimpSockets}
import play.api.Logger
import play.api.libs.json.Json

import scala.concurrent.duration.DurationInt

class Logs(tags: CloudTags, auth: PimpAuth, ctx: ActorExecution) {
  val appenderName = "RX"
  lazy val jsonEvents = LogbackUtils.getAppender[BasicBoundedReplayRxAppender](appenderName)
    .logEvents
    .tumblingBuffer(100.millis)
    .filter(_.nonEmpty)
    .map(Json.toJson(_))
  lazy val sockets = PimpSockets.observingSockets(jsonEvents, auth, ctx)

  def index = auth.navigate[TagPage](_ => tags.admin)

  def logs = auth.navigate[TagPage](_ => tags.logs)

  def openSocket = sockets.newSocket
}

object Logs {
  private val log = Logger(getClass)
}
