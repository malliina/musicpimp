package com.malliina.pimpcloud.http4s

import cats.effect.Sync
import com.malliina.config.{ConfigError, ConfigNode}
import com.malliina.musicpimp.auth.SecretKey
import com.malliina.pimpcloud.LocalConf
import com.malliina.web.WebLiterals.cid
import com.malliina.web.{AuthConf, ClientSecret}

object CloudConf:
  def parseF[F[_]: Sync]: F[CloudConf] = Sync[F].fromEither(parse)

  private def parse: Either[ConfigError, CloudConf] =
    for
      pimpcloud <- LocalConf.localConf.parse[ConfigNode]("pimpcloud")
      appSecret <- pimpcloud.parse[SecretKey]("secret")
      googleSecret <- pimpcloud.parse[ClientSecret]("google.client.secret")
    yield CloudConf(
      appSecret,
      AuthConf(
        cid"122390040180-0c0kn1hfijved70qh499r48fqgsio5c3.apps.googleusercontent.com",
        googleSecret
      )
    )

case class CloudConf(secret: SecretKey, google: AuthConf)
