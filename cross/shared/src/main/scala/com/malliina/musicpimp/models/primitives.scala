package com.malliina.musicpimp.models

import com.malliina.values.{ErrorMessage, ValidatingCompanion}
import play.api.libs.json.{Format, JsError, JsSuccess, Reads, Writes}

case class RequestIdentifier(id: String) extends Identifier

object RequestIdentifier extends IdentCompanion[RequestIdentifier]:
  override def build(input: String): Either[ErrorMessage, RequestIdentifier] = Right(apply(input))

trait Identifier extends Any:
  def id: String

  override def toString: String = id

abstract class IdentCompanion[T <: Identifier] extends ValidatingCompanion[String, T]:
  override def write(t: T): String = t.id

  implicit val playJson: Format[T] = Format(
    Reads.StringReads.flatMapResult(s =>
      build(s).fold(err => JsError(err.message), ok => JsSuccess(ok))
    ),
    Writes.StringWrites.contramap(write)
  )
