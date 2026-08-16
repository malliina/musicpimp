package com.malliina.musicpimp.messaging

import com.malliina.concurrent.Execution.cached
import com.malliina.http.OkClient
import com.malliina.musicpimp.messaging.APNSUtils.fold
import com.malliina.musicpimp.messaging.cloud.{APNSHttpResult, APNSPayload}
import com.malliina.push.apns.*
import com.malliina.values.ErrorMessage
import play.api.Configuration

import scala.concurrent.Future

object APNSUtils:
  def fold(result: Either[APNSError, APNSIdentifier], token: APNSToken): APNSHttpResult =
    result.fold(
      err => APNSHttpResult(token, None, Option(err)),
      id => APNSHttpResult(token, Option(id), None)
    )

/** Using HTTP/2.
  */
class APNSHttpHandler(client: APNSHttpClient)
  extends PushRequestHandler[APNSPayload, APNSHttpResult]:
  val MusicPimpTopic = APNSTopic("org.musicpimp.MusicPimp")
  val meta = APNSMeta.withTopic(MusicPimpTopic)

  def pushOne(request: APNSPayload): Future[APNSHttpResult] =
    client
      .push(request.token, APNSRequest(request.message, meta))
      .map(r => fold(r, request.token))

object APNSTokenHandler:
  def fromConf(config: Configuration, http: OkClient, isSandbox: Boolean) =
    val attempt = APNSTokenConf.parse: key =>
      config
        .getOptional[String](key)
        .toRight(ErrorMessage(s"Key not found: '$key'."))
    attempt.fold(msg => throw new Exception(msg.message), apply(_, http, isSandbox))

  def apply(conf: APNSTokenConf, http: OkClient, isSandbox: Boolean) =
    new APNSTokenHandler(APNSTokenClient(conf, http, isSandbox))

class APNSTokenHandler(client: APNSHttpClient)
  extends PushRequestHandler[APNSPayload, APNSHttpResult]:
  val MusicPimpTopic = APNSTopic("org.musicpimp.MusicPimp")

  override def pushOne(request: APNSPayload): Future[APNSHttpResult] =
    client
      .push(request.token, APNSRequest.withTopic(MusicPimpTopic, request.message))
      .map(r => fold(r, request.token))
