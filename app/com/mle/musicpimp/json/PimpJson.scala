package com.mle.musicpimp.json

import com.mle.audio.PlayerStates

/**
 *
 * @author mle
 */
trait PimpJson {

  implicit object playStateFormat extends com.mle.play.json.JsonFormats.SimpleFormat[PlayerStates.PlayerState](PlayerStates.withName)

}

object PimpJson extends PimpJson
