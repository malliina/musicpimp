package com.malliina.musicpimp.stats

import com.malliina.http.Errors
import com.malliina.http4s.QueryParsers
import io.circe.{Codec, Decoder, Json}
import org.http4s.Request

case class ItemLimits(from: Int, until: Int) derives Codec.AsObject

object ItemLimits:
  val DefaultItemCount = 300
  val From = "from"
  val Until = "until"

  val Default = ItemLimits(0, DefaultItemCount)

  def fromJson(body: Json): Decoder.Result[ItemLimits] =
    def readInt(key: String, default: Int) =
      body.hcursor.downField(key).as[Option[Int]].map(_ getOrElse default)
    for
      from <- readInt(From, 0)
      until <- readInt(Until, from + DefaultItemCount)
    yield ItemLimits(from, until)

  def fromReq(request: Request[?]): Either[Errors, ItemLimits] =
    val q = request.uri.query
    for
      from <- QueryParsers.parseOrDefault(q, From, 0)
      until <- QueryParsers.parseOrDefault(q, Until, from + DefaultItemCount)
    yield ItemLimits(from, until)
