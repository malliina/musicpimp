package com.malliina.musicpimp.tags

import com.malliina.musicpimp.tags.Tags.jsScript
import controllers.routes.Assets.at
import play.api.mvc.Call

import scalatags.Text.GenericAttr

object PlayTags extends PlayTags

trait PlayTags {
  implicit val callAttr = new GenericAttr[Call]

  def jsLinks(files: String*) = files map jsLink

  def jsLink(file: String) = jsScript(at(s"js/$file"))
}
