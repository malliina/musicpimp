package org.musicpimp.js

case class Command(cmd: String)

object Command {
  val Subscribe = apply("subscribe")
}
