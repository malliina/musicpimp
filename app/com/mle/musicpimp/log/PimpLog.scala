package com.mle.musicpimp.log

import com.mle.log.LogLoader
import com.mle.util.FileUtilities

/**
 *
 * @author mle
 */
trait PimpLog extends LogLoader {
  val logFile = FileUtilities pathTo "logs/musicpimp.log"

}

object PimpLog extends PimpLog
