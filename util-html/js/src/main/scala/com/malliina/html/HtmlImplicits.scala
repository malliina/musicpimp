package com.malliina.html

import com.malliina.http.FullUrl
import org.scalajs.dom.Element
import scalatags.JsDom.all._

object HtmlImplicits extends HtmlImplicits

trait HtmlImplicits {
  implicit val fullUrl: AttrValue[FullUrl] = attrType[FullUrl](_.url)

  def attrType[T](stringify: T => String): AttrValue[T] = (t: Element, a: Attr, v: T) =>
    t.setAttribute(a.name, stringify(v))
}
