package org.musicpimp.js

import upickle.Js.Value
import upickle.{Invalid, Js}

import scala.concurrent.duration.{Duration, DurationDouble}

object PimpJSON extends upickle.AttributeTagged {
  implicit val durationJson = PimpJSON.ReadWriter[Duration](
    dur => Js.Num(dur.toSeconds), { case Js.Num(num) => num.seconds })

  override implicit def OptionW[T: Writer]: Writer[Option[T]] = Writer {
    case None => Js.Null
    case Some(s) => implicitly[Writer[T]].write(s)
  }

  override implicit def OptionR[T: Reader]: Reader[Option[T]] = Reader {
    case Js.Null => None
    case v: Js.Value => Some(implicitly[Reader[T]].read.apply(v))
  }

  def parseObj(raw: String): Either[Invalid, Map[String, Value]] =
    toEither(read[Js.Value](raw).obj)

  def parse(raw: String): Either[Invalid, Js.Value] =
    toEither(read[Js.Value](raw))

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
