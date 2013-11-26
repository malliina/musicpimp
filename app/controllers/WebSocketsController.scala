package controllers

import play.api.mvc.{WebSocket, RequestHeader}
import play.api.libs.iteratee.{Iteratee, Concurrent}
import com.mle.musicpimp.Starter
import java.net.InetSocketAddress
import WebSocketsController._
import play.api.libs.json.JsValue
import com.mle.musicpimp.exception.PimpException

object WebSocketsController {
  /**
   * May not work if the HTTPS port is 80 and excluded from the request's <code>host</code> member.
   *
   * @param request
   * @return true if the request was made over HTTPS, false otherwise
   */
  def isHttps(request: RequestHeader) = {
    val httpsPort = Starter.nettyServer.flatMap(_.HTTPS.map(_._2.getLocalAddress.asInstanceOf[InetSocketAddress].getPort))
    httpsPort.exists(sslPort => request.host.contains(s":$sslPort"))
  }
}

