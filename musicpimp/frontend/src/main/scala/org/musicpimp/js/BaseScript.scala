package org.musicpimp.js

import org.scalajs.jquery.JQueryEventObject
import play.api.libs.json.{Json, Writes}

import scala.scalajs.js

trait BaseScript {
  val DataId = "data-id"

  def global = js.Dynamic.global

  def literal = js.Dynamic.literal

  def postAjax[C: Writes](resource: String, payload: C) =
    BaseScript.postAjax(resource, payload)

  def withDataId[T](clazzSelector: String)(withId: String => T) =
    installClick(clazzSelector) { e =>
      Option(e.delegateTarget.getAttribute(DataId)) foreach { id =>
        withId(id)
      }
    }

  def installClick(clazzSelector: String)(f: JQueryEventObject => Any) =
    MyJQuery(clazzSelector).click(f)
}

object BaseScript {
  val ApplicationJson = "application/json"

  def postAjax[C: Writes](resource: String, payload: C) = {
    val settings = PimpQuery.postSettings(
      resource,
      ApplicationJson,
      Json.stringify(Json.toJson(payload))
    )
    MyJQuery.ajax(settings)
  }
}
