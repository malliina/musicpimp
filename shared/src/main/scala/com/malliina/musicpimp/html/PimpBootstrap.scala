package com.malliina.musicpimp.html

import com.malliina.html.{Bootstrap, HtmlTags}

object PimpBootstrap extends PimpBootstrap

class PimpBootstrap extends Bootstrap(HtmlTags) {
  implicit val callAttr = com.malliina.play.tags.PlayTags.callAttr
}
