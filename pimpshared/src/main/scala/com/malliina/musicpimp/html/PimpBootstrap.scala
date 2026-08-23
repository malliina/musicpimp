package com.malliina.musicpimp.html

import com.malliina.html.HtmlTags.spanClass
import com.malliina.html.HtmlWords.True
import com.malliina.html.{Bootstrap, HtmlTags}
import scalatags.Text.all.*

object PimpBootstrap extends PimpBootstrap

class PimpBootstrap extends Bootstrap(HtmlTags):
  def iconic(iconicName: String) =
    spanClass(s"oi oi-$iconicName", title := iconicName, aria.hidden := True)

  def faIcon(faName: String) =
    spanClass(s"fa fa-$faName", title := faName, aria.hidden := True)
