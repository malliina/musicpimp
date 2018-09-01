package com.malliina.pimpcloud.auth

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.models.{CloudID, RequestID}
import com.malliina.pimpcloud.ws.{NoopActor, PhoneConnection}
import com.malliina.play.auth.{AuthFailure, Authenticator, InvalidCredentials}
import com.malliina.values.Username
import controllers.pimpcloud.ServerRequest
import play.api.http.HttpErrorHandler
import play.api.mvc.RequestHeader

import scala.concurrent.Future

class FakeAuth(as: ActorSystem, mat: Materializer, errorHandler: HttpErrorHandler) extends CloudAuthentication {
  private var currentServer: Option[ServerRequest] = None

  override lazy val phone: Authenticator[PhoneConnection] = Authenticator(rh => authPhone(rh, errorHandler))

  override lazy val server: Authenticator[ServerRequest] = Authenticator(rh => authServer(rh, errorHandler))

  override def authServer(req: RequestHeader, errorHandler: HttpErrorHandler): Future[Either[AuthFailure, ServerRequest]] =
    Future.successful(Right(getOrInit(req, errorHandler)))

  override def authPhone(req: RequestHeader, errorHandler: HttpErrorHandler): PhoneAuthResult =
    Future.successful(Right(PhoneConnection(Username("test"), req, getOrInit(req, errorHandler).socket)))

  override def authWebClient(creds: CloudCredentials): PhoneAuthResult =
    Future.successful(Left(InvalidCredentials(creds.rh)))

  def getOrInit(req: RequestHeader, errorHandler: HttpErrorHandler) = {
    val s: ServerRequest = currentServer.getOrElse(ServerRequest(FakeAuth.FakeUuid, fakeServerSocket(req, errorHandler, mat)))
    currentServer = Option(s)
    s
  }

  def fakeServerSocket(req: RequestHeader, errorHandler: HttpErrorHandler, mat: Materializer): PimpServerSocket = {
    val actor = as.actorOf(NoopActor.props())
    new PimpServerSocket(actor, CloudID("test"), req, mat, errorHandler, () => ())
  }
}

object FakeAuth {
  val FakeUuid = RequestID.build("d3ef33ab-5ba5-4a34-bf7c-9a182c882ab7").get
}
