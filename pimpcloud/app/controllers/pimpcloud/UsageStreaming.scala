package controllers.pimpcloud

import com.malliina.pimpcloud.ws.PhoneSockets
import play.api.libs.json.JsValue
import play.api.mvc.Call
import rx.lang.scala.Observable

class UsageStreaming(servers: Servers,
                     phoneSockets: PhoneSockets,
                     auth: PimpAuth) extends AdminStreaming(auth, servers.mat) {
  override def jsonEvents: Observable[JsValue] = servers.usersJson merge phoneSockets.usersJson merge servers.uuidsJson

  override def openSocketCall: Call = routes.UsageStreaming.openSocket()
}
