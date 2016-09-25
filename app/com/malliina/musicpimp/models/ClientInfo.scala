package com.malliina.musicpimp.models

import akka.stream.scaladsl.SourceQueue
import com.malliina.musicpimp.json.JsonFormatVersions
import com.malliina.musicpimp.models.ClientInfo.log
import com.malliina.play.models.Username
import com.malliina.play.ws.SocketClient
import controllers.PimpRequest
import play.api.Logger
import play.api.http.MimeTypes
import play.api.mvc.RequestHeader
/**
  * @param channel channel used to push messages to the client
  * @param request the request headers from the HTTP request that initiated the WebSocket connection
  * @param user    the authenticated username
  */
case class ClientInfo[T](channel: SourceQueue[T], request: RequestHeader, user: Username)
  extends SocketClient[T] {
  val protocol = if (request.secure) "wss" else "ws"
  val remoteAddress = request.remoteAddress
  val describe = s"$protocol://${user.name}@$remoteAddress"

  /** The desired format for clients compatible with API version 17 is
    * incorrectly determined to be HTML, because those clients do not
    * specify an Accept header in their WebSocket requests thus the server
    * thinks they are browsers by default. However, the WebSocket API does
    * not support HTML, only JSON, so we can safely assume they are JSON
    * clients and since clients newer than version 17 must use the Accept
    * header, we can conclude that they are API version 17 JSON clients.
    *
    * Therefore we can filter out HTML formats as below and default to API
    * version 17 unless the client explicitly requests otherwise.
    *
    * This is a workaround to ensure API compatibility during a transition
    * period from a non-versioned API to a versioned one. Once the transition
    * is complete, we should default to the latest API version unless
    * clients specify otherwise.
    */
  val apiVersion = PimpRequest.requestedResponseFormat(request)
    .filter(_ != MimeTypes.HTML)
    .getOrElse(JsonFormatVersions.JSONv17)
  log debug s"Client connected with API version: $apiVersion"
  override val toString = describe
}

object ClientInfo {
  private val log = Logger(getClass)
}
