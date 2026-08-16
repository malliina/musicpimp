package com.malliina.musicpimp.messaging

import com.malliina.musicpimp.messaging.cloud.GCMPayload
import com.malliina.push.fcm.FCMLegacyClient
import com.malliina.push.gcm.MappedGCMResponse

class GCMHandler(client: FCMLegacyClient) extends PushRequestHandler[GCMPayload, MappedGCMResponse]:
  override def pushOne(request: GCMPayload) =
    client.push(request.token, request.message)
