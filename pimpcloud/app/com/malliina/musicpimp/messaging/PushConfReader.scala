package com.malliina.musicpimp.messaging

import java.nio.file.{Path, Paths}

import com.malliina.file.StorageFile
import com.malliina.push.apns.APNSTokenConf
import com.malliina.push.wns.WNSCredentials
import com.malliina.util.BaseConfigReader
import com.malliina.values.ErrorMessage

object PushConfReader extends BaseConfigReader[PushConf] {
  val GcmApiKey = "gcmApiKey"
  val AdmClientId = "admClientId"
  val AdmClientSecret = "admClientSecret"
  val WnsPackageSid = "wnsPackageSid"
  val WnsClientSecret = "wnsClientSecret"

  val DefaultKeyStoreType = "PKCS12"

  val DefaultFilePath = userHome / "keys" / "push.conf"

  val PushConfKey = "push.conf"

  override def filePath: Option[Path] =
    Option(sys.props.get(PushConfKey).map(Paths.get(_)) getOrElse DefaultFilePath)

  override def fromMapOpt(map: Map[String, String]): Option[PushConf] = {
    def get(key: String) = (map get key).toRight(ErrorMessage(s"Missing: '$key'."))

    val either = for {
      gcmApiKey <- get(GcmApiKey)
      admClientId <- get(AdmClientId)
      admClientSecret <- get(AdmClientSecret)
      wnsPackageSid <- get(WnsPackageSid)
      wnsClientSecret <- get(WnsClientSecret)
      apns <- APNSTokenConf.parse(key => get(key))
    } yield {
      val adm = ADMCredentials(admClientId, admClientSecret)
      val wns = WNSCredentials(wnsPackageSid, wnsClientSecret)
      PushConf(apns, gcmApiKey, adm, wns)
    }
    either.toOption
  }
}
