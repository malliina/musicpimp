package com.malliina.oauth

import com.malliina.play.util.ConfOps
import com.malliina.values.ErrorMessage
import play.api.Configuration

case class DiscoGsOAuthCredentials(
  consumerKey: String,
  consumerSecret: String,
  accessToken: String,
  accessTokenSecret: String
)

object DiscoGsOAuthCredentials {
  def apply(conf: Configuration): Either[ErrorMessage, DiscoGsOAuthCredentials] =
    for {
      consumerKey <- conf.read("discogs.consumer.key")
      consumerSecret <- conf.read("discogs.consumer.secret")
      accessToken <- conf.read("discogs.access.token")
      accessTokenSecret <- conf.read("discogs.access.secret")
    } yield DiscoGsOAuthCredentials(consumerKey, consumerSecret, accessToken, accessTokenSecret)
}
