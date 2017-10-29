package com.malliina.musicpimp.messaging

import com.malliina.musicpimp.messaging.cloud.GCMPayload
import com.malliina.push.gcm.{GCMClient, MappedGCMResponse}

class GCMHandler(client: GCMClient) extends PushRequestHandler[GCMPayload, MappedGCMResponse] {
  override def pushOne(request: GCMPayload) =
    client.push(request.token, request.message)
}
