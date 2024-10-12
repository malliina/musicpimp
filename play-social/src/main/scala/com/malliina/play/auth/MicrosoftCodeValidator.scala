package com.malliina.play.auth

import com.malliina.values.Email
import com.malliina.web._

object MicrosoftCodeValidator {
  def apply[U](oauth: OAuthConf[Email]) = EmailValidator(microsoft(oauth))

  def microsoft[U](oauth: OAuthConf[U]) = CodeValidationConf(
    oauth,
    MicrosoftAuthFlow.conf(oauth.conf, oauth.http)
  )
}
