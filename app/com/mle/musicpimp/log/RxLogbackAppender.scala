package com.mle.musicpimp.log

import com.mle.logbackrx.BasicBoundedReplayRxAppender

/**
 *
 * @author mle
 */
object RxLogbackAppender extends BasicBoundedReplayRxAppender {
  setTimeFormat("yyyy-MM-dd HH:mm:ss")
}