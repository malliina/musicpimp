package com.mle.musicpimp.log

import java.nio.file.Paths

import com.mle.util.FileImplicits.StorageFile
import com.mle.util.FileUtilities

/**
 *
 * @author mle
 */
trait PimpLog {
  val logDir = sys.props.get("log.dir").fold(FileUtilities.basePath)(str => Paths.get(str))

  def logFile = logDir / "musicpimp.log"
}

object PimpLog extends PimpLog
