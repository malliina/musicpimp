package com.mle.musicpimp.json

import com.mle.audio.PlayerStates
import com.mle.play.json.JsonFormats2

/**
 *
 * @author mle
 */
trait PimpJson {

  implicit object playStateFormat extends JsonFormats2.SimpleFormat[PlayerStates.PlayerState](PlayerStates.withName)

}

object PimpJson extends PimpJson
