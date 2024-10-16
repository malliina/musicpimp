package com.malliina.musicpimp.messaging.gcm

import com.malliina.musicpimp.messaging.PushKeys.{Cmd, Stop, Tag}
import com.malliina.musicpimp.messaging.ServerTag
import com.malliina.musicpimp.messaging.cloud.GCMPayload
import com.malliina.push.gcm.GCMMessage

class GCMBuilder:
  def message(tag: ServerTag) = GCMMessage(Map(Cmd -> Stop, Tag -> tag.tag))

  def buildRequest(dest: GCMDevice) = GCMPayload(dest.id, message(dest.tag))
