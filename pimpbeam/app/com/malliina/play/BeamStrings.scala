package com.malliina.play

trait BeamStrings {
  val STATUS = "status"
  val OK = "ok"
  val FAILURE = "failure"
  val VERSION = "version"
  val EXISTS = "exists"
  val READY = "ready"

  val RESET = "reset"
  val DISCONNECTED = "disconnected"
  val BEAM_HOST = "host"
  val PORT = "port"
  val SSL_PORT = "ssl_port"
  val USER = "user"
  val SUPPORTS_PLAINTEXT = "supports_plaintext"
  val SUPPORTS_TLS = "supports_tls"

  val COVER_AVAILABLE = "cover_available"
  val COVER_SOURCE = "source"
  val COVER_RESOURCE = "image"
}

object BeamStrings extends BeamStrings
