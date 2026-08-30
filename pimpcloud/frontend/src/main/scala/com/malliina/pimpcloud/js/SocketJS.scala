package com.malliina.pimpcloud.js

import com.malliina.pimpcloud.CloudStrings
import org.musicpimp.js.{BaseLogger, BaseSocket}

abstract class SocketJS(wsPath: String)
  extends BaseSocket(wsPath, CloudStrings.Hidden, BaseLogger.noop)
