package com.malliina.play

import com.malliina.json.JsonFormats
import com.malliina.musicpimp.models.RangeLike
import com.malliina.storage.{StorageInt, StorageSize}
import play.api.http.HeaderNames._
import play.api.libs.json.{Format, Json, Writes}
import play.api.mvc.RequestHeader

import scala.util.{Failure, Try}

case class ContentRange(start: Int, endInclusive: Int, size: StorageSize) extends RangeLike {
  val totalSizeBytes = size.toBytes

  def endExclusive = endInclusive + 1

  def contentLength = endExclusive - start

  def contentSize: StorageSize = contentLength.bytes

  def contentRange = s"${ContentRange.BYTES} $start-$endInclusive/$totalSizeBytes"

  def isAll = start == 0 && endInclusive == totalSizeBytes.toInt - 1

  def description = {
    val total = s"${size.toBytes} bytes"
    if (isAll) total
    else s"($start-$endInclusive)/$total"
  }

  override def toString: String = description
}

object ContentRange {
  val BYTES = "bytes"

  implicit val ssf = JsonFormats.storageSizeFormat
  val writer = Writes[ContentRange](range => Json.obj(
    "start" -> range.start,
    "endInclusive" -> range.endInclusive,
    "size" -> range.size,
    "isAll" -> range.isAll,
    "description" -> range.description
  ))
  implicit val json = Format(Json.reads[ContentRange], writer)

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
