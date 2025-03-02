package com.malliina.play.ws

import org.apache.pekko.actor.{Actor, Cancellable, Props}
import com.malliina.play.http.Proxies
import com.malliina.play.json.JsonMessages
import com.malliina.play.ws.JsonActor.log
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import play.api.Logger

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

class JsonActor(ctx: ActorMeta) extends Actor:
  implicit val ec: ExecutionContextExecutor = context.dispatcher
  val out = ctx.out
  val rh = ctx.rh
  var pinger: Option[Cancellable] = None

  override def preStart(): Unit =
    super.preStart()
    scheduleHealthCheck()

  private def scheduleHealthCheck(): Unit =
    val healthCheck = context.system.scheduler.scheduleWithFixedDelay(
      initialDelay = 10.seconds,
      delay = 30.seconds,
      receiver = out,
      message = JsonMessages.ping
    )
    pinger = Option(healthCheck)

  override def receive: Receive = { case json: Json =>
    onMessage(json)
  }

  def onMessage(message: Json): Unit =
    log.info(s"Client '$address' says '$message'.")

  def address: String = Proxies.realAddress(rh)

  def sendOut[C: Encoder](c: C): Unit = out ! c.asJson

  override def postStop(): Unit =
    super.postStop()
    pinger.foreach(p => p.cancel())

object JsonActor:
  private val log = Logger(getClass)

  def props(ctx: ActorMeta) = Props(new JsonActor(ctx))
