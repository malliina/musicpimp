package com.malliina.pimpcloud.ws

import akka.actor.{ActorRef, Scheduler}
import akka.stream.Materializer
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.json.CommonMessages
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.json.JsonStrings.{Body, Cmd, Id}
import com.malliina.pimpcloud.ws.ServerMediator.{ServerEvent, ServerJoined, StreamsUpdated}
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws.{ActorConfig, JsonActor}
import play.api.http.HttpErrorHandler
import play.api.libs.json.{JsValue, Json}

object ServerActor {
  val RegisteredKey = "registered"
}

/** A MusicPimp server connected to pimpcloud.
  */
class ServerActor(
  serverMediator: ActorRef,
  phoneMediator: ActorRef,
  conf: ActorConfig[AuthedRequest],
  errorHandler: HttpErrorHandler,
  mat: Materializer,
  scheduler: Scheduler
) extends JsonActor(conf) {
  val cloudId = CloudID(conf.user.user.name)
  val server = new PimpServerSocket(
    out,
    cloudId,
    conf.rh,
    mat,
    scheduler,
    errorHandler,
    () => serverMediator ! StreamsUpdated
  )

  override def preStart(): Unit = {
    super.preStart()
    out ! Json.obj(Cmd -> ServerActor.RegisteredKey, Body -> Json.obj(Id -> server.id))
    serverMediator ! ServerJoined(server, out)
  }

  override def onMessage(message: JsValue): Unit = {
    if (message == CommonMessages.ping) {
      out ! CommonMessages.pong
    } else {
      val completed = server complete message
      if (!completed) {
        // sendToPhone, i.e. send to phones connected to this server
        phoneMediator ! ServerEvent(message, server.id)
      }
    }
  }
}
