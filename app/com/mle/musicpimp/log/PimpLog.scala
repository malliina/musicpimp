package com.mle.musicpimp.log

import com.mle.log.LogLoader
import java.nio.file.Paths
import com.mle.util.Implicits._
import com.mle.util.FileUtilities

/**
 *
 * @author mle
 */
trait PimpLog extends LogLoader {
  val logDir = sys.props.get("log.dir").map(str => Paths.get(str)) getOrElse FileUtilities.basePath

  def logFile = logDir / "musicpimp.log"
}

object PimpLog extends PimpLog
