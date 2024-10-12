package com.malliina.play.tags

import play.api.mvc.Call

import scalatags.Text.GenericAttr

object Tags extends com.malliina.html.Tags(scalatags.Text)

object PlayTags extends PlayTags

trait PlayTags {
  implicit val callAttr: GenericAttr[Call] = new GenericAttr[Call]
}

object Bootstrap extends com.malliina.html.Bootstrap(Tags)
