package com.malliina.musicpimp.json

import com.malliina.audio.PlayerStates
import com.malliina.json.JsonFormats.SimpleFormat

trait PimpJson {

  implicit object playStateFormat
    extends SimpleFormat[PlayerStates.PlayerState](PlayerStates.withName)

}

object PimpJson extends PimpJson
