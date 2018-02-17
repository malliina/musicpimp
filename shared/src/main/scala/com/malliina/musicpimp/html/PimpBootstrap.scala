package com.malliina.musicpimp.html

import com.malliina.html.{Bootstrap, Tags}

object PimpBootstrap extends PimpBootstrap

class PimpBootstrap extends Bootstrap(Tags) {
  implicit val callAttr = com.malliina.play.tags.PlayTags.callAttr
}
