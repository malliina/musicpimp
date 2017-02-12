package com.malliina.musicpimp.messaging

import com.malliina.io.FileSet
import play.api.libs.json.Format

class TaggedDevices[T <: TaggedDevice[_]](file: String)(implicit format: Format[T])
  extends FileSet[T](file) {

  override protected def id(elem: T): String = elem.tag.tag
}
