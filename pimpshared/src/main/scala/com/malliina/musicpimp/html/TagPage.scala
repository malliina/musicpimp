package com.malliina.musicpimp.html

import scalatags.Text

/** Helper that enables imports-free usage in Play's `Action` s such as: `Action(Ok(myTags))`.
  *
  * @param tags
  *   scalatags
  */
case class TagPage(tags: Text.TypedTag[String]):
  override def toString = tags.toString()

object TagPage:
  val DocTypeTag = "<!DOCTYPE html>"
