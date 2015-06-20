package com.mle.musicpimp.log

import java.nio.file.Paths

import com.mle.file.{FileUtilities, StorageFile}
import com.mle.musicpimp.util.FileUtil

/**
 *
 * @author mle
 */
trait PimpLog {
  val logDir = sys.props.get("log.dir").map(str => Paths.get(str)).getOrElse(FileUtil.pimpHomeDir / "logs")

  def logFile = logDir / "musicpimp.log"
}

object PimpLog extends PimpLog
