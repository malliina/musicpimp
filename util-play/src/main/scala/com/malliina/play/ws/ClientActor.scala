package com.malliina.play.ws

import org.apache.pekko.actor.Props
import com.malliina.play.ws.ClientActor.log
import com.malliina.play.ws.Mediator.ClientMessage
import io.circe.{Decoder, Json}
import play.api.Logger

class ClientActor(ctx: ClientContext) extends JsonActor(ctx):
  val mediator = ctx.mediator

  override def preStart(): Unit =
    super.preStart()
    mediator ! Mediator.ClientJoined(ctx.out)

  override def onMessage(message: Json): Unit =
    transform(message).fold(
      error => log.error(s"Validation of '$message' failed. $error"),
      json => mediator ! ClientMessage(json, rh)
    )

  /** Transforms an incoming message before sending it to the mediator.
    *
    * @param message
    *   incoming message
    * @return
    *   the transformed message, or an error message
    */
  def transform(message: Json): Decoder.Result[Json] = Right(message)

object ClientActor:
  private val log = Logger(getClass)

  def props(ctx: ClientContext) = Props(new ClientActor(ctx))
