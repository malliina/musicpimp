package org.musicpimp.js

import org.scalajs.jquery.{JQuery, JQueryEventObject, jQuery}
import upickle.{Invalid, Js}

import scala.scalajs.js

trait BaseScript {
  val DataId = "data-id"

  def elem(id: String): JQuery = jQuery(s"#$id")

  def global = js.Dynamic.global

  def literal = js.Dynamic.literal

  def validate[T: PimpJSON.Reader](in: Js.Value): Either[Invalid, T] =
    PimpJSON.validateJs(in)

  def write[T: PimpJSON.Writer](t: T) =
    PimpJSON.write(t)

  def postAjax[C: PimpJSON.Writer](resource: String, payload: C) =
    BaseScript.postAjax(resource, payload)

  def withDataId[T](clazzSelector: String)(withId: String => T) =
    installClick(clazzSelector) { e =>
      Option(e.delegateTarget.getAttribute(DataId)) foreach { id =>
        withId(id)
      }
    }

  def installClick(clazzSelector: String)(f: JQueryEventObject => Any) =
    jQuery(clazzSelector).click(f)
}

object BaseScript {
  def postAjax[C: PimpJSON.Writer](resource: String, payload: C) =
    jQuery.ajax(PimpQuery.postSettings(
      resource,
      "application/json",
      PimpJSON.write(payload)))
}