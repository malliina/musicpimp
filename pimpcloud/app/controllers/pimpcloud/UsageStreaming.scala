package controllers.pimpcloud

import com.malliina.pimpcloud.ws.PhoneSockets
import com.malliina.play.{ActorExecution, PimpSockets}
import play.api.libs.json.JsValue
import rx.lang.scala.Observable

class UsageStreaming(servers: Servers,
                     phoneSockets: PhoneSockets,
                     auth: PimpAuth,
                     ctx: ActorExecution) {
  val jsonEvents: Observable[JsValue] = servers.usersJson merge phoneSockets.usersJson merge servers.uuidsJson
  val sockets = PimpSockets.observingSockets(jsonEvents, auth, ctx)

  def openSocket = sockets.newSocket
}
