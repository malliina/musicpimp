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
  val Lead = "lead"
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

  val download = attr(Download).empty

  val empty: Modifier = ""

  val section = tag(Section)
  val titleTag = tag(Title)

  def labelFor(forTarget: String, more: Modifier*) = label(`for` := forTarget, more)

  def divClass(clazz: String, more: Modifier*) = div(`class` := clazz, more)

  def spanClass(clazz: String) = span(`class` := clazz)

  def iClass(clazz: String) = i(`class` := clazz)

  def leadPara = pClass(Lead)

  def pClass(clazz: String, more: Modifier*) = p(`class` := clazz, more)

  def ulClass(clazz: String) = ul(`class` := clazz)

  def liHref[V: AttrValue](url: V, more: Modifier*)(text: Modifier*) = li(aHref(url, more)(text))

  // WTF? Removing currying requires an AttrValue - should require Modifier?
  def aHref[V: AttrValue](url: V, more: Modifier*)(text: Modifier*) = a(href := url, more)(text)

  def jsScript[V: AttrValue](url: V) = script(src := url)

  def cssLink[V: AttrValue](url: V) = link(rel := Stylesheet, href := url)
}
