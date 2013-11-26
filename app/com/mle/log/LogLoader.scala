package com.mle.log

import java.nio.file.Path
import com.mle.util.Util

/**
 *
 * @author mle
 */
trait LogLoader {
  def logFile: Path

  def usingLog[T](logFunction: Iterator[String] => T): T =
    Util.resource(openLog)(src => logFunction(src.getLines()))

  private def openLog = io.Source.fromFile(logFile.toFile)
}
