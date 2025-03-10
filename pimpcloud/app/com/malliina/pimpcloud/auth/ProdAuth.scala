package com.malliina.pimpcloud.auth

import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.models.{CloudID, RequestID}
import com.malliina.pimpcloud.FutureFunctions
import com.malliina.pimpcloud.ws.PhoneConnection
import com.malliina.play.auth.*
import com.malliina.values.Username
import com.malliina.ws.JsonFutureSocket
import controllers.pimpcloud.{ServerRequest, Servers}
import play.api.http.HttpErrorHandler
import play.api.mvc.RequestHeader

import scala.concurrent.{ExecutionContextExecutor, Future}

object ProdAuth:
  val ServerKey = "s"

class ProdAuth(servers: Servers, errorHandler: HttpErrorHandler) extends CloudAuthentication:
  implicit val ec: ExecutionContextExecutor = servers.ctx.executionContext

  override def authServer(
    req: RequestHeader,
    errorHandler: HttpErrorHandler
  ): Future[Either[AuthFailure, ServerRequest]] =
    val requestOpt = for
      requestID <- req.headers.get(JsonFutureSocket.RequestId)
      request <- RequestID.build(requestID).toOption
    yield request
    requestOpt
      .map: reqID =>
        servers.connectedServers.map: ss =>
          findServer(ss, reqID).toRight(InvalidCredentials(req))
      .getOrElse:
        fut(Left(InvalidCredentials(req)))

  override val phone: Authenticator[PhoneConnection] =
    Authenticator(rh => authPhone(rh, errorHandler))
  override val server: Authenticator[ServerRequest] =
    Authenticator(rh => authServer(rh, errorHandler))

  override def authPhone(req: RequestHeader, errorHandler: HttpErrorHandler): PhoneAuthResult =
    connectedServers.flatMap(servers => authPhone(req, servers))

  override def authWebClient(creds: CloudCredentials): PhoneAuthResult =
    connectedServers.flatMap(servers => validate(creds, servers))

  private def findServer(ss: Set[PimpServerSocket], request: RequestID): Option[ServerRequest] =
    ss.find(_.fileTransfers.exists(request)).map(s => ServerRequest(request, s))

  /** @param req
    *   request
    * @return
    *   the socket or a failure
    */
  private def authPhone(req: RequestHeader, servers: Set[PimpServerSocket]): PhoneAuthResult =
    // header -> query -> session
    headerAuth(req, servers).orEither(queryAuth(req, servers)).or(sessionAuth(req, servers))

  private def headerAuth(req: RequestHeader, servers: Set[PimpServerSocket]): PhoneAuthResult =
    PimpAuth
      .cloudCredentials(req)
      .map(creds => validate(creds, servers))
      .getOrElse(missing(req))

  private def queryAuth(rh: RequestHeader, servers: Set[PimpServerSocket]): PhoneAuthResult =
    val maybeResult = for
      s <- rh.queryString get ProdAuth.ServerKey
      server <- s.headOption.map(CloudID.apply)
      creds <- Auth.credentialsFromQuery(rh)
    yield validate(CloudCredentials(server, creds.username, creds.password, rh), servers)
    maybeResult getOrElse missing(rh)

  private def sessionAuth(
    req: RequestHeader,
    servers: Set[PimpServerSocket]
  ): Either[AuthFailure, PhoneConnection] =
    req.session
      .get(Auth.DefaultSessionKey)
      .map(Username.apply)
      .flatMap(user =>
        servers.find(_.id.id == user.name).map(server => PhoneConnection(user, req, server))
      )
      .toRight(InvalidCredentials(req))

  /** @return
    *   a socket or a [[Future]] failed with [[NoSuchElementException]] if validation fails
    */
  private def validate(creds: CloudCredentials, servers: Set[PimpServerSocket]): PhoneAuthResult =
    servers
      .find(_.id == creds.cloudID)
      .map: server =>
        val user = creds.username
        server
          .authenticate(user, creds.password)
          .map: isValid =>
            if isValid then Right(PhoneConnection(user, creds.rh, server))
            else Left(InvalidCredentials(creds.rh))
      .getOrElse:
        fail(creds.rh)

  def fail(rh: RequestHeader) = fut(Left(InvalidCredentials(rh)))

  def missing(rh: RequestHeader) = fut(Left(MissingCredentials(rh)))

  def fut[T](t: T) = Future.successful(t)

  private def connectedServers = servers.connectedServers
