package com.malliina.json

import com.malliina.http.FullUrl
import com.malliina.values.ErrorMessage
import play.api.libs.json.Json.toJson
import play.api.libs.json.{Format, JsError, JsSuccess, Reads, Writes}

import scala.concurrent.duration.{Duration, DurationDouble}

object PlayFormats {
  implicit val durationFormat: Format[Duration] = Format[Duration](
    Reads(_.validate[Double].map(_.seconds)),
    Writes(d => toJson(d.toSeconds))
  )
  implicit val url: Format[FullUrl] = fromMapping(FullUrl.build, FullUrl.write)

  def fromMapping[T](
    read: String => Either[ErrorMessage, T],
    write: T => String
  ): Format[T] =
    Format(
      Reads.StringReads.flatMapResult(s =>
        read(s).fold(err => JsError(err.message), ok => JsSuccess(ok))
      ),
      Writes.StringWrites.contramap(write)
    )
}
