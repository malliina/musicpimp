package com.malliina.musicpimp.json

import com.malliina.audio.PlayerStates
import com.malliina.json.JsonFormats

trait PimpJson {

  implicit object playStateFormat extends JsonFormats.SimpleFormat[PlayerStates.PlayerState](PlayerStates.withName)

}

object PimpJson extends PimpJson
