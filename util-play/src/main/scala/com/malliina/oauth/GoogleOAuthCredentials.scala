package com.malliina.oauth

import com.malliina.play.util.ConfOps
import com.malliina.values.ErrorMessage
import play.api.Configuration

trait GoogleOAuthKey {
  def clientId: String

  def clientSecret: String
}

case class GoogleOAuthCredentials(clientId: String, clientSecret: String, scope: String)
  extends GoogleOAuthKey

object GoogleOAuthCredentials {
  def apply(conf: Configuration): Either[ErrorMessage, GoogleOAuthCredentials] =
    for {
      clientId <- conf.read("google.client.id")
      clientSecret <- conf.read("google.client.secret")
      scope <- conf.read("google.client.scope")
    } yield GoogleOAuthCredentials(clientId, clientSecret, scope)
}

case class GoogleOAuthCreds(clientId: String, clientSecret: String) extends GoogleOAuthKey
