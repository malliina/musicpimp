package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.models.{FailReason, RequestID}
import io.circe.{Encoder, Json}
import io.circe.syntax.EncoderOps

/** A response to `request`, sent to pimpcloud.
  */
case class CloudResponse[T: Encoder](request: RequestID, success: Boolean, body: T)

object CloudResponse:
  val RequestKey = "request"
  val BodyKey = "body"
  val SuccessKey = "success"

  implicit def json[T: Encoder]: Encoder[CloudResponse[T]] = Encoder[CloudResponse[T]]: r =>
    Json.obj(
      RequestKey -> r.request.asJson,
      SuccessKey -> r.success.asJson,
      BodyKey -> r.body.asJson
    )

  def ack(request: RequestID) =
    success(request, Json.obj())

  def success[T: Encoder](request: RequestID, body: T) =
    CloudResponse(request, success = true, body)

  def genericFailed(request: RequestID) =
    failed(request, JsonMessages.genericFailure)

  def failed(request: RequestID, reason: FailReason) =
    CloudResponse(request, success = false, reason)
