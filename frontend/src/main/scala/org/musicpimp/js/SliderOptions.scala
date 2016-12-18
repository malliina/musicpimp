package org.musicpimp.js

import org.scalajs.dom.raw.Event

import scala.scalajs.js

@js.native
trait Ui extends js.Object {
  def handle: js.Dynamic = js.native

  def value: Int = js.native
}

@js.native
trait StopOptions extends js.Object {
  def stop: js.Function2[Event, Ui, Unit] = js.native
}

object StopOptions {
  def default(stop: (Event, Ui) => Unit) = apply(stop)

  def apply(stop: js.Function2[Event, Ui, Unit]) =
    js.Dynamic.literal(stop = stop).asInstanceOf[StopOptions]
}

@js.native
trait SliderOptions extends StopOptions {
  def orientation: String = js.native

  def range: String = js.native

  def min: Int = js.native

  def max: Int = js.native
}

object SliderOptions {
  val Horizontal = "horizontal"

  def horizontal(range: String, min: Int, max: Int, stop: Ui => Any) =
    apply(Horizontal, range, min, max, (_: Event, ui: Ui) => stop(ui))

  def apply(orientation: String, range: String, min: Int, max: Int, stop: js.Function2[Event, Ui, Any]) =
    js.Dynamic.literal(orientation = orientation, range = range, min = min, max = max, stop = stop).asInstanceOf[SliderOptions]
}
