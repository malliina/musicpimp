package org.musicpimp.js

import upickle.{Invalid, Js}

object PimpJSON extends upickle.AttributeTagged {
  override implicit def OptionW[T: Writer]: Writer[Option[T]] = Writer {
    case None => Js.Null
    case Some(s) => implicitly[Writer[T]].write(s)
  }

  override implicit def OptionR[T: Reader]: Reader[Option[T]] = Reader {
    case Js.Null => None
    case v: Js.Value => Some(implicitly[Reader[T]].read.apply(v))
  }

  def validate[T: Reader](expr: String): Either[Invalid, T] =
    toEither[T] {
      val jsValue = read[Js.Value](expr)
      readJs[T](jsValue)
    }

  def validateJs[T: Reader](v: Js.Value): Either[Invalid, T] =
    toEither(readJs[T](v))

  def toEither[T](code: => T): Either[Invalid, T] =
    try {
      Right(code)
    } catch {
      case e: Invalid => Left(e)
    }
}
