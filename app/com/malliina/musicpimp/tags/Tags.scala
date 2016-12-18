package com.malliina.musicpimp.tags

import scalatags.Text.all._

object Tags extends Tags

trait Tags {
  val Button = "button"
  val Checkbox = "checkbox"
  val Download = "download"
  val En = "en"
  val FormRole = "form"
  val Image = "image"
  val Number ="number"
  val Password = "password"
  val Post = "POST"
  val Search = "search"
  val Section = "section"
  val Separator = "separator"
  val Submit = "submit"
  val Stylesheet = "stylesheet"
  val Text = "text"
  val Title = "title"

  val empty: Modifier = ""

  val download = attr(Download).empty

  val ariaExpanded = attr("aria-expanded")
  val ariaHasPopup = attr("aria-haspopup")
  val ariaLabel = attr("aria-label")

  val section = tag(Section)
  val titleTag = tag(Title)

  def labelFor(forTarget: String, more: Modifier*) = label(`for` := forTarget, more)

  def divClass(clazz: String, more: Modifier*) = div(`class` := clazz, more)

  def spanClass(clazz: String, more: Modifier*) = span(`class` := clazz, more)

  def iClass(clazz: String) = i(`class` := clazz)

  def pClass(clazz: String, more: Modifier*) = p(`class` := clazz, more)

  def ulClass(clazz: String) = ul(`class` := clazz)

  def liHref[V: AttrValue](url: V, more: Modifier*)(text: Modifier*) = li(aHref(url, more)(text))

  // WTF? Removing currying requires an AttrValue - should require Modifier?
  def aHref[V: AttrValue](url: V, more: Modifier*)(text: Modifier*) = a(href := url, more)(text)

  def jsScript[V: AttrValue](url: V) = script(src := url)

  def cssLink[V: AttrValue](url: V) = link(rel := Stylesheet, href := url)

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
    input(`type` := idAndName, name := idAndName, more)

}
