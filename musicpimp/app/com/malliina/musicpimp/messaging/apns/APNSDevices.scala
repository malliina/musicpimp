package com.malliina.musicpimp.messaging.apns

import com.malliina.musicpimp.messaging.TaggedDevices
import com.malliina.push.apns.APNSToken

object APNSDevices extends TaggedDevices[APNSDevice]("apns.json") {
  def removeAll(tokens: Seq[APNSToken]): Seq[APNSDevice] =
    removeWhere(d => tokens.contains(d.id))
}
