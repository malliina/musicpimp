package com.malliina.play.auth

import com.malliina.values.Email
import com.malliina.web.{AuthError, GoogleAuthFlow, Verified}
import play.api.mvc.RequestHeader

object GoogleCodeValidator {
  def apply(oauth: OAuthConf[Email]): GoogleCodeValidator = apply(google(oauth))

  def apply(conf: CodeValidationConf[Email]): GoogleCodeValidator =
    new GoogleCodeValidator(conf)

  def google(oauth: OAuthConf[Email]): CodeValidationConf[Email] = CodeValidationConf(
    oauth,
    GoogleAuthFlow.conf(oauth.conf, oauth.http)
  )
}

class GoogleCodeValidator(conf: CodeValidationConf[Email])
  extends GoogleAuthFlow(conf.codeConf)
  with PlayFlow[Verified] {
  override def redirCall = conf.redirCall
  override def onOutcome(outcome: Either[AuthError, Verified], req: RequestHeader) =
    conf.handler.resultFor(outcome.flatMap(parse), req)
}
