package com.malliina.musicpimp.messaging.adm

import com.malliina.musicpimp.messaging.PushKeys.{Cmd, Stop, Tag}
import com.malliina.musicpimp.messaging.ServerTag
import com.malliina.musicpimp.messaging.cloud.ADMPayload
import com.malliina.push.android.AndroidMessage

import scala.concurrent.duration.DurationInt

class ADMBuilder {
  def message(tag: ServerTag) =
    AndroidMessage(Map(Cmd -> Stop, Tag -> tag.tag), 60.seconds)

  def buildRequest(dest: ADMDevice) =
    ADMPayload(dest.id, message(dest.tag))
}
