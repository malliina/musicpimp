package com.malliina.beam

import cats.effect.Sync
import com.malliina.config.ConfigNode
import com.malliina.musicpimp.auth.SecretKey

import java.nio.file.Paths

case class BeamConf(secret: SecretKey, host: String, port: Int, sslPort: Int):
  override def toString = s"$host:$port"

object BeamConf:
  val userHome = Paths.get(sys.props("user.home"))
  val appDir = userHome.resolve(".pimpcloud")
  def local(file: String) = ConfigNode.default(appDir.resolve(file))
  val localConf = local("pimpcloud.conf")

  val hostKey = "beam.host"
  val portKey = "beam.port"
  val sslPortKey = "beam.sslPort"
//  val defaultConf = BeamConf("beam.musicpimp.org", port = 80, sslPort = 443)

  def parseF[F[_]: Sync]: F[BeamConf] = Sync[F].fromEither(parse)

  private def parse =
    for
      root <- localConf.parse[ConfigNode]("pimpbeam")
      conf <- parseNode(root)
    yield conf

  private def parseNode(conf: ConfigNode) = for
    secret <- conf.parse[SecretKey]("secret")
    host <- conf.parse[String](hostKey)
    port <- conf.parse[Int](portKey)
    sslPort <- conf.parse[Int](sslPortKey)
  yield BeamConf(secret, host, port, sslPort)
