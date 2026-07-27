package com.malliina.json

import io.circe.{Codec, Decoder, Encoder}

trait JsonEnum[T]:
  def all: Seq[T]

  def resolveName(item: T): String

  def withName(name: String): Option[T] =
    all.find(i => resolveName(i).toLowerCase == name.toLowerCase)

  private def allNames = all.map(resolveName).mkString(", ")

  given json: Codec[T] = Codec.from(
    Decoder.decodeString.emap(s =>
      withName(s).toRight(s"Unknown name: $s. Must be one of: $allNames.")
    ),
    Encoder.encodeString.contramap(resolveName)
  )
