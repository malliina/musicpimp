package com.malliina.play.auth

import com.malliina.values.Email
import com.malliina.web._
import play.api.mvc.RequestHeader

object GitHubCodeValidator {
  def apply(conf: OAuthConf[Email]) = new GitHubCodeValidator(conf)
}

class GitHubCodeValidator(val oauth: OAuthConf[Email])
  extends GitHubAuthFlow(oauth.conf, oauth.http)
  with PlayFlow[Email] {
  override def redirCall = oauth.redirCall
  override def onOutcome(outcome: Either[AuthError, Email], req: RequestHeader) =
    oauth.handler.resultFor(outcome, req)
}
