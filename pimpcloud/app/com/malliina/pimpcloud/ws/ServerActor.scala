package com.malliina.pimpcloud.ws

import org.apache.pekko.actor.{ActorRef, Scheduler}
import org.apache.pekko.stream.Materializer
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.json.CommonMessages
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.json.JsonStrings.{Body, Cmd, Id}
import com.malliina.pimpcloud.ws.ServerMediator.{ServerEvent, ServerJoined, StreamsUpdated}
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws.{ActorConfig, JsonActor}
import io.circe.Json
import io.circe.syntax.EncoderOps
import play.api.http.HttpErrorHandler

object ServerActor:
  val RegisteredKey = "registered"

/** A MusicPimp server connected to pimpcloud.
  */
class ServerActor(
  serverMediator: ActorRef,
  phoneMediator: ActorRef,
  conf: ActorConfig[AuthedRequest],
  errorHandler: HttpErrorHandler,
  mat: Materializer,
  scheduler: Scheduler
) extends JsonActor(conf):
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

  override def preStart(): Unit =
    super.preStart()
    out ! Json.obj(
      Cmd -> ServerActor.RegisteredKey.asJson,
      Body -> Json.obj(Id -> server.id.asJson)
    )
    serverMediator ! ServerJoined(server, out)

  override def onMessage(message: Json): Unit =
    if message == CommonMessages.ping then out ! CommonMessages.pong
    else {
      val completed = server.complete(message)
      if !completed then
        // sendToPhone, i.e. send to phones connected to this server
        phoneMediator ! ServerEvent(message, server.id)
    }
