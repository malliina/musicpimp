package com.malliina.musicpimp.messaging

import cats.implicits.toFunctorOps
import cats.{Applicative, Monad}
import com.malliina.concurrent.Execution.cached
import com.malliina.http.io.HttpClientF
import com.malliina.musicpimp.messaging.APNSUtils.fold
import com.malliina.musicpimp.messaging.cloud.{APNSHttpResult, APNSPayload}
import com.malliina.push.apns.*
import com.malliina.values.ErrorMessage

object APNSUtils:
  def fold(result: Either[APNSError, APNSIdentifier], token: APNSToken): APNSHttpResult =
    result.fold(
      err => APNSHttpResult(token, None, Option(err)),
      id => APNSHttpResult(token, Option(id), None)
    )

/** Using HTTP/2.
  */
class APNSHttpHandler[F[_]: Applicative](client: APNSHttpClientF[F])
  extends PushRequestHandler[F, APNSPayload, APNSHttpResult]:
  val MusicPimpTopic = APNSTopic("org.musicpimp.MusicPimp")
  val meta = APNSMeta.withTopic(MusicPimpTopic)

  def pushOne(request: APNSPayload): F[APNSHttpResult] =
    client
      .push(request.token, APNSRequest(request.message, meta))
      .map(r => fold(r, request.token))

object APNSTokenHandler:
//  def fromConf[F[_]](config: Configuration, http: HttpClientF[F], isSandbox: Boolean) =
//    val attempt = APNSTokenConf.parse: key =>
//      config
//        .getOptional[String](key)
//        .toRight(ErrorMessage(s"Key not found: '$key'."))
//    attempt.fold(msg => throw new Exception(msg.message), apply(_, http, isSandbox))

  def apply[F[_]: Monad](conf: APNSTokenConf, http: HttpClientF[F], isSandbox: Boolean) =
    new APNSTokenHandler(APNSHttpClientF(conf, http, isSandbox))

class APNSTokenHandler[F[_]: Monad](client: APNSHttpClientF[F])
  extends PushRequestHandler[F, APNSPayload, APNSHttpResult]:
  val MusicPimpTopic = APNSTopic("org.musicpimp.MusicPimp")

  override def pushOne(request: APNSPayload): F[APNSHttpResult] =
    client
      .push(request.token, APNSRequest.withTopic(MusicPimpTopic, request.message))
      .map(r => fold(r, request.token))
