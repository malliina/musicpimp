package com.mle.musicpimp.log

import java.nio.file.Paths

import com.mle.file.{FileUtilities, StorageFile}

/**
 *
 * @author mle
 */
trait PimpLog {
  val logDir = sys.props.get("log.dir").fold(FileUtilities.basePath)(str => Paths.get(str))

  def logFile = logDir / "musicpimp.log"
}

object PimpLog extends PimpLog
