package com.mle.musicpimp.messaging

import com.mle.io.FileSet

/**
 * @author Michael
 */
class AndroidDevices(file: String) extends FileSet[AndroidDevice](file) {
  override protected def id(elem: AndroidDevice): String = elem.tag
}
