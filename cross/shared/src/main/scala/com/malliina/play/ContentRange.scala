package com.malliina.play

import com.malliina.storage.{StorageInt, StorageSize}
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Encoder, Json}

import scala.util.Try

case class ContentRange(start: Int, endInclusive: Int, size: StorageSize):
  private val totalSizeBytes: Long = size.toBytes

  def endExclusive = endInclusive + 1
  def contentLength = endExclusive - start
  def contentSize: StorageSize = contentLength.bytes
  def contentRange = s"${ContentRange.BYTES} $start-$endInclusive/$totalSizeBytes"
  def isAll = start == 0 && endInclusive == totalSizeBytes.toInt - 1

  def description: String =
    val total = s"${size.toBytes} bytes"
    if isAll then total
    else s"($start-$endInclusive)/$total"

  override def toString: String = description

object ContentRange:
  val BYTES = "bytes"

  val writer = Encoder[ContentRange]: range =>
    Json.obj(
      "start" -> range.start.asJson,
      "endInclusive" -> range.endInclusive.asJson,
      "size" -> range.size.asJson,
      "isAll" -> range.isAll.asJson,
      "description" -> range.description.asJson
    )
  implicit val json: Codec[ContentRange] = Codec.from(deriveDecoder[ContentRange], writer)

  def all(size: StorageSize) = ContentRange(0, size.toBytes.toInt - 1, size)

  def fromHeader(headerValue: String, size: StorageSize): Try[ContentRange] = Try:
    val sizeBytes = size.toBytes.toInt
    val prefix = s"$BYTES="
    if headerValue.startsWith(prefix) then
      val suffix = headerValue.substring(prefix.length)
      val (start, end) =
        if suffix.startsWith("-") then (sizeBytes - suffix.tail.toInt, sizeBytes - 1)
        else if suffix.endsWith("-") then (suffix.init.toInt, sizeBytes - 1)
        else
          val Array(start, endInclusive) = suffix.split("-")
          (start.toInt, endInclusive.toInt)
      if end >= start then ContentRange(start, end, size)
      else
        throw new IllegalArgumentException(s"End must be greater or equal to start: $headerValue")
    else throw new IllegalArgumentException(s"Does not start with '$prefix': $headerValue")
