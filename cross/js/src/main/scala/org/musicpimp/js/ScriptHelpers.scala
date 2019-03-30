package org.musicpimp.js

import org.scalajs.jquery.{JQuery, JQueryStatic}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

trait ScriptHelpers {
  def elem(id: String): JQuery = MyJQuery(s"#$id")
}

@js.native
@JSImport("jquery", JSImport.Namespace)
object MyJQuery extends JQueryStatic

@js.native
@JSImport("jquery-ui", JSImport.Namespace)
object MyJQueryUI extends js.Object

@JSImport("jquery-ui/ui/widgets/slider", JSImport.Namespace)
@js.native
object slider extends js.Object

@JSImport("jquery-ui/ui/widgets/autocomplete", JSImport.Namespace)
@js.native
object autocomplete extends js.Object
