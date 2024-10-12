package com.malliina.musicpimp.json

import com.malliina.musicpimp.json.JsonStrings.*
import com.malliina.musicpimp.models.{FailReason, Version}
import io.circe.Json
import io.circe.syntax.EncoderOps

object JsonMessages extends JsonMessages

trait JsonMessages extends CommonMessages:
  val version = Version(com.malliina.musicpimp.BuildInfo.version)
  val unAuthorized = FailReason(AccessDenied)
  val databaseFailure = FailReason(DatabaseError)
  val genericFailure = FailReason(GenericError)
  val invalidParameter = FailReason(InvalidParameter)
  val invalidCredentials = FailReason(InvalidCredentials)
  val invalidJson = FailReason(InvalidJson)
  val noFileInMultipart = FailReason(NoFileInMultipart)

  def exception(e: Throwable) =
    FailReason(e.getMessage)

  def withStatus(json: Json): Json =
    event(StatusKey).deepMerge(json)

  def thanks =
    Json.obj(Msg -> ThankYou.asJson)
