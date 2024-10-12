package com.malliina.play.auth

import cats.effect.IO
import com.malliina.play.http.FullUrls
import com.malliina.values.{AccessToken, Email}
import com.malliina.web.TwitterAuthFlow.{OauthTokenKey, OauthVerifierKey}
import com.malliina.web._
import play.api.mvc.Results.Redirect
import play.api.mvc.{RequestHeader, Result}

object TwitterValidator {
  def apply(oauth: OAuthConf[Email]): TwitterValidator = new TwitterValidator(oauth)
}

class TwitterValidator(val oauth: OAuthConf[Email])
  extends TwitterAuthFlow(oauth.conf, oauth.http) {
  val handler = oauth.handler

  def start(req: RequestHeader, extraParams: Map[String, String] = Map.empty): IO[Result] =
    requestToken(FullUrls(oauth.redirCall, req)).map { e =>
      e.fold(
        err => handler.onUnauthorized(err, req),
        token =>
          Redirect(authTokenUrl(token).url)
            .addingToSession(RequestToken.Key -> token.token)(req)
      )
    }

  def validateCallback(req: RequestHeader): IO[Result] = {
    val maybe = for {
      token <- req.getQueryString(OauthTokenKey).map(AccessToken.apply)
      requestToken <- req.session.get(RequestToken.Key).map(AccessToken.apply)
      verifier <- req.getQueryString(OauthVerifierKey)
    } yield {
      validateTwitterCallback(token, requestToken, verifier).map { e =>
        e.fold(
          err => handler.onUnauthorized(err, req),
          user => handler.resultFor(user.email.toRight(OAuthError("Email missing.")), req)
        )
      }
    }
    maybe.getOrElse(
      IO.pure(handler.onUnauthorized(OAuthError("Invalid callback parameters."), req))
    )
  }
}
