package com.malliina.html

import com.malliina.http.FullUrl
import scalatags.Text.all._
import scalatags.text.Builder

object HtmlImplicits extends HtmlImplicits

trait HtmlImplicits {
  implicit val fullUrl: AttrValue[FullUrl] = attrType[FullUrl](_.url)

  def attrType[T](stringify: T => String): AttrValue[T] = (t: Builder, a: Attr, v: T) =>
    t.setAttr(a.name, Builder.GenericAttrValueSource(stringify(v)))
}
