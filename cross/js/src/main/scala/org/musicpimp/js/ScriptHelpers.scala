package org.musicpimp.js

import com.malliina.musicpimp.js.FrontStrings
import org.scalajs.dom.{Element, Event, document}

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSImport, JSName}

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
