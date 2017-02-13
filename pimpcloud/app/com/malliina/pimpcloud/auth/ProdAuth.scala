package com.malliina.pimpcloud.auth

import java.util.UUID

import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.pimpcloud.json.JsonStrings
import com.malliina.pimpcloud.models.CloudID
import com.malliina.pimpcloud.{CloudCredentials, PimpAuth}
import com.malliina.play.auth.Auth
import com.malliina.play.models.Username
import com.malliina.ws.JsonFutureSocket
import controllers.pimpcloud.{PhoneConnection, Phones, ServerRequest, Servers}
import play.api.mvc.{RequestHeader, Security}

import scala.concurrent.Future

class ProdAuth(servers: Servers) extends CloudAuthentication {
  implicit val ec = servers.mat.executionContext

  override def authServer(req: RequestHeader): Future[ServerRequest] = {
    val uuidOpt = for {
      requestID <- req.headers get JsonStrings.RequestId
      uuid <- JsonFutureSocket.tryParseUUID(requestID)
    } yield uuid
    for {
      uuid <- toFuture(uuidOpt)
      ss <- servers.connectedServers
      server <- toFuture(findServer(ss, uuid))
    } yield server
  }

  override def authPhone(req: RequestHeader): Future[PhoneConnection] =
    connectedServers.flatMap(servers => authPhone(req, servers))

  override def validate(creds: CloudCredentials): Future[PhoneConnection] =
    connectedServers.flatMap(servers => validate(creds, servers))

  private def findServer(ss: Set[PimpServerSocket], uuid: UUID): Option[ServerRequest] =
    ss.find(_.fileTransfers.exists(uuid)).map(s => ServerRequest(uuid, s))

  private def toFuture[T](opt: Option[T]): Future[T] =
    opt.map(Future.successful).getOrElse(Future.failed(new NoSuchElementException))

  /** Fails with a [[NoSuchElementException]] if authentication fails.
    *
    * @param req request
    * @return the socket, if auth succeeds
    */
  private def authPhone(req: RequestHeader, servers: Set[PimpServerSocket]): Future[PhoneConnection] = {
    // header -> query -> session
    headerAuth(req, servers)
      .recoverWithAll(_ => queryAuth(req, servers))
      .recoverAll(_ => sessionAuth(req, servers).get)
  }

  private def headerAuth(req: RequestHeader, servers: Set[PimpServerSocket]): Future[PhoneConnection] =
    flattenInvalid {
      PimpAuth.cloudCredentials(req).map(creds => validate(creds, servers))
    }

  private def queryAuth(req: RequestHeader, servers: Set[PimpServerSocket]): Future[PhoneConnection] =
    flattenInvalid {
      for {
        s <- req.queryString get JsonStrings.ServerKey
        server <- s.headOption.map(CloudID.apply)
        creds <- Auth.credentialsFromQuery(req)
      } yield validate(CloudCredentials(server, creds.username, creds.password), servers)
    }

  private def sessionAuth(req: RequestHeader, servers: Set[PimpServerSocket]): Option[PhoneConnection] = {
    req.session.get(Security.username)
      .map(Username.apply)
      .flatMap(user => servers.find(_.id.id == user.name).map(server => PhoneConnection(user, server)))
  }


  /**
    * @param creds
    * @return a socket or a [[Future]] failed with [[NoSuchElementException]] if validation fails
    */
  private def validate(creds: CloudCredentials, servers: Set[PimpServerSocket]): Future[PhoneConnection] = flattenInvalid {
    servers.find(_.id == creds.cloudID) map { server =>
      val user = creds.username
      server.authenticate(user, creds.password)
        .filter(_ == true)
        .map(_ => PhoneConnection(user, server))
    }
  }

  private def flattenInvalid[T](optFut: Option[Future[T]]) =
    optFut getOrElse Future.failed[T](Phones.invalidCredentials)

  private def connectedServers = servers.connectedServers
}
