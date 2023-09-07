package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.models.{FailReason, RequestID}
import play.api.libs.json.{Json, Writes}

/** A response to `request`, sent to pimplcoud.
  */
case class CloudResponse[T: Writes](request: RequestID, success: Boolean, body: T)

object CloudResponse {
  val RequestKey = "request"
  val BodyKey = "body"
  val SuccessKey = "success"

  implicit def json[T: Writes]: Writes[CloudResponse[T]] = Writes[CloudResponse[T]] { r =>
    Json.obj(
      RequestKey -> r.request,
      SuccessKey -> r.success,
      BodyKey -> Json.toJson(r.body)
    )
  }

  def ack(request: RequestID) =
    success(request, Json.obj())

  def success[T: Writes](request: RequestID, body: T) =
    CloudResponse(request, success = true, body)

  def genericFailed(request: RequestID) =
    failed(request, JsonMessages.genericFailure)

  def failed(request: RequestID, reason: FailReason) =
    CloudResponse(request, success = false, reason)
}
