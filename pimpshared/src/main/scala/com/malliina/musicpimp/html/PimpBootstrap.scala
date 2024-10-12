package com.malliina.musicpimp.html

import com.malliina.html.HtmlTags.spanClass
import com.malliina.html.HtmlWords.True
import com.malliina.html.{Bootstrap, HtmlTags}
import play.api.mvc.Call
import scalatags.Text
import scalatags.Text.all.*

object PimpBootstrap extends PimpBootstrap

class PimpBootstrap extends Bootstrap(HtmlTags):
  implicit val callAttr: Text.GenericAttr[Call] = com.malliina.play.tags.PlayTags.callAttr

  def iconic(iconicName: String) =
    spanClass(s"oi oi-$iconicName", title := iconicName, aria.hidden := True)
