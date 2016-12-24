package org.musicpimp.js

import org.scalajs.jquery.{JQuery, JQueryAjaxSettings, JQueryEventObject, jQuery}
import upickle.{Invalid, Js}

import scala.scalajs.js

trait BaseScript {
  def elem(id: String): JQuery = jQuery(s"#$id")

  def global = js.Dynamic.global

  def literal = js.Dynamic.literal

  def validate[T: PimpJSON.Reader](in: Js.Value): Either[Invalid, T] =
    PimpJSON.validateJs(in)

  def write[T: PimpJSON.Writer](t: T) =
    PimpJSON.write(t)

  def postAjax[C: PimpJSON.Writer](resource: String, payload: C) =
    jQuery.ajax(literal(
      url = resource,
      `type` = "POST",
      contentType = "application/json",
      data = write(payload)
    ).asInstanceOf[JQueryAjaxSettings])

  def withDataId[T](clazzSelector: String)(withId: String => T) =
    installClick(clazzSelector) { e =>
      Option(e.delegateTarget.getAttribute("data-id")) foreach { id =>
        withId(id)
      }
    }

  def installClick(clazzSelector: String)(f: JQueryEventObject => Any) =
    jQuery(clazzSelector).click(f)
}
