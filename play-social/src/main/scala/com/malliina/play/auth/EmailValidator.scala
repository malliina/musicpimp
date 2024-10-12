package com.malliina.play.auth

import com.malliina.values.Email
import com.malliina.web.{AuthError, EmailAuthFlow, JWTError, Verified}
import play.api.mvc.RequestHeader

object EmailValidator {
  def apply(conf: CodeValidationConf[Email]): EmailValidator =
    new EmailValidator(conf)

  def map[T](
    c: CodeValidationConf[Email]
  )(parseUser: Verified => Either[JWTError, Email]): EmailValidator =
    new EmailValidator(c) {
      override def parse(v: Verified): Either[JWTError, Email] = parseUser(v)
    }
}

class EmailValidator(conf: CodeValidationConf[Email])
  extends EmailAuthFlow(conf.codeConf)
  with PlaySupport[Verified] {
  override def redirCall = conf.redirCall
  override def onOutcome(outcome: Either[AuthError, Verified], req: RequestHeader) =
    conf.handler.resultFor(outcome.flatMap(parse), req)
}
