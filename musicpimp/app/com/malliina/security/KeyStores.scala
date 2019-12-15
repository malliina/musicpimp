package com.malliina.security

import java.io.FileInputStream
import java.nio.file.{Path, Paths}
import java.security.KeyStore

import com.malliina.exception.ConfigException
import com.malliina.file.FileUtilities
import com.malliina.util.Util

trait KeyStores {
  val (keyStoreKey, keyStorePassKey, keyStoreTypeKey) =
    ("https.keyStore", "https.keyStorePassword", "https.keyStoreType")
  val defaultKeyStoreType = "JKS"

  def validateKeyStoreIfSpecified(): Unit = {
    sysProp(keyStoreKey) foreach (keyStore => {
      val absPath = FileUtilities.pathTo(keyStore).toAbsolutePath
      FileUtilities.verifyFileReadability(absPath)
      val pass = sysProp(keyStorePassKey) getOrElse (throw new ConfigException(
        s"Key $keyStoreKey exists but no corresponding $keyStorePassKey was found."
      ))
      val storeType = sysProp(keyStoreTypeKey) getOrElse defaultKeyStoreType
      validateKeyStore(Paths get keyStore, pass, storeType)
    })
  }

  def validateKeyStore(
    keyStore: Path,
    keyStorePassword: String,
    keyStoreType: String = defaultKeyStoreType
  ): Unit = {
    val ks = KeyStore.getInstance(keyStoreType)
    Util.using(new FileInputStream(keyStore.toFile))(keyStream =>
      ks.load(keyStream, keyStorePassword.toCharArray)
    )
  }

  private def sysProp(key: String) = sys.props.get(key)
}

object KeyStores extends KeyStores
