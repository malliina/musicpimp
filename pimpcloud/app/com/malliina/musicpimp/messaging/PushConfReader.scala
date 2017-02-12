package com.malliina.musicpimp.messaging

import java.io.FileInputStream
import java.nio.file.{Path, Paths}
import java.security.KeyStore

import com.malliina.file.StorageFile
import com.malliina.push.wns.WNSCredentials
import com.malliina.util.{BaseConfigReader, Util}

import scala.util.Try

object PushConfReader extends BaseConfigReader[PushConf] {
  val ApnsKeyStore = "apnsKeyStore"
  val ApnsKeyStorePass = "apnsKeyStorePass"
  val ApnsKeyStoreType = "apnsKeyStoreType"
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
    def get(key: String): Option[String] = map get key
    for {
      keyStore <- get(ApnsKeyStore)
      keyStorePass <- get(ApnsKeyStorePass)
      keyStoreType = get(ApnsKeyStoreType) getOrElse DefaultKeyStoreType
      keyStorePath = Paths.get(keyStore)
      ks <- loadKeyStore(keyStorePath, keyStorePass, keyStoreType).toOption
      gcmApiKey <- get(GcmApiKey)
      admClientId <- get(AdmClientId)
      admClientSecret <- get(AdmClientSecret)
      wnsPackageSid <- get(WnsPackageSid)
      wnsClientSecret <- get(WnsClientSecret)
    } yield {
      val apns = APNSCredentials(ks, keyStorePass)
      val adm = ADMCredentials(admClientId, admClientSecret)
      val wns = WNSCredentials(wnsPackageSid, wnsClientSecret)
      PushConf(apns, gcmApiKey, adm, wns)
    }
  }

  def loadKeyStore(keyStore: Path, keyStorePassword: String, keyStoreType: String): Try[KeyStore] = Try {
    val ks = KeyStore.getInstance(keyStoreType)
    Util.using(new FileInputStream(keyStore.toFile))(keyStream => ks.load(keyStream, keyStorePassword.toCharArray))
    ks
  }
}
