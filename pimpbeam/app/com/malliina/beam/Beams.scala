package com.malliina.beam

import cats.effect.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}
import org.apache.pekko.actor.{Actor, ActorRef, Props, Terminated}
import org.apache.pekko.pattern.ask
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.Timeout
import com.malliina.beam.BeamMediator.*
import com.malliina.beam.Beams.log
import com.malliina.musicpimp.auth.{Authenticator, UserPayload}
import com.malliina.musicpimp.auth.Authenticator.AuthOutcome
import com.malliina.play.ActorExecution
import com.malliina.play.auth.{Auth, BasicCredentials, InvalidCredentials}
import com.malliina.play.json.JsonMessages
import com.malliina.play.ws.{ActorConfig, JsonActor, Sockets}
import com.malliina.util.AppLogger
import com.malliina.values.Username
import controllers.Home
import io.circe.Json
import org.http4s.Request
import play.api.Logger
import play.api.mvc.RequestHeader

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

object Beams:
  private val log = AppLogger(getClass)

class Beams[F[_]: Async]:
  val F = Async[F]
//  val mediator = ctx.actorSystem.actorOf(BeamMediator.props(ctx.materializer))

  val playerAuthenticator: Authenticator[F, UserPayload] = ??? // UserAuthenticator.session()
  val phoneAuthenticator: Authenticator[F, UserPayload] =
    ??? // = UserAuthenticator.basic(Auth.basicCredentials, validatePhoneCredentials)

//  val playerSockets = new Sockets[Username](playerAuthenticator, ctx):
//    override def props(conf: ActorConfig[Username]): Props =
//      BeamClientActor.props(conf, mediator, isPhone = false)
//  val phoneSockets = new Sockets[Username](phoneAuthenticator, ctx):
//    override def props(conf: ActorConfig[Username]) =
//      BeamClientActor.props(conf, mediator, isPhone = true)

//  def openPlayer = playerSockets.newSocket
//
//  def openPhone = phoneSockets.newSocket

  def player(user: Username) = findPlayer(user).flatMap: maybe =>
    maybe.map(F.pure).getOrElse(F.raiseError(new NoSuchElementException(user.name)))

  def findPlayer(user: Username): F[Option[PlayerClient]] = F.pure(None)
//    (mediator ? FindPlayer(user)).mapTo[Option[PlayerClient]]

  /** Validates the supplied credentials, which are valid if:
    *
    * 1) a player is connected with the same non-empty username as the one supplied here 2) the
    * default password is correct
    *
    * @return
    *   true if the credentials are valid, false otherwise
    */
  def validatePhoneCredentials(creds: BasicCredentials): F[Option[Username]] =
    val user = creds.username
    log.debug(s"Validating '$user'...")
    if Home.validateCredentials(creds) then
      playerExists(user).map(exists => if exists then Option(user) else None)
    else F.pure(None)

  def playerExists(username: Username): F[Boolean] = F.pure(false)
//    (mediator ? FindPlayer(username)).mapTo[Option[ActorRef]].map(_.isDefined)

  /** @return
    *   the player connected as the username specified in the session
    */
  def authPlayer(rh: Request[F]): F[AuthOutcome[PlayerClient]] =
    playerAuthenticator
      .authenticate(rh)
      .flatMap: outcome =>
        outcome.fold(
          fail => F.pure(Left(fail)),
          user => findPlayer(user.username).map(_.toRight(???)) // InvalidCredentials(rh)))
        )

  def authUser(rh: Request[F]) =
    playerAuthenticator.authenticate(rh)

object BeamMediator:
  sealed trait BeamMediatorMessage

  case class PlayerJson(message: Json, user: Username) extends BeamMediatorMessage

  case class PhoneJson(message: Json, user: Username) extends BeamMediatorMessage

  case class PlayerJoined(user: Username, ref: ActorRef) extends BeamMediatorMessage

  case class PhoneJoined(user: Username, ref: ActorRef) extends BeamMediatorMessage

  case class PlayerLeft(user: Username) extends BeamMediatorMessage

  case class PhoneLeft(user: Username) extends BeamMediatorMessage

  case class ToPhone(message: Json, user: Username) extends BeamMediatorMessage

  case class ToPlayer(message: Json, user: Username) extends BeamMediatorMessage

  case class FindPlayer(user: Username) extends BeamMediatorMessage

  case class FindPhone(user: Username) extends BeamMediatorMessage

class BeamMediator(mat: Materializer) extends Actor:
  var players: Seq[PlayerClient] = Nil
  var phones: Map[Username, ActorRef] = Map.empty

  override def receive: Receive = {
    case ToPlayer(message, to) =>
      players.filter(_.user == to).foreach(_.out ! message)
    case ToPhone(message, to) =>
      phones.view
        .filterKeys(u => u == to)
        .toMap
        .foreach:
          case (_, ref) => ref ! message
    case PlayerJoined(user, ref) =>
      context.watch(ref)
      players = players :+ new PlayerClient(user, ref, mat)
      ref ! JsonMessages.welcome
    case PhoneJoined(user, ref) =>
      context.watch(ref)
      phones += (user -> ref)
      ref ! JsonMessages.welcome
    case PlayerLeft(user) =>
      players = players.filterNot(_.user == user)
      self ! ToPhone(BeamMessages.partyDisconnected(user), user)
    case PhoneLeft(user) =>
      phones -= user
      self ! ToPlayer(BeamMessages.partyDisconnected(user), user)
    case FindPlayer(user) =>
      sender() ! players.find(_.user == user)
    case FindPhone(user) =>
      sender() ! phones
        .find:
          case (u, _) => u == user
        .map:
          case (_, ref) => ref
    case Terminated(ref) =>
      players
        .find(_.out == ref)
        .foreach: player =>
          self ! PlayerLeft(player.user)
      phones
        .find:
          case (_, saved) => saved == ref
        .foreach:
          case (user, _) =>
            self ! PhoneLeft(user)
  }

object BeamClientActor:
  def props(meta: ActorConfig[Username], mediator: ActorRef, isPhone: Boolean) =
    Props(new BeamClientActor(meta, mediator, isPhone))

class BeamClientActor(meta: ActorConfig[Username], mediator: ActorRef, isPhone: Boolean)
  extends JsonActor(meta):
  val user = meta.user

  override def preStart(): Unit =
    super.preStart()
    val msg =
      if isPhone then PhoneJoined(user, meta.out)
      else PlayerJoined(user, meta.out)
    mediator ! msg

  override def onMessage(message: Json): Unit =
    val msg =
      if isPhone then ToPlayer(message, user)
      else ToPhone(message, user)
    mediator ! msg
