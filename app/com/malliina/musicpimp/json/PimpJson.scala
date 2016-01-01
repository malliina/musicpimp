package com.malliina.musicpimp.json

import com.malliina.audio.PlayerStates

/**
 *
 * @author mle
 */
trait PimpJson {

  implicit object playStateFormat extends com.malliina.play.json.JsonFormats.SimpleFormat[PlayerStates.PlayerState](PlayerStates.withName)

}

object PimpJson extends PimpJson
