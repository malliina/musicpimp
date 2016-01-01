package com.malliina.musicpimp.messaging

import com.malliina.io.FileSet
import play.api.libs.json.Format

/**
  * @author Michael
  */
class AndroidDevices[T <: AndroidDevice[_]](file: String)(implicit format: Format[T])
  extends FileSet[T](file) {

  override protected def id(elem: T): String = elem.tag
}
