package com.malliina.musicpimp.auth

import cats.effect.Concurrent
import com.malliina.http.Errors
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Printer}
import org.http4s.{DecodeResult, EntityDecoder, EntityEncoder}
import org.http4s.circe.CirceInstances

object JsonInstances extends JsonInstances

trait JsonInstances extends CirceInstances:
  override protected val defaultPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)

  given [F[_]]: EntityEncoder[F, Errors] = circeJsonEncoder[F, Errors]

  given circeJsonEncoder[F[_], T: Encoder]: EntityEncoder[F, T] =
    jsonEncoder[F].contramap[T](t => t.asJson)

  def jsonBody[F[_]: Concurrent, A](using decoder: Decoder[A]): EntityDecoder[F, A] =
    jsonDecoder[F].flatMapR: json =>
      json
        .as[A]
        .fold(
          errors => DecodeResult.failureT[F, A](JsonException(errors, json)),
          ok => DecodeResult.successT(ok)
        )
