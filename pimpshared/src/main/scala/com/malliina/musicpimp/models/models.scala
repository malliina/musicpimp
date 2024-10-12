package com.malliina.musicpimp.models

import com.malliina.musicpimp.cloud.PimpMessage
import com.malliina.play.{ContentRange, Writeables}
import com.malliina.values.Literals.err
import com.malliina.values.{ErrorMessage, ValidatingCompanion}
import io.circe.Codec
import play.api.http.Writeable

import java.util.UUID

case class Version(version: String) extends PimpMessage derives Codec.AsObject

object Version:
  implicit val html: Writeable[Version] = Writeables.fromCirceJson[Version]

case class FailReason(reason: String) extends PimpMessage derives Codec.AsObject

object FailReason:
  implicit val html: Writeable[FailReason] = Writeables.fromCirceJson[FailReason]

case class WrappedID(id: String) derives Codec.AsObject

object WrappedID:
  def forId(id: Identifier): WrappedID = WrappedID(id.id)

case class WrappedLong(id: Long) derives Codec.AsObject

case class RangedRequest(id: TrackID, range: ContentRange) derives Codec.AsObject

sealed trait RequestID extends Identifier:
  def toId = RequestIdentifier(id)

object RequestID extends ValidatingCompanion[String, RequestID]:
  private def apply(validated: String) = Impl(validated)

  override def build(input: String): Either[ErrorMessage, RequestID] =
    if input.nonEmpty then Right(RequestID(input)) else Left(err"Request ID cannot be empty.")

  override def write(t: RequestID) = t.id

  def random(): RequestID = RequestID(UUID.randomUUID().toString)

  private case class Impl(id: String) extends RequestID

case class SimpleCommand(cmd: String) derives Codec.AsObject
