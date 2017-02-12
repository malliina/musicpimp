package com.malliina.musicpimp.tags

import java.nio.charset.StandardCharsets

import akka.util.ByteString
import play.api.http.{MimeTypes, Writeable}

import scalatags.Text

/** Helper that enables imports-free usage in Play's `Action`s such as: `Action(Ok(myTags))`.
  *
  * @param tags scalatags
  */
case class TagPage(tags: Text.TypedTag[String]) {
  override def toString = tags.toString()
}

object TagPage {
  val DocTypeTag = "<!DOCTYPE html>"

  val typedTagWriteable: Writeable[Text.TypedTag[String]] =
    Writeable(toUtf8, Option(MimeTypes.HTML))

  implicit val html: Writeable[TagPage] =
    typedTagWriteable.map(_.tags)

  private def toUtf8(tags: Text.TypedTag[String]): ByteString =
    ByteString(DocTypeTag + tags, StandardCharsets.UTF_8.name())
}
