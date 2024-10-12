package com.malliina.json

import com.malliina.http.FullUrl
import com.malliina.values.{Email, ErrorMessage, Password, UnixPath, Username, ValidatingCompanion}
import io.circe.{Decoder, Encoder}
import play.api.libs.json.Json.toJson
import play.api.libs.json.{Format, JsError, JsSuccess, Json, Reads, Writes}

import scala.concurrent.duration.{Duration, DurationDouble}
import io.circe.syntax.EncoderOps
import scala.language.implicitConversions

object SharedPlayFormats extends SharedPlayFormats

trait SharedPlayFormats:
  implicit val durationFormat: Format[Duration] = Format[Duration](
    Reads(_.validate[Double].map(_.seconds)),
    Writes(d => toJson(d.toSeconds))
  )
  implicit val url: Format[FullUrl] = fromMapping(FullUrl.build, FullUrl.write)
  implicit val unix: Format[UnixPath] = fromMapping(UnixPath.build, UnixPath.write)
  implicit val user: Format[Username] = from(Username)
  implicit val pass: Format[Password] = from(Password)
  implicit val emailJson: Format[Email] = from(Email)

  def from[T](comp: ValidatingCompanion[String, T]): Format[T] =
    fromMapping(comp.build, comp.write)
  def fromLong[T](comp: ValidatingCompanion[Long, T]): Format[T] =
    fromMapping(comp.build, comp.write)

  def fromMapping[S: Reads: Writes, T](
    read: S => Either[ErrorMessage, T],
    write: T => S
  ): Format[T] =
    Format(
      implicitly[Reads[S]].flatMapResult(s =>
        read(s).fold(err => JsError(err.message), ok => JsSuccess(ok))
      ),
      implicitly[Writes[S]].contramap(write)
    )

  def writer[T: Encoder]: Writes[T] = (t: T) => Json.parse(t.asJson.noSpaces)

  implicit def decoder[T](using play: Reads[T]): Decoder[T] = Decoder.decodeJson.emap: json =>
    try
      Json
        .parse(json.noSpaces)
        .validate[T]
        .asEither
        .left
        .map: err =>
          err.flatMap(_._2).map(_.message).headOption.getOrElse(s"Failed to parse JSON.")
    catch case e: Exception => Left(s"JSON parsing failure at $e")
