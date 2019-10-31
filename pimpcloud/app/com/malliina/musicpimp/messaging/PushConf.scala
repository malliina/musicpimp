package com.malliina.musicpimp.messaging

import com.malliina.push.apns.APNSTokenConf
import com.malliina.push.wns.WNSCredentials
import com.malliina.values.ErrorMessage
import play.api.Configuration

case class PushConf(
  apns: APNSTokenConf,
  gcmApiKey: String,
  adm: ADMCredentials,
  wns: WNSCredentials
)

object PushConf {
  val GcmApiKey = "push.gcm.apiKey"
  val AdmClientId = "push.adm.clientId"
  val AdmClientSecret = "push.adm.clientSecret"
  val WnsPackageSid = "push.wns.packageSid"
  val WnsClientSecret = "push.wns.clientSecret"

  def orFail(conf: Configuration) =
    apply(conf).fold(err => throw new Exception(err.message), identity)

  def apply(conf: Configuration): Either[ErrorMessage, PushConf] = {
    def get(key: String) = conf.getOptional[String](key).toRight(ErrorMessage(s"Missing: '$key'."))

    for {
      gcmApiKey <- get(GcmApiKey)
      admClientId <- get(AdmClientId)
      admClientSecret <- get(AdmClientSecret)
      wnsPackageSid <- get(WnsPackageSid)
      wnsClientSecret <- get(WnsClientSecret)
      apns <- APNSTokenConf.parse(key => get(s"push.apns.$key"))
    } yield {
      val adm = ADMCredentials(admClientId, admClientSecret)
      val wns = WNSCredentials(wnsPackageSid, wnsClientSecret)
      PushConf(apns, gcmApiKey, adm, wns)
    }
  }
}
