package com.malliina.musicpimp.auth

import com.malliina.musicpimp.http.PimpContentController
import com.malliina.musicpimp.models.Errors
import com.malliina.play.auth.{AuthFailure, Authenticator}
import com.malliina.play.controllers.AuthBundle
import com.malliina.play.http.Proxies
import play.api.Logger
import play.api.mvc.{Call, Result, Results}

object PimpAuths:
  private val log = Logger(getClass)

  val IntendedUri = "intended_uri"

  def redirecting[T](redir: Call, auth: Authenticator[T]): AuthBundle[T] =
    new AuthBundle[T]:
      override def authenticator: Authenticator[T] =
        auth

      override def onUnauthorized(failure: AuthFailure): Result =
        val request = failure.rh
        val remoteAddress = Proxies.realAddress(request)
        log warn s"Unauthorized request '${request.uri}' from '$remoteAddress'."
        PimpContentController.pimpResult(request)(
          html = Results.Redirect(redir).withSession(IntendedUri -> request.uri),
          json = Errors.accessDenied
        )
