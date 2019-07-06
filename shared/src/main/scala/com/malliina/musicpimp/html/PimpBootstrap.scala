package com.malliina.musicpimp.html

import com.malliina.html.HtmlTags.spanClass
import com.malliina.html.HtmlWords.True
import com.malliina.html.{Bootstrap, HtmlTags}
import scalatags.Text.all._

object PimpBootstrap extends PimpBootstrap

class PimpBootstrap extends Bootstrap(HtmlTags) {
  implicit val callAttr = com.malliina.play.tags.PlayTags.callAttr

  def iconic(iconicName: String) =
    spanClass(s"oi oi-$iconicName", title := iconicName, aria.hidden := True)
}
