package com.malliina.pimpcloud.js

import com.malliina.pimpcloud.CloudStrings
import org.musicpimp.js.{BaseLogger, BaseSocket}

import scala.scalajs.js

abstract class SocketJS(wsPath: String)
  extends BaseSocket(wsPath, CloudStrings.Hidden, BaseLogger.noop) {
//  def global = js.Dynamic.global
}
