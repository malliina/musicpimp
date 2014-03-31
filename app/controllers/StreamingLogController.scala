package controllers

import play.api.libs.json.{JsValue, Json}
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
  def logEvents: Observable[JsValue]

  private val subscriptions: collection.concurrent.Map[Client, Subscription] =
    new ConcurrentHashMap[Client, Subscription]()

  def openLogSocket = ws(FrameFormatter.jsonFrame)

  override def onConnect(client: Client): Unit = ()

  override def onMessage(msg: Message, client: Client): Unit = {
    msg.validate[SimpleCommand].map(_.cmd match {
      case JsonStrings.SUBSCRIBE => onSubscribe(client)
      case _ => log.warn(s"Unknown message: $msg")
    })
  }

  def onSubscribe(client: Client) {
    val subscription = logEvents.subscribe(e => client.channel push e)
    subscriptions += (client -> subscription)
    writeLog("observes logs", client)
  }

  override def onDisconnect(client: Client): Unit = {
    subscriptions.get(client).foreach(_.unsubscribe())
    subscriptions -= client
    writeLog("stopped observing", client)
  }

  private def writeLog(action: String, client: Client): Unit =
    log.info(s"User: ${client.user} from: ${client.request.remoteAddress} $action. Observers in total: ${subscriptions.size}")
}