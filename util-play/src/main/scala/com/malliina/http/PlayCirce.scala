package com.malliina.http

import _root_.io.circe.{Encoder, Json}
import _root_.io.circe.syntax.EncoderOps
import _root_.io.circe.parser
import org.apache.pekko.util.ByteString
import play.api.http.{ContentTypes, Writeable}
import play.api.libs.json.{JsValue, Writes, Json as PlayJson}
import play.api.mvc.BodyParser

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext

object PlayCirce extends PlayCirce

trait PlayCirce:
  given writer[T: Encoder]: Writes[T] = Writes[T](t => PlayJson.parse(Encoder[T].apply(t).noSpaces))

  implicit def jsonWriteable[T: Encoder]: Writeable[T] = Writeable[T](
    t => ByteString.fromArrayUnsafe(t.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)),
    Option(ContentTypes.JSON)
  )

  def circeParser(playJsonParser: BodyParser[JsValue])(using
    ec: ExecutionContext
  ): BodyParser[Json] =
    playJsonParser.map: playJson =>
      val str = PlayJson.stringify(playJson)
      parser.parse(str).getOrElse(throw IllegalArgumentException("Failed to parse JSON."))
