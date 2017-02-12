package com.malliina.play

import com.malliina.json.JsonFormats
import com.malliina.storage.StorageSize
import com.malliina.util.Log
import play.api.http.HeaderNames.RANGE
import play.api.libs.json.{Format, Json, Writes}
import play.api.mvc.RequestHeader

import scala.util.{Failure, Try}

case class ContentRange(start: Int, endInclusive: Int, size: StorageSize) {
  val totalSizeBytes = size.toBytes

  def endExclusive = endInclusive + 1

  def contentLength = endExclusive - start

  def contentRange = s"${ContentRange.BYTES} $start-$endInclusive/$totalSizeBytes"

  def isAll = start == 0 && endInclusive == totalSizeBytes.toInt - 1

  def description = {
    val total = s"${size.toBytes} bytes"
    if (isAll) total
    else s"($start-$endInclusive)/$total"
  }

  override def toString = description
}

object ContentRange extends Log {

  implicit val ssf = JsonFormats.storageSizeFormat
  //  implicit val json = Json.format[ContentRange]
  val writer = Writes[ContentRange](range => Json.obj(
    "start" -> range.start,
    "endInclusive" -> range.endInclusive,
    "size" -> range.size,
    "isAll" -> range.isAll,
    "description" -> range.description
  ))
  implicit val format = Format(Json.reads[ContentRange], writer)

  val BYTES = "bytes"

  def all(size: StorageSize) = ContentRange(0, size.toBytes.toInt - 1, size)

  def fromHeaderOrAll(request: RequestHeader, size: StorageSize): ContentRange =
    fromHeader(request, size) getOrElse all(size)

  def fromHeader(request: RequestHeader, size: StorageSize): Try[ContentRange] = {
    request.headers.get(RANGE)
      .map(range => fromHeader(range, size))
      .getOrElse(Failure(new IllegalArgumentException(s"Missing $RANGE header.")))
  }

  def fromHeader(headerValue: String, size: StorageSize): Try[ContentRange] = Try {
    val sizeBytes = size.toBytes.toInt
    val prefix = s"$BYTES="
    if (headerValue startsWith prefix) {
      val suffix = headerValue substring prefix.length
      val (start, end) =
        if (suffix startsWith "-") {
          (sizeBytes - suffix.tail.toInt, sizeBytes - 1)
        } else if (suffix endsWith "-") {
          (suffix.init.toInt, sizeBytes - 1)
        } else {
          val Array(start, endInclusive) = suffix split "-"
          (start.toInt, endInclusive.toInt)
        }
      if (end >= start) {
        ContentRange(start, end, size)
      } else {
        throw new IllegalArgumentException(s"End must be greater or equal to start: $headerValue")
      }
    } else {
      throw new IllegalArgumentException(s"Does not start with '$prefix': $headerValue")
    }
  }
}
