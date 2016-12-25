package com.malliina.tags

import scalatags.Text.all._

object Tags extends Tags

trait Tags {
  val Button = "button"
  val Checkbox = "checkbox"
  val Download = "download"
  val En = "en"
  val FormRole = "form"
  val Group = "group"
  val Image = "image"
  val Number = "number"
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

  def labelFor(forTarget: String, more: Modifier*) = label(`for` := forTarget, SeqNode(more))

  def divClass(clazz: String, more: Modifier*) = div(`class` := clazz, SeqNode(more))

  def spanClass(clazz: String, more: Modifier*) = span(`class` := clazz, SeqNode(more))

  def iClass(clazz: String) = i(`class` := clazz)

  def pClass(clazz: String, more: Modifier*) = p(`class` := clazz, SeqNode(more))

  def ulClass(clazz: String) = ul(`class` := clazz)

  def liClass(clazz: String) = li(`class` := clazz)

  def liHref[V: AttrValue](url: V, more: Modifier*)(text: Modifier*) = li(aHref(url, SeqNode(more))(SeqNode(text)))

  // WTF? Removing currying requires an AttrValue - should require Modifier?
  def aHref[V: AttrValue](url: V, more: Modifier*)(text: Modifier*) = a(href := url, SeqNode(more))(SeqNode(text))

  def jsScript[V: AttrValue](url: V) = script(src := url)

  def cssLink[V: AttrValue](url: V) = link(rel := Stylesheet, href := url)

  def submitButton(more: Modifier*) = button(`type` := Submit, SeqNode(more))

  def headeredTable(clazz: String, headers: Seq[Modifier])(tableBody: Modifier*) =
    table(`class` := clazz)(
      thead(headers.map(header => th(header))),
      SeqNode(tableBody)
    )

  def imageInput[V: AttrValue](imageUrl: V, more: Modifier*) =
    input(`type` := Image, src := imageUrl, SeqNode(more))

  def deviceWidthViewport =
    meta(name := "viewport", content := "width=device-width, initial-scale=1.0")

  def namedInput(idAndName: String, more: Modifier*) =
    input(id := idAndName, name := idAndName, SeqNode(more))
}
