package controllers

import play.api.libs.json.Json
import rx.lang.scala.{Subscription, Observable}
import com.mle.logbackrx.LogEvent
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConversions._
import play.api.mvc.WebSocket.FrameFormatter
import com.mle.play.json.SimpleCommand
import com.mle.musicpimp.json.JsonStrings

/**
 *
 * @author mle
 */
trait StreamingLogController extends JsonWebSocketController {
  def logEvents: Observable[LogEvent]

  private val jsonLogEvents = logEvents.map(e => Json.toJson(e))

  private val subscriptions: collection.concurrent.Map[Client, Subscription] =
    new ConcurrentHashMap[Client, Subscription]()

  def openLogSocket = ws(FrameFormatter.jsonFrame)

  override def onMessage(msg: Message, client: Client): Unit = {
    msg.validate[SimpleCommand].map(_.cmd match {
      case JsonStrings.SUBSCRIBE =>
        val subscription = jsonLogEvents.subscribe(e => client.channel push e)
        subscriptions += (client -> subscription)
      case _ => log.warn(s"Unknown message: $msg")
    })
  }

  // no need to store the client separately
  override def onConnect(client: Client): Unit =
    writeLog("connected", client)

  override def onDisconnect(client: Client): Unit = {
    subscriptions.get(client).foreach(_.unsubscribe())
    subscriptions -= client
    writeLog("disconnected", client)
  }

  private def writeLog(action: String, client: Client): Unit =
    log.info(s"User: ${client.user} from: ${client.request.remoteAddress} $action. Subscriptions in total: ${subscriptions.size}")
}