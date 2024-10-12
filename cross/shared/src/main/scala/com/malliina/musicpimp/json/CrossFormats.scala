package com.malliina.musicpimp.json

import com.malliina.musicpimp.js.FrontStrings.EventKey
import io.circe.{Codec, Decoder, Encoder}

import scala.concurrent.duration.{Duration, DurationDouble, DurationLong, FiniteDuration}

object CrossFormats:
  implicit val duration: Codec[Duration] = Codec.from(
    Decoder.decodeLong.map(_.seconds),
    Encoder.encodeLong.contramap[Duration](_.toSeconds)
  )

  implicit val finiteDuration: Codec[FiniteDuration] = Codec.from(
    Decoder.decodeDouble.map(_.seconds),
    Encoder.encodeDouble.contramap[FiniteDuration](_.toSeconds.toDouble)
  )

  def singleEvent[T](value: String, t: T): Codec[T] = evented(value, pure(t))

  def evented[T](eventName: String, payload: Codec[T]): Codec[T] =
    keyValued(EventKey, eventName, payload)

  def singleCmd[T](value: String, t: T) = cmd(value, pure(t))

  def cmd[T](value: String, payload: Codec[T]): Codec[T] =
    keyValued(CommonStrings.Cmd, value, payload)

  def pure[T](value: T): Codec[T] = Codec.from(Decoder.const(value), (t: T) => io.circe.Json.obj())

  import io.circe.syntax.EncoderOps

  /** A JSON format for objects of type T that contains a top-level key-value pair.
    */
  def keyValued[T](key: String, value: String, payload: Codec[T]): Codec[T] =
    val decoder: Decoder[T] = Decoder.decodeJson.emap: json =>
      json.hcursor
        .downField(key)
        .as[String]
        .left
        .map(_.message)
        .flatMap: v =>
          if v == value then payload.decodeJson(json).left.map(_.message)
          else Left(s"Was $v, expected $value.")
    val encoder: Encoder[T] = Encoder.encodeJson.contramap: t =>
      io.circe.Json.obj(key -> value.asJson).deepMerge(payload(t))
    Codec.from(decoder, encoder)
