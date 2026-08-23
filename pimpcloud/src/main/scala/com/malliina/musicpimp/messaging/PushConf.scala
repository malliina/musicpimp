package com.malliina.musicpimp.messaging

import cats.data.NonEmptyList
import com.malliina.config.{ConfigError, ConfigNode, InvalidValue}
import com.malliina.push.apns.APNSTokenConf
import com.malliina.push.wns.WNSCredentials
import com.malliina.values.ErrorMessage

case class PushConf(
  apns: APNSTokenConf,
  gcmApiKey: String,
  adm: ADMCredentials,
  wns: WNSCredentials
)

object PushConf:
  val GcmApiKey = "push.gcm.apiKey"
  val AdmClientId = "push.adm.clientId"
  val AdmClientSecret = "push.adm.clientSecret"
  val WnsPackageSid = "push.wns.packageSid"
  val WnsClientSecret = "push.wns.clientSecret"

  def orFail(conf: ConfigNode) =
    apply(conf).fold(err => throw new Exception(err.message.message), identity)

  def apply(conf: ConfigNode): Either[ConfigError, PushConf] =
    def get(key: String) = conf.parse[String](key)

    for
      gcmApiKey <- get(GcmApiKey)
      admClientId <- get(AdmClientId)
      admClientSecret <- get(AdmClientSecret)
      wnsPackageSid <- get(WnsPackageSid)
      wnsClientSecret <- get(WnsClientSecret)
      apns <- APNSTokenConf
        .parse(key => get(s"push.apns.$key").left.map(_.message))
        .left
        .map(e => InvalidValue(e, NonEmptyList.of("apns"), None))
    yield
      val adm = ADMCredentials(admClientId, admClientSecret)
      val wns = WNSCredentials(wnsPackageSid, wnsClientSecret)
      PushConf(apns, gcmApiKey, adm, wns)
