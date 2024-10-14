package com.malliina.play.ws

import cats.effect.unsafe.implicits.global
import com.malliina.play.ActorExecution
import com.malliina.play.auth.{AuthFailure, Authenticator}
import com.malliina.play.ws.Sockets.{DefaultActorBufferSize, DefaultOverflowStrategy, circeTransformer, log}
import io.circe.{Json, ParsingFailure, parser}
import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.{Materializer, OverflowStrategy}
import play.api.Logger
import play.api.http.websocket.{BinaryMessage, CloseCodes, CloseMessage, Message, TextMessage}
import play.api.libs.streams.{ActorFlow, PekkoStreams}
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc.{RequestHeader, Result, Results, WebSocket}

import scala.concurrent.ExecutionContextExecutor

object Sockets:
  private val log = Logger(getClass)

  val DefaultActorBufferSize = 1000
  val DefaultOverflowStrategy = OverflowStrategy.fail

  implicit val circeTransformer: MessageFlowTransformer[Json, Json] =
    def closeOnException(e: Either[ParsingFailure, Json]): Either[Json, CloseMessage] =
      e.swap.map: f =>
        CloseMessage(Option(CloseCodes.Unacceptable), "Unable to parse JSON message.")

    val splitter = Flow[Message].collect:
      case BinaryMessage(data) => closeOnException(parser.parse(data.utf8String))
      case TextMessage(text)   => closeOnException(parser.parse(text))
    (flow: Flow[Json, Json, ?]) =>
      PekkoStreams.bypassWith[Message, Json, Message](splitter)(flow.map: json =>
        TextMessage(json.noSpaces))

abstract class Sockets[User](
  auth: Authenticator[User],
  ctx: ActorExecution,
  actorBufferSize: Int = DefaultActorBufferSize,
  overflowStrategy: OverflowStrategy = DefaultOverflowStrategy
):
  implicit val actorSystem: ActorSystem = ctx.actorSystem
  implicit val mat: Materializer = ctx.materializer
  implicit val ec: ExecutionContextExecutor = ctx.executionContext

  /** Builds actor Props of an authenticated client.
    *
    * @param conf
    *   context for the client connection
    * @return
    *   Props of an actor that receives incoming messages
    */
  def props(conf: ActorConfig[User]): Props

  def onUnauthorized(rh: RequestHeader, failure: AuthFailure): Result =
    log.warn(s"Unauthorized request $rh")
    Results.Unauthorized

  def newSocket = WebSocket.acceptOrResult[Json, Json]: rh =>
    auth
      .authenticate(rh)
      .map: authResult =>
        authResult.fold(
          failure => Left(onUnauthorized(rh, failure)),
          user => Right(actorFlow(user, rh))
        )

  private def actorFlow(user: User, rh: RequestHeader): Flow[Json, Json, ?] =
    ActorFlow.actorRef[Json, Json](
      out => props(DefaultActorConfig(out, rh, user)),
      actorBufferSize,
      overflowStrategy
    )
