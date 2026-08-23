package com.malliina.pimpcloud.ws

import com.malliina.musicpimp.models.CloudID
import io.circe.Json

object ServerMediator:
  case class ServerEvent(message: Json, from: CloudID)
  case class Exists(id: CloudID)
  case object GetServers
  case object StreamsUpdated
