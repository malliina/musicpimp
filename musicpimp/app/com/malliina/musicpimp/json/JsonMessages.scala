package com.malliina.musicpimp.json

import com.malliina.audio.PlayerStates
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.models.{FailReason, Version}
import play.api.libs.json.Json._
import play.api.libs.json.{JsObject, JsValue, Json}

object JsonMessages extends JsonMessages

trait JsonMessages extends CommonMessages {
  val version = Version(com.malliina.musicpimp.BuildInfo.version)
  val noMedia = obj(State -> PlayerStates.NoMedia.toString)
  val unAuthorized = FailReason(AccessDenied)
  val databaseFailure = FailReason(DatabaseError)
  val genericFailure = FailReason(GenericError)
  val invalidParameter = FailReason(InvalidParameter)
  val invalidCredentials = FailReason(InvalidCredentials)
  val invalidJson = FailReason(InvalidJson)
  val noFileInMultipart = FailReason(NoFileInMultipart)
  val ping = event(Ping)

  def exception(e: Throwable) =
    FailReason(e.getMessage)

  def withStatus(json: JsValue): JsValue =
    event(StatusKey) ++ json.as[JsObject]

  def thanks =
    Json.obj(Msg -> Json.toJson(ThankYou))
}
