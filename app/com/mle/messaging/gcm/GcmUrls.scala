package com.mle.messaging.gcm

import com.mle.musicpimp.util.FileSet

/**
 *
 * @author mle
 */
object GcmUrls extends FileSet[GcmUrl]("gcm.json") {
  override def id(elem: GcmUrl): String = elem.id
}