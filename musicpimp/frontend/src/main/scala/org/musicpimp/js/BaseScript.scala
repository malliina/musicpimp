package org.musicpimp.js

import io.circe.Encoder
import io.circe.syntax.EncoderOps
import org.musicpimp.js.ScriptHelpers.ElementOps
import org.scalajs.dom.{Element, Event, document}

import scala.scalajs.js

trait BaseScript:
  val DataId = "data-id"

  def literal = js.Dynamic.literal

  def postAjax[C: Encoder](resource: String, payload: C) =
    BaseScript.postAjax(resource, payload)

  // .currentTarget used to be .delegateTarget, check if that introduces a regression
  def withDataId[T](cls: String, more: String*)(withId: String => T): Unit =
    installClick(cls +: more): e =>
      Option(e.currentTarget.asInstanceOf[Element].getAttribute(DataId)).foreach: id =>
        withId(id)

  def installClick(classes: Seq[String])(f: Event => Any): Unit =
    document
      .getElementsByClassName(classes.mkString(" "))
      .foreach: e =>
        e.onClick: event =>
          f(event)

object BaseScript:
  val ApplicationJson = "application/json"

  def postAjax[C: Encoder](resource: String, payload: C) =
    val settings = PimpQuery.postSettings(
      resource,
      ApplicationJson,
      payload.asJson.noSpaces
    )
    MyJQuery.ajax(settings)
