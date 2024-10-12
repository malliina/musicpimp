package org.musicpimp.js

import com.malliina.musicpimp.js.FrontStrings
import org.scalajs.dom.{Element, Event, document}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ScriptHelpers extends ScriptHelpers

trait ScriptHelpers:
  def elem(id: String): Element =
    findElem(id).getOrElse(throw Exception(s"Element not found: '$id'."))
  def findElem(id: String): Option[Element] = Option(document.getElementById(id))
  def elemAs[T <: Element](id: String): T = elem(id).asInstanceOf[T]

  implicit class ElementOps(e: Element):
    def addClass(cls: String): Unit = if !hasClass(cls) then e.classList.add(cls) else ()
    def removeClass(cls: String): Unit = e.classList.remove(cls)
    def hasClass(cls: String): Boolean = e.classList.contains(cls)
    def onClick(code: Event => Unit): Unit = e.addEventListener("click", code)
    def html(content: String): Unit = e.innerHTML = content
    def hide(): Unit = addClass(FrontStrings.HiddenClass)
    def show(): Unit = removeClass(FrontStrings.HiddenClass)
    def toggleClass(cls: String): Unit =
      if hasClass(cls) then e.removeClass(cls) else e.addClass(cls)

@js.native
trait AjaxSettings extends js.Object:
  def url: String = js.native
  def contentType: String = js.native
  def data: String = js.native
  def `type`: String = js.native

@js.native
trait Request extends js.Object:
  def term: String = js.native

object Request:
  def apply(term: String) =
    js.Dynamic.literal(term = term).asInstanceOf[Request]

@js.native
trait Response extends js.Object {}

@js.native
trait JQXHR extends js.Object:
  def done(callback: js.Any => Boolean): js.Any = js.native

@js.native
trait JQuery extends js.Object

@js.native
@JSImport("jquery", JSImport.Namespace)
object MyJQuery extends js.Object:
  def apply(): JQuery = js.native
  def apply(selector: String): JQuery = js.native
  def apply(selector: String, context: Element | JQuery): JQuery = js.native
  def ajax(options: AjaxSettings): JQXHR = js.native
  def getJSON(
    url: String,
    data: Request,
    success: js.Function3[js.Object, String, JQXHR, Any]
  ): JQXHR =
    js.native

@js.native
@JSImport("jquery-ui", JSImport.Namespace)
object MyJQueryUI extends js.Object

@JSImport("jquery-ui/ui/widgets/slider", JSImport.Namespace)
@js.native
object slider extends js.Object

@JSImport("jquery-ui/ui/widgets/autocomplete", JSImport.Namespace)
@js.native
object autocomplete extends js.Object
