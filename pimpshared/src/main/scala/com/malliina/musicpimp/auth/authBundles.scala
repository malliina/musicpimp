package com.malliina.musicpimp.auth

import cats.effect.Sync
import cats.implicits.toFunctorOps
import com.malliina.http.Errors
import com.malliina.musicpimp.http4s.Responses
import com.malliina.util.AppLogger
import org.http4s.headers.Location
import org.http4s.{Response, Uri}

trait AuthBundle[F[_], U]:
  def authenticator: Authenticator[F, U]
  def onUnauthorized(failure: Http4sAuthFailure): F[Response[F]]

class AuthBundles[F[_]: Sync](cookies: Http4sAuth[F]) extends Responses[F]:
  private val log = AppLogger(getClass)

  def default[U](auth: Authenticator[F, U]): AuthBundle[F, U] =
    new AuthBundle[F, U]:
      override val authenticator: Authenticator[F, U] = auth

      override def onUnauthorized(failure: Http4sAuthFailure): F[Response[F]] =
        val req = failure.req
        val ip = Proxies2.realAddress(req)
        val resource = req.uri
        log.warn(s"Unauthorized request to '$resource' from '$ip'.")
        unauthorizedNoCache(Errors("Unauthorized."))

  def redirecting[T](redir: Uri, auth: Authenticator[F, T]): AuthBundle[F, T] =
    new AuthBundle[F, T]:
      override def authenticator: Authenticator[F, T] =
        auth

      override def onUnauthorized(failure: Http4sAuthFailure): F[Response[F]] =
        val request = failure.req
        val remoteAddress = Proxies2.realAddress(request)
        log.warn(s"Unauthorized request '${request.uri}' from '$remoteAddress'.")
        pimpResult(request)(
          html = SeeOther(Location(redir)).map(res => cookies.withIntendedUri(request.uri, res)),
          json = accessDenied
        )

  private def pure[T](t: T): F[T] = Sync[F].pure(t)
