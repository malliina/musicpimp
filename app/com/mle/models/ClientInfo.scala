package com.mle.models

import com.mle.musicpimp.json.JsonFormatVersions
import com.mle.play.ws.SocketClient
import com.mle.util.Log
import controllers.{PimpRequest, RequestHelpers}
import play.api.http.MimeTypes
import play.api.libs.iteratee.Concurrent
import play.api.mvc.RequestHeader

/**
 * @param channel channel used to push messages to the client
 * @param request the request headers from the HTTP request that initiated the WebSocket connection
 * @param user the authenticated username
 */
case class ClientInfo[T](channel: Concurrent.Channel[T], request: RequestHeader, user: String)
  extends SocketClient[T] with Log {
  val protocol = if (RequestHelpers isHttps request) "wss" else "ws"
  val remoteAddress = request.remoteAddress
  val describe = s"$protocol://$user@$remoteAddress"
  /**
   * The desired format for clients compatible with API version 17 is
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