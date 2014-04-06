package com.mle.play

import play.api.mvc.RequestHeader
import com.mle.util.FileUtilities

/**
 *
 * @author mle
 */
trait PlayUtils {
  def headersString(request: RequestHeader) =
    request.headers.toSimpleMap
      .map(kv => kv._1 + "=" + kv._2)
      .mkString(FileUtilities.lineSep, FileUtilities.lineSep, "")
}

object PlayUtils extends PlayUtils
