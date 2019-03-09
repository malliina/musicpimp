package com.malliina.play

import akka.actor.Props
import com.malliina.play.auth.Authenticator
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws.{ActorConfig, ObserverActor, Sockets}
import play.api.libs.json.JsValue
import rx.lang.scala.Observable

object PimpSockets {
  def observingSockets(events: Observable[JsValue],
                       auth: Authenticator[AuthedRequest],
                       ctx: ActorExecution): Sockets[AuthedRequest] = {
    new Sockets(auth, ctx) {
      override def props(conf: ActorConfig[AuthedRequest]) =
        Props(new ObserverActor(events, conf))
    }
  }
}
