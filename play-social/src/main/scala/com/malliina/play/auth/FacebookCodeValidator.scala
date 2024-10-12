package com.malliina.play.auth

import com.malliina.values.Email
import com.malliina.web._
import play.api.mvc.RequestHeader

object FacebookCodeValidator {
  def apply(conf: OAuthConf[Email]) = new FacebookCodeValidator(conf)
}

class FacebookCodeValidator(val oauth: OAuthConf[Email])
  extends FacebookAuthFlow(oauth.conf, oauth.http)
  with PlaySupport[Email] {
  override def redirCall = oauth.redirCall
  override def onOutcome(outcome: Either[AuthError, Email], req: RequestHeader) =
    oauth.handler.resultFor(outcome, req)
}
