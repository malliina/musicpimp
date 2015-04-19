package com.mle.play

import scala.util.Try

/**
 * @author Michael
 */
case class ContentRange(start: Int, endInclusive: Int, size: Int) {
  def endExclusive = endInclusive + 1

  def contentLength = endExclusive - start

  def contentRange = s"bytes $start-$endInclusive/$size"
}

object ContentRange {
  def fromHeader(headerValue: String, size: Int): Try[ContentRange] = Try {
    val prefix = "bytes="
    if (headerValue startsWith prefix) {
      val suffix = headerValue substring prefix.length
      val (start, end) =
        if (suffix startsWith "-") {
          (size - suffix.drop(1).toInt, size - 1)
        } else if (suffix endsWith "-") {
          (suffix.init.toInt, size - 1)
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

//  /**
//   * Removes any overlap between `ranges`.
//   *
//   * For example, given ranges 500-700,601-999, returns one range 500-999.
//   *
//   * @param ranges ranges to minimize
//   * @return
//   */
//  def minimize(ranges: Seq[ContentRange]) = removeOverlap(ranges.sortBy(_.start).toList)
//
//  private def removeOverlap(sortedRanges: List[ContentRange]): List[ContentRange] = {
//    sortedRanges match {
//      case Nil => Nil
//      case onlyOne :: Nil => List(onlyOne)
//      case first :: second :: rest =>
//        if (first.endExclusive >= second.start) {
//          val merged = ContentRange(first.start, math.max(first.endInclusive, second.endInclusive), first.size)
//          removeOverlap(merged +: rest)
//        } else {
//          first :: removeOverlap(second :: rest)
//        }
//    }
//  }
}
