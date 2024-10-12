package com.malliina.html

import com.malliina.values.WrappedString
import scalatags.generic.Bundle

import scala.language.implicitConversions

class Tags[Builder, Output <: FragT, FragT](val impl: Bundle[Builder, Output, FragT])
  extends HtmlWords:

  import impl.all.*

  val crossorigin = attr("crossorigin")
  val integrity = attr("integrity")
  val defer = attr("defer").empty

  val empty: Modifier = ""

  val download = attr(Download).empty

  val section = tag(Section)
  val titleTag = tag(Title)

  implicit def wrappedFrag[T <: WrappedString](t: T): Frag = stringFrag(t.value)

  def labelFor(forTarget: String, more: Modifier*) = label(`for` := forTarget, more)

  def divClass(clazz: String, more: Modifier*) = div(`class` := clazz, more)

  def spanClass(clazz: String, more: Modifier*) = span(`class` := clazz, more)

  def iClass(clazz: String) = i(`class` := clazz)

  def pClass(clazz: String, more: Modifier*) = p(`class` := clazz, more)

  def ulClass(clazz: String) = ul(`class` := clazz)

  def liClass(clazz: String) = li(`class` := clazz)

  def liHref[V: AttrValue](url: V, more: Modifier*)(text: Modifier*) =
    li(a(href := url, more)(text))

  def cssLinkHashed[V: AttrValue](url: V, integrityHash: String, more: Modifier*) =
    cssLink(url, integrity := integrityHash, crossorigin := Anonymous, more)

  def cssLink[V: AttrValue](url: V, more: Modifier*) =
    link(rel := Stylesheet, href := url, more)

  def jsHashed[V: AttrValue](url: V, integrityHash: String, more: Modifier*) =
    jsScript(url, integrity := integrityHash, crossorigin := Anonymous, more)

  def jsScript[V: AttrValue](url: V, more: Modifier*) = script(src := url, more)

  def submitButton(more: Modifier*) = button(`type` := Submit, more)

  def headeredTable(clazz: String, headers: Seq[Modifier])(tableBody: Modifier*) =
    table(`class` := clazz)(
      thead(headers.map(header => th(header))),
      tableBody
    )

  def imageInput[V: AttrValue](imageUrl: V, more: Modifier*) =
    input(`type` := Image, src := imageUrl, more)

  def deviceWidthViewport =
    meta(name := "viewport", content := "width=device-width, initial-scale=1.0")

  def namedInput(idAndName: String, more: Modifier*) =
    input(id := idAndName, name := idAndName, more)
