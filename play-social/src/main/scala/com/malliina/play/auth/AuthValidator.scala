package com.malliina.play.auth

import cats.effect.IO
import com.malliina.web.OAuthKeys.LoginHint
import com.malliina.web.{FlowStart, LoginHint}
import play.api.mvc.{RequestHeader, Result}

trait AuthValidator extends FlowStart[IO] {
  def brandName: String

  /** The initial result that initiates sign-in.
    */
  def start(req: RequestHeader, extraParams: Map[String, String] = Map.empty): IO[Result]

  /** The callback in the auth flow, i.e. the result for redirect URIs.
    */
  def validateCallback(req: RequestHeader): IO[Result]
}

trait LoginHintSupport extends LoginHint[IO] { self: AuthValidator =>
  def startHinted(
    req: RequestHeader,
    loginHint: Option[String],
    extraParams: Map[String, String] = Map.empty
  ): IO[Result] = self.start(
    req,
    extraParams ++ loginHint.map(lh => Map(LoginHint -> lh)).getOrElse(Map.empty)
  )
}

trait OAuthValidator[U] {
  def oauth: OAuthConf[U]
  def handler = oauth.handler
  def redirCall = oauth.redirCall
  def http = oauth.http
  def clientConf = oauth.conf
}
