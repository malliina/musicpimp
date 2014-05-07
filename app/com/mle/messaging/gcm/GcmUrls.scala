package com.mle.messaging.gcm

import com.mle.messaging.mpns.PushSet

/**
 *
 * @author mle
 */
object GcmUrls extends PushSet[GcmUrl]("gcm.json") {
  override def id(elem: GcmUrl): String = elem.id
}