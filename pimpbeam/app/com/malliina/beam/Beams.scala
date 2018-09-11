package com.malliina.beam

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.malliina.beam.BeamMediator._
import com.malliina.beam.Beams.log
import com.malliina.play.ActorExecution
import com.malliina.play.auth.Authenticator.Outcome
import com.malliina.play.auth.{Auth, BasicCredentials, InvalidCredentials, UserAuthenticator}
import com.malliina.play.json.JsonMessages
import com.malliina.play.ws.{ActorConfig, JsonActor, Sockets}
import com.malliina.values.Username
import controllers.Home
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

object Beams {
  private val log = Logger(getClass)

  def apply(ctx: ActorExecution) = new Beams(ctx)
}

class Beams(ctx: ActorExecution) {
  implicit val timeout: Timeout = Timeout(10.seconds)
  implicit val ec: ExecutionContext = ctx.executionContext
  val mediator = ctx.actorSystem.actorOf(BeamMediator.props(ctx.materializer))

  val playerAuthenticator = UserAuthenticator.session()
  val phoneAuthenticator = UserAuthenticator.basic(Auth.basicCredentials, validatePhoneCredentials)

  val playerSockets = new Sockets[Username](playerAuthenticator, ctx) {
    override def props(conf: ActorConfig[Username]): Props =
      BeamClientActor.props(conf, mediator, isPhone = false)
  }
  val phoneSockets = new Sockets[Username](phoneAuthenticator, ctx) {
    override def props(conf: ActorConfig[Username]) =
      BeamClientActor.props(conf, mediator, isPhone = true)
  }

  def openPlayer = playerSockets.newSocket

  def openPhone = phoneSockets.newSocket

  def player(user: Username) = findPlayer(user).flatMap { maybe =>
    maybe.map(Future.successful).getOrElse(Future.failed(new NoSuchElementException(user.name)))
  }

  def findPlayer(user: Username): Future[Option[PlayerClient]] =
    (mediator ? FindPlayer(user)).mapTo[Option[PlayerClient]]

  /** Validates the supplied credentials, which are valid if:
    *
    * 1) a player is connected with the same non-empty username as the one supplied here
    * 2) the default password is correct
    *
    * @return true if the credentials are valid, false otherwise
    */
  def validatePhoneCredentials(creds: BasicCredentials): Future[Option[Username]] = {
    val user = creds.username
    log debug s"Validating '$user'..."
    if (Home.validateCredentials(creds))
      playerExists(user).map(exists => if (exists) Option(user) else None)
    else Future.successful(None)
  }

  def playerExists(username: Username) = {
    (mediator ? FindPlayer(username)).mapTo[Option[ActorRef]].map(_.isDefined)
  }

  /**
    * @return the player connected as the username specified in the session
    */
  def authPlayer(rh: RequestHeader): Future[Outcome[PlayerClient]] = {
    playerAuthenticator.authenticate(rh).flatMap { outcome =>
      outcome.fold(
        fail => Future.successful(Left(fail)),
        user => findPlayer(user).map(_.toRight(InvalidCredentials(rh)))
      )
    }
  }

  def authUser(rh: RequestHeader) =
    playerAuthenticator.authenticate(rh)
}

object BeamMediator {
  def props(mat: Materializer) = Props(new BeamMediator(mat))

  sealed trait BeamMediatorMessage

  case class PlayerJson(message: JsValue, user: Username) extends BeamMediatorMessage

  case class PhoneJson(message: JsValue, user: Username) extends BeamMediatorMessage

  case class PlayerJoined(user: Username, ref: ActorRef) extends BeamMediatorMessage

  case class PhoneJoined(user: Username, ref: ActorRef) extends BeamMediatorMessage

  case class PlayerLeft(user: Username) extends BeamMediatorMessage

  case class PhoneLeft(user: Username) extends BeamMediatorMessage

  case class ToPhone(message: JsValue, user: Username) extends BeamMediatorMessage

  case class ToPlayer(message: JsValue, user: Username) extends BeamMediatorMessage

  case class FindPlayer(user: Username) extends BeamMediatorMessage

  case class FindPhone(user: Username) extends BeamMediatorMessage

}

class BeamMediator(mat: Materializer) extends Actor {
  var players: Seq[PlayerClient] = Nil
  var phones: Map[Username, ActorRef] = Map.empty

  override def receive: Receive = {
    case ToPlayer(message, to) =>
      players.filter(_.user == to).foreach(_.out ! message)
    case ToPhone(message, to) =>
      phones.filterKeys(u => u == to).foreach { case (_, ref) => ref ! message }
    case PlayerJoined(user, ref) =>
      context watch ref
      players = players :+ new PlayerClient(user, ref, mat)
      ref ! JsonMessages.welcome
    case PhoneJoined(user, ref) =>
      context watch ref
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
      sender() ! phones.find { case (u, _) => u == user }.map { case (_, ref) => ref }
    case Terminated(ref) =>
      players.find(_.out == ref).foreach { player =>
        self ! PlayerLeft(player.user)
      }
      phones.find { case (_, saved) => saved == ref }.foreach { case (user, _) =>
        self ! PhoneLeft(user)
      }
  }
}

object BeamClientActor {
  def props(meta: ActorConfig[Username], mediator: ActorRef, isPhone: Boolean) =
    Props(new BeamClientActor(meta, mediator, isPhone))
}

class BeamClientActor(meta: ActorConfig[Username], mediator: ActorRef, isPhone: Boolean)
  extends JsonActor(meta) {
  val user = meta.user

  override def preStart(): Unit = {
    super.preStart()
    val msg =
      if (isPhone) PhoneJoined(user, meta.out)
      else PlayerJoined(user, meta.out)
    mediator ! msg
  }

  override def onMessage(message: JsValue): Unit = {
    val msg =
      if (isPhone) ToPlayer(message, user)
      else ToPhone(message, user)
    mediator ! msg
  }
}
