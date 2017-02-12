package com.malliina.pimpcloud

import akka.stream.Materializer
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.pimpcloud.auth.{FakeAuth, ProdAuth}
import com.malliina.pimpcloud.ws.{PhoneClient, PhoneSockets}
import com.malliina.ws.RxStmStorage
import controllers.{PhoneConnection, Servers}
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

import scala.concurrent.Future

class JoinedSockets(mat: Materializer) {
  val servers = new Servers(mat) {
    override def sendToPhone(msg: JsValue, client: PimpServerSocket): Unit =
      onServerMessage(msg, client)
  }

  val auths = new ProdAuth(servers)
//  val auths = new FakeAuth(mat)

  val phones = new PhoneSockets(RxStmStorage[PhoneClient](), mat) {
    override def authenticatePhone(req: RequestHeader): Future[PhoneConnection] =
      authPhone(req)
  }

  def onServerMessage(msg: JsValue, server: PimpServerSocket) =
    phones.send(msg, server)

  def authPhone(req: RequestHeader): Future[PhoneConnection] =
    auths.authPhone(req)
}
