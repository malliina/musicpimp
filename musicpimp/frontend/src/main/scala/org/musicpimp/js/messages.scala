package org.musicpimp.js

import com.malliina.musicpimp.json.PlaybackStrings.*
import io.circe.Codec

case class IdCommand(cmd: String, id: String) derives Codec.AsObject

trait ValuedCommand[T]:
  def cmd: String
  def value: T

case class IntCommand(cmd: String, value: Int) extends ValuedCommand[Int] derives Codec.AsObject

case class BoolCommand(cmd: String, value: Boolean) extends ValuedCommand[Boolean]
  derives Codec.AsObject

object ValuedCommand:
  def apply(cmd: String, value: Int) = IntCommand(cmd, value)

  def mute(isMute: Boolean): BoolCommand =
    BoolCommand(Mute, isMute)
