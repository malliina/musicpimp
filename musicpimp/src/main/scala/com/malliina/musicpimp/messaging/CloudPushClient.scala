package com.malliina.musicpimp.messaging

import cats.effect.Async
import cats.implicits.toFlatMapOps
import com.malliina.http.io.HttpClientF2
import com.malliina.http.{FullUrl, HttpResponse}
import com.malliina.musicpimp.json.JsonStrings.{Body, Cmd, PushValue}
import com.malliina.musicpimp.messaging.cloud.*
import com.malliina.push.PushException
import io.circe.Json
import io.circe.syntax.EncoderOps

object CloudPushClient:
  def default[F[_]: Async](http: HttpClientF2[F]) =
    CloudPushClient(FullUrl("https", "cloud.musicpimp.org", ""), http)
  def local[F[_]: Async](http: HttpClientF2[F]) =
    CloudPushClient(FullUrl("http", "localhost:9000", ""), http)

class CloudPushClient[F[_]: Async](host: FullUrl, http: HttpClientF2[F]):
  val F = Async[F]
  private val pushUrl = host.append("/push")

  def push(pushTask: PushTask): F[PushResult] =
    http
      .postJson(pushUrl, Json.obj(Cmd -> PushValue.asJson, Body -> pushTask.asJson))
      .flatMap: res =>
        if res.code == 200 then
          res
            .parse[PushResponse]
            .map: response =>
              F.pure(response.result)
            .getOrElse:
              F.raiseError(new PushJsonException(res))
        else F.raiseError(new OkPushException(res))

class PushJsonException(response: HttpResponse)
  extends PushException(s"Unexpected push response body: '${response.asString}'.")

class OkPushException(val response: HttpResponse) extends PushException("Request failed")
