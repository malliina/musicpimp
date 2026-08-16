package com.malliina.pimpcloud.auth

import cats.effect.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.malliina.musicpimp.auth.{Auth2, Authenticator, Http4sAuth, Http4sAuthFailure, InvalidCredentials, MissingCredentials, UserPayload}
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.models.{CloudID, RequestID}
import com.malliina.pimpcloud.http4s.{ServerRequest, Servers}
import com.malliina.pimpcloud.ws.PhoneConnection
import com.malliina.ws.JsonFutureSocket
import org.http4s.Request
import org.typelevel.ci.CIString

object ProdAuth:
  private val ServerKey = "s"

class ProdAuth[F[_]: Async](servers: Servers[F], cookies: Http4sAuth[F])
  extends CloudAuthentication[F]:
  val F = Async[F]

  override def authServer(
    req: Request[F]
  ): F[Either[Http4sAuthFailure, ServerRequest[F]]] =
    val requestOpt = for
      requestID <- req.headers.get(CIString(JsonFutureSocket.RequestId)).map(_.head.value)
      request <- RequestID.build(requestID).toOption
    yield request
    requestOpt
      .map: reqID =>
        servers.connectedServers.map: ss =>
          findServer(ss, reqID).toRight(InvalidCredentials(req))
      .getOrElse:
        F.pure(Left(InvalidCredentials(req)))

  override val phone: Authenticator[F, PhoneConnection[F]] =
    Authenticator.make(rh => authPhone(rh))
  override val server: Authenticator[F, ServerRequest[F]] =
    Authenticator.make(rh => authServer(rh))

  override def authPhone(req: Request[F]): PhoneAuthResult =
    connectedServers.flatMap(servers => authPhone(req, servers))

  override def authWebClient(creds: CloudCredentials): PhoneAuthResult =
    connectedServers.flatMap(servers => validate(creds, servers))

  private def findServer(
    ss: Set[PimpServerSocket[F]],
    request: RequestID
  ): Option[ServerRequest[F]] =
    ss.find(_.fileTransfers.exists(request)).map(s => ServerRequest(request, s))

  /** @param req
    *   request
    * @return
    *   the socket or a failure
    */
  private def authPhone(req: Request[F], servers: Set[PimpServerSocket[F]]): PhoneAuthResult =
    // header -> query -> session
    headerAuth(req, servers)
      .orEither(queryAuth(req, servers))
      .orEither(F.pure(sessionAuth(req, servers)))

  private def headerAuth(req: Request[F], servers: Set[PimpServerSocket[F]]): PhoneAuthResult =
    PimpAuth
      .cloudCredentials(req)
      .map(creds => validate(creds, servers))
      .getOrElse(missing(req))

  private def queryAuth(rh: Request[F], servers: Set[PimpServerSocket[F]]): PhoneAuthResult =
    val maybeResult = for
      s <- rh.uri.query.params.get(ProdAuth.ServerKey)
      server = CloudID(s)
      creds <- Auth2.credentialsFromQuery(rh.uri)
    yield validate(CloudCredentials(server, creds.username, creds.password, rh), servers)
    maybeResult.getOrElse(missing(rh))

  private def sessionAuth(
    req: Request[F],
    servers: Set[PimpServerSocket[F]]
  ): Either[Http4sAuthFailure, PhoneConnection[F]] = cookies
    .auth(req)
    .flatMap: user =>
      servers
        .find(_.id.id == user.username.name)
        .map: server =>
          PhoneConnection(user.username, req, server)
        .toRight(InvalidCredentials(req))

  /** @return
    *   a socket or a task failed with [[NoSuchElementException]] if validation fails
    */
  private def validate(
    creds: CloudCredentials,
    servers: Set[PimpServerSocket[F]]
  ): PhoneAuthResult =
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

  def fail(rh: Request[?]): PhoneAuthResult = F.pure(Left(InvalidCredentials(rh)))

  private def missing(rh: Request[?]): PhoneAuthResult = F.pure(Left(MissingCredentials(rh)))

  private def connectedServers = servers.connectedServers

  extension [L, R](task: F[Either[L, R]])
    def orEither(ifLeft: => F[Either[L, R]]): F[Either[L, R]] =
      task.flatMap(e => e.fold(_ => ifLeft, r => F.pure(Right(r))))
