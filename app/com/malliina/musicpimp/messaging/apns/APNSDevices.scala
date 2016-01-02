package com.malliina.musicpimp.messaging.apns

import com.malliina.io.FileSet
import com.malliina.push.apns.APNSToken

/**
  * @author mle
  */
object APNSDevices extends FileSet[APNSToken]("apns.json") {
  override protected def id(elem: APNSToken): String = elem.token
}
