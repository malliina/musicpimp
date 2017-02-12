package com.malliina.ws

import akka.stream.Materializer
import com.malliina.pimpcloud.ws.StorageSocket
import com.malliina.play.ws.JsonWebSockets

abstract class PhoneActorSockets(mat: Materializer)
  extends JsonWebSockets(mat)
    with StorageSocket
