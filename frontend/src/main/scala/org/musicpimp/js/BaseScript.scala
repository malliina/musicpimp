package org.musicpimp.js

import org.scalajs.jquery.jQuery
import upickle.Invalid

import scala.scalajs.js

trait BaseScript {
  def elem(id: String) = jQuery(s"#$id")

  def global = js.Dynamic.global

  def literal = js.Dynamic.literal

  def validate[T: PimpJSON.Reader](in: String): Either[Invalid, T] =
    PimpJSON.validate(in)

  def write[T: PimpJSON.Writer](t: T) =
    PimpJSON.write(t)
}
