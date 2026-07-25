package com.malliina.musicpimp.app

import com.malliina.config.ConfigNode
import com.malliina.musicpimp.auth.SecretKey
import com.malliina.util.AppLogger
import com.typesafe.config.ConfigFactory
import play.api.Configuration

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

object LocalConf:
  private val log = AppLogger(getClass)

  val userHome = Paths.get(sys.props("user.home"))
  val appDir = userHome.resolve(".musicpimp")
  def local(file: String) = ConfigNode.default(appDir.resolve(file))
  val localConf = local("musicpimp.conf")
  val charset = StandardCharsets.UTF_8
  val secretPlaceholder = SecretKey("changeme")

  def readOrGenerateSecret(file: Path): SecretKey =
    if Files.exists(file) && Files.isReadable(file) then
      SecretKey(new String(Files.readAllBytes(file), charset))
    else
      val secret = InitOptions.generateSecret()
      Option(file.getParent).foreach(dir => Files.createDirectories(dir))
      Files.write(file, secret.getBytes(charset))
      log.info(s"Generated random secret key and saved it to '$file'.")
      SecretKey(secret)
