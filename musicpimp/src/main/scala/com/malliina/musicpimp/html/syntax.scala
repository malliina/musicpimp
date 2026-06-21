package com.malliina.musicpimp.html

import com.malliina.musicpimp.http4s.Reverse
import org.http4s.Uri
import scalatags.Text.all.{Attr, AttrValue}
import scalatags.text.Builder

trait UriSyntax:
  val reverse = Reverse

  given AttrValue[Uri] = attrValue(_.renderString)

  private def attrValue[T](f: T => String): AttrValue[T] =
    (t: Builder, a: Attr, v: T) => t.setAttr(a.name, Builder.GenericAttrValueSource(f(v)))

class HtmlSyntax extends PimpBootstrap with UriSyntax
