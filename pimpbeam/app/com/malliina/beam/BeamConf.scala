package com.malliina.beam

import play.api.Configuration

case class BeamConf(host: String, port: Int, sslPort: Int) {
  override def toString = s"$host:$port"
}

object BeamConf {
  val hostKey = "beam.host"
  val portKey = "beam.port"
  val sslPortKey = "beam.sslPort"
  val defaultConf = BeamConf("beam.musicpimp.org", port = 80, sslPort = 443)

  def apply(conf: Configuration): BeamConf = fromAppConfOpt(conf).getOrElse(defaultConf)

  def fromAppConfOpt(conf: Configuration): Option[BeamConf] = for {
    host <- conf.getOptional[String](hostKey)
    port <- conf.getOptional[Int](portKey)
    sslPort <- conf.getOptional[Int](sslPortKey)
  } yield BeamConf(host, port, sslPort)
}
